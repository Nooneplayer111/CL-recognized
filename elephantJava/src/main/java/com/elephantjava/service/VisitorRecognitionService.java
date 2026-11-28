package com.elephantjava.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elephantjava.Exception.BaseException;
import com.elephantjava.contex.BaseContext;
import com.elephantjava.contex.VisitorContext;
import com.elephantjava.entity.AuditEvent;
import com.elephantjava.entity.Individual;
import com.elephantjava.entity.MediaUpload;
import com.elephantjava.entity.RecognitionRecord;
import com.elephantjava.entity.RecognitionTask;
import com.elephantjava.entity.Species;
import com.elephantjava.entity.VisitorSession;
import com.elephantjava.interceptor.VisitorSessionInterceptor;
import com.elephantjava.mapper.AuditEventMapper;
import com.elephantjava.mapper.IndividualMapper;
import com.elephantjava.mapper.MediaUploadMapper;
import com.elephantjava.mapper.RecognitionRecordMapper;
import com.elephantjava.mapper.RecognitionTaskMapper;
import com.elephantjava.mapper.SpeciesMapper;
import com.elephantjava.mapper.VisitorSessionMapper;
import com.elephantjava.properties.RecognitionProperties;
import com.elephantjava.properties.UploadProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VisitorRecognitionService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/quicktime");
    private static final Set<String> UNCONFIRMED_NAMES = Set.of("识别中...", "识别中", "大象", "unknown", "Unknown");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final VisitorSessionMapper sessionMapper;
    private final MediaUploadMapper uploadMapper;
    private final RecognitionTaskMapper taskMapper;
    private final RecognitionRecordMapper recordMapper;
    private final IndividualMapper individualMapper;
    private final SpeciesMapper speciesMapper;
    private final AuditEventMapper auditMapper;
    private final RecognitionProperties recognitionProperties;
    private final UploadProperties uploadProperties;
    private final RestClient restClient;
    private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "recognition-task");
        thread.setDaemon(true);
        return thread;
    });

    public VisitorRecognitionService(VisitorSessionMapper sessionMapper,
                                     MediaUploadMapper uploadMapper,
                                     RecognitionTaskMapper taskMapper,
                                     RecognitionRecordMapper recordMapper,
                                     IndividualMapper individualMapper,
                                     SpeciesMapper speciesMapper,
                                     AuditEventMapper auditMapper,
                                     RecognitionProperties recognitionProperties,
                                     UploadProperties uploadProperties) {
        this.sessionMapper = sessionMapper;
        this.uploadMapper = uploadMapper;
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.individualMapper = individualMapper;
        this.speciesMapper = speciesMapper;
        this.auditMapper = auditMapper;
        this.recognitionProperties = recognitionProperties;
        this.uploadProperties = uploadProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public Map<String, Object> createSession() {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        VisitorSession session = new VisitorSession();
        session.setId(id("vs"));
        session.setTokenHash(VisitorSessionInterceptor.sha256(token));
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusHours(24));
        sessionMapper.insert(session);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("sessionId", session.getId());
        view.put("sessionType", "ANONYMOUS_VISITOR");
        view.put("expiresAt", session.getExpiresAt().toString());
        view.put("token", token);
        return view;
    }

    public Map<String, Object> createUpload(Map<String, Object> request) {
        String contentType = text(request.get("contentType"));
        if (!IMAGE_TYPES.contains(contentType) && !VIDEO_TYPES.contains(contentType)) {
            throw new BaseException("只接受 jpg、png、webp 图片，或 mp4、mov 视频");
        }
        long size = longValue(request.get("sizeBytes"));
        if (size < 1 || size > MAX_BYTES) {
            throw new BaseException("文件需要在 1 字节到 10MB 之间");
        }
        LocalDateTime now = LocalDateTime.now();
        MediaUpload upload = new MediaUpload();
        upload.setId(id("up"));
        upload.setMediaAssetId(id("media"));
        upload.setSessionId(requiredSession());
        upload.setFileName(limit(text(request.get("fileName")), 255, "photo.jpg"));
        upload.setContentType(contentType);
        upload.setSizeBytes(size);
        upload.setSource("camera".equals(text(request.get("source"))) ? "camera" : "album");
        upload.setState("UPLOAD_PENDING");
        upload.setMediaType(contentType.startsWith("video/") ? "video" : "image");
        upload.setCreatedAt(now);
        upload.setExpiresAt(now.plusMinutes(15));
        uploadMapper.insert(upload);
        return uploadView(upload);
    }

    public Map<String, Object> saveContent(String uploadId, byte[] bytes) throws Exception {
        MediaUpload upload = ownedUpload(uploadId);
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new BaseException("文件需要在 1 字节到 10MB 之间");
        }
        Path dir = Paths.get(uploadProperties.getPath(), "visitor").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String fileName = upload.getId() + "." + extension(upload.getContentType());
        Files.write(dir.resolve(fileName), bytes);
        upload.setStoragePath(dir.resolve(fileName).toString());
        upload.setSizeBytes((long) bytes.length);
        uploadMapper.updateById(upload);
        return uploadView(upload);
    }

    public Map<String, Object> completeUpload(String uploadId) {
        MediaUpload upload = ownedUpload(uploadId);
        if (!StringUtils.hasText(upload.getStoragePath()) || !Files.exists(Path.of(upload.getStoragePath()))) {
            throw new BaseException("照片还没有上传完成");
        }
        upload.setState("READY");
        uploadMapper.updateById(upload);
        return uploadView(upload);
    }

    public Map<String, Object> createTask(String mediaAssetId, String idempotencyKey, String animalId) {
        String sessionId = requiredSession();
        if (StringUtils.hasText(idempotencyKey)) {
            RecognitionTask existing = taskMapper.selectOne(new LambdaQueryWrapper<RecognitionTask>()
                    .eq(RecognitionTask::getSessionId, sessionId)
                    .eq(RecognitionTask::getIdempotencyKey, idempotencyKey)
                    .last("limit 1"));
            if (existing != null) {
                return taskView(existing);
            }
        }
        MediaUpload upload = uploadMapper.selectOne(new LambdaQueryWrapper<MediaUpload>()
                .eq(MediaUpload::getMediaAssetId, mediaAssetId)
                .eq(MediaUpload::getSessionId, sessionId)
                .last("limit 1"));
        if (upload == null || !"READY".equals(upload.getState())) {
            throw new BaseException("照片还不能开始识别");
        }
        LocalDateTime now = LocalDateTime.now();
        RecognitionTask task = new RecognitionTask();
        task.setId(id("task"));
        task.setSessionId(sessionId);
        task.setMediaAssetId(upload.getMediaAssetId());
        task.setIdempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null);
        task.setMediaType(upload.getMediaType());
        task.setSpeciesId(speciesIdOf(animalId));
        task.setTaskState("QUEUED");
        task.setResultKind("PROCESSING");
        task.setReviewState("NOT_REQUIRED");
        task.setVisible(1);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
        audit("visitor:" + sessionId, "TASK_CREATED", "recognition_task", task.getId(), upload.getMediaType());
        workers.execute(() -> runRecognition(task.getId()));
        return taskView(task);
    }

    public Map<String, Object> getTask(String taskId) {
        RecognitionTask task = ownedTask(taskId, false);
        expireIfNeeded(task);
        return taskView(taskMapper.selectById(task.getId()));
    }

    public Map<String, Object> history() {
        List<RecognitionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<RecognitionTask>()
                .eq(RecognitionTask::getSessionId, requiredSession())
                .eq(RecognitionTask::getVisible, 1)
                .orderByDesc(RecognitionTask::getUpdatedAt));
        List<Map<String, Object>> items = new ArrayList<>();
        for (RecognitionTask task : tasks) {
            expireIfNeeded(task);
            items.add(taskView(taskMapper.selectById(task.getId())));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        return body;
    }

    public Map<String, Object> deleteHistory(String taskId) {
        RecognitionTask task = ownedTask(taskId, true);
        task.setVisible(0);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        audit("visitor:" + task.getSessionId(), "HISTORY_DELETED", "recognition_task", task.getId(), "游客删除单条历史");
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("id", "del_" + task.getId());
        job.put("state", "COMPLETED");
        job.put("scope", "ONE_HISTORY");
        return job;
    }

    public Map<String, Object> feedback(String taskId, String verdict) {
        if (!Set.of("CORRECT", "INCORRECT", "UNSURE").contains(verdict)) {
            throw new BaseException("反馈只能是正确、不正确或不确定");
        }
        RecognitionTask task = ownedTask(taskId, false);
        task.setFeedbackVerdict(verdict);
        task.setUpdatedAt(LocalDateTime.now());
        if ("INCORRECT".equals(verdict)) {
            task.setTaskState("REVIEW_PENDING");
            task.setResultKind("PENDING_REVIEW");
            task.setReviewState("OPEN");
        }
        taskMapper.updateById(task);
        audit("visitor:" + task.getSessionId(), "FEEDBACK", "recognition_task", task.getId(), verdict);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("verdict", verdictLabel(verdict));
        view.put("status", "已提交");
        return view;
    }

    public Map<String, Object> privacyRequest() {
        String sessionId = requiredSession();
        String requestId = id("privacy");
        audit("visitor:" + sessionId, "PRIVACY_REQUEST", "privacy_request", requestId, "访问或更正申请");
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("id", requestId);
        job.put("state", "COMPLETED");
        job.put("scope", "PRIVACY_REQUEST");
        return job;
    }

    public List<Map<String, Object>> publicElephants() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Individual individual : activeElephants()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(individual.getId()));
            item.put("displayName", individual.getName());
            item.put("coverEmoji", "🐘");
            items.add(item);
        }
        return items;
    }

    public Map<String, Object> publicElephant(String id) {
        Individual individual = individualMapper.selectById(Long.valueOf(id));
        if (individual == null || !isElephant(individual)) {
            throw new BaseException("公开档案暂不可用");
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", String.valueOf(individual.getId()));
        profile.put("displayName", individual.getName());
        profile.put("coverEmoji", "🐘");
        profile.put("heroMark", StringUtils.hasText(individual.getSpeciesName()) ? individual.getSpeciesName() : "亚洲象");
        profile.put("sexLabel", sexLabel(individual.getGender()));
        profile.put("ageLabel", ageLabel(individual.getBirthDate()));
        profile.put("story", StringUtils.hasText(individual.getDescription()) ? individual.getDescription() : "这是园区已发布的亚洲象档案。");
        profile.put("traits", List.of("亚洲象", "已发布档案"));
        profile.put("conservation", "请保持安全距离，不使用闪光灯，不越过围栏。");
        profile.put("sourceNotice", "只展示已发布的公开资料，不包含内部编号和精确位置。");
        return profile;
    }

    public List<Map<String, Object>> adminEvents() {
        List<RecognitionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<RecognitionTask>()
                .orderByDesc(RecognitionTask::getCreatedAt)
                .last("limit 100"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RecognitionTask task : tasks) {
            rows.add(adminView(task));
        }
        return rows;
    }

    public List<Map<String, Object>> reviewQueue() {
        List<RecognitionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<RecognitionTask>()
                .eq(RecognitionTask::getReviewState, "OPEN")
                .orderByAsc(RecognitionTask::getCreatedAt));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RecognitionTask task : tasks) {
            rows.add(adminView(task));
        }
        return rows;
    }

    public List<Map<String, Object>> auditEvents() {
        List<AuditEvent> events = auditMapper.selectList(new LambdaQueryWrapper<AuditEvent>()
                .orderByDesc(AuditEvent::getCreatedAt)
                .last("limit 200"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AuditEvent event : events) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", event.getId());
            row.put("actor", event.getActor());
            row.put("action", event.getAction());
            row.put("resourceType", event.getResourceType());
            row.put("resourceId", event.getResourceId());
            row.put("detail", event.getDetail());
            row.put("createdAt", event.getCreatedAt() == null ? "" : TIME.format(event.getCreatedAt()));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> conclude(String taskId, String decision, Long individualId) {
        RecognitionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("复核任务不存在");
        }
        task.setUpdatedAt(LocalDateTime.now());
        task.setTaskState("COMPLETED");
        task.setReviewState("RESOLVED");
        String detail;
        if ("CONFIRM".equals(decision)) {
            if (individualId == null) {
                throw new BaseException("确认个体时需要选择档案");
            }
            Individual individual = individualMapper.selectById(individualId);
            if (individual == null) {
                throw new BaseException("个体不存在");
            }
            task.setIndividualId(individual.getId());
            task.setPredictedName(individual.getName());
            task.setResultKind("KNOWN");
            detail = "确认 " + individual.getName();
        } else if ("UNKNOWN".equals(decision)) {
            task.setIndividualId(null);
            task.setResultKind("UNKNOWN");
            detail = "标为无法确认";
        } else {
            throw new BaseException("复核结论只能是确认或无法确认");
        }
        taskMapper.updateById(task);
        if (task.getRecordId() != null) {
            RecognitionRecord record = recordMapper.selectById(task.getRecordId());
            if (record != null) {
                record.setPredictedName("KNOWN".equals(task.getResultKind()) ? task.getPredictedName() : "无法确认");
                record.setIndividualId(task.getIndividualId());
                record.setStatus("success");
                record.setUpdateTime(LocalDateTime.now());
                recordMapper.updateById(record);
            }
        }
        Long adminId = BaseContext.getCurrentId();
        audit("admin:" + (adminId == null ? "unknown" : adminId), "REVIEW_CONCLUDED", "recognition_task", task.getId(), detail);
        return adminView(task);
    }

    private void runRecognition(String taskId) {
        RecognitionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setTaskState("PROCESSING");
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        try {
            MediaUpload upload = uploadMapper.selectOne(new LambdaQueryWrapper<MediaUpload>()
                    .eq(MediaUpload::getMediaAssetId, task.getMediaAssetId())
                    .last("limit 1"));
            byte[] bytes = Files.readAllBytes(Path.of(upload.getStoragePath()));
            String response = "video".equals(task.getMediaType())
                    ? requestVideoRecognition(bytes)
                    : requestImageRecognition(task.getId(), bytes);
            applyModelResult(task, upload, JSON.readTree(response == null ? "{}" : response));
        } catch (Exception ex) {
            task.setTaskState("FAILED");
            task.setResultKind("FAILED");
            task.setReviewState("NOT_REQUIRED");
            task.setErrorMessage(limit(ex.getMessage(), 500, "识别服务暂时不可用"));
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            saveRecord(task, null, "failed");
            audit("system", "TASK_FAILED", "recognition_task", task.getId(), task.getErrorMessage());
        }
    }

    private String requestVideoRecognition(byte[] bytes) {
        return restClient.post()
                .uri(recognitionProperties.getPredictVideoUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("video_base64", Base64.getEncoder().encodeToString(bytes)))
                .retrieve()
                .body(String.class);
    }

    private String requestImageRecognition(String sessionId, byte[] bytes) throws Exception {
        if (!StringUtils.hasText(recognitionProperties.getApiKey())) {
            throw new BaseException("未配置识别服务 API Key");
        }
        byte[] jpeg = jpegBytes(bytes);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("image", new ByteArrayResource(jpeg) {
            @Override
            public String getFilename() {
                return "frame.jpg";
            }
        });
        parts.add("session_id", sessionId);
        parts.add("species", "elephant");
        try {
            return restClient.post()
                    .uri(recognitionProperties.getPredictUrl())
                    .header("X-Api-Key", recognitionProperties.getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new BaseException("识别服务返回 " + ex.getStatusCode().value() + "："
                    + limit(ex.getResponseBodyAsString(), 300, "无详情"));
        }
    }

    private static byte[] jpegBytes(byte[] bytes) throws Exception {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return bytes;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new BaseException("照片需要是 JPEG、PNG 或 WebP");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", out)) {
            throw new BaseException("照片无法转成 JPEG");
        }
        return out.toByteArray();
    }

    private void applyModelResult(RecognitionTask task, MediaUpload upload, JsonNode root) {
        if ("video".equals(task.getMediaType())) {
            int count = root.path("elephant_count").asInt(root.path("elephants").size());
            task.setQualityReason(count + "只 / " + root.path("frame_count").asInt(0) + "帧");
            if (count <= 0) {
                finish(task, upload, "NO_ELEPHANT", "NOT_REQUIRED", null, null);
                return;
            }
            if (count > 1) {
                JsonNode first = root.path("elephants").isArray() && !root.path("elephants").isEmpty()
                        ? root.path("elephants").get(0) : null;
                String name = first == null ? "多只大象" : text(first.path("name").asText(null));
                finish(task, upload, "MULTIPLE", "NOT_REQUIRED", name, confidenceOf(first == null ? null : first.get("confidence")));
                return;
            }
            JsonNode only = root.path("elephants").isArray() && !root.path("elephants").isEmpty()
                    ? root.path("elephants").get(0) : root;
            judgeSingle(task, upload, text(only.path("name").asText(null)), confidenceOf(only.get("confidence")));
            return;
        }
        if (root.path("tracks").isArray()) {
            applyTracks(task, upload, root.path("tracks"));
            return;
        }
        String name = text(root.path("elephant_name").asText(null));
        if (!StringUtils.hasText(name)) {
            name = text(root.path("name").asText(null));
        }
        judgeSingle(task, upload, name, confidenceOf(root.get("confidence")));
    }

    private void applyTracks(RecognitionTask task, MediaUpload upload, JsonNode tracks) {
        Map<String, BigDecimal> confirmed = new LinkedHashMap<>();
        boolean pending = false;
        for (JsonNode track : tracks) {
            String name = text(track.path("name").asText(null));
            if (!StringUtils.hasText(name)) {
                name = text(track.path("elephant_name").asText(null));
            }
            if (!StringUtils.hasText(name) || UNCONFIRMED_NAMES.contains(name)) {
                pending = pending || StringUtils.hasText(name);
                continue;
            }
            confirmed.putIfAbsent(name, trackConfidence(track));
        }
        if (confirmed.isEmpty()) {
            finish(task, upload, pending ? "UNKNOWN" : "NO_ELEPHANT", "NOT_REQUIRED", null, null);
            return;
        }
        Map.Entry<String, BigDecimal> first = confirmed.entrySet().iterator().next();
        if (confirmed.size() > 1) {
            finish(task, upload, "MULTIPLE", "NOT_REQUIRED", first.getKey(), first.getValue());
            return;
        }
        judgeSingle(task, upload, first.getKey(), first.getValue());
    }

    private static BigDecimal trackConfidence(JsonNode track) {
        for (String field : List.of("confidence", "score", "prob")) {
            BigDecimal value = confidenceOf(track.get(field));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void judgeSingle(RecognitionTask task, MediaUpload upload, String name, BigDecimal confidence) {
        task.setPredictedName(name);
        task.setConfidence(confidence);
        if (!StringUtils.hasText(name)) {
            finish(task, upload, "UNKNOWN", "NOT_REQUIRED", null, confidence);
            return;
        }
        Individual matched = null;
        if (task.getSpeciesId() != null) {
            matched = individualMapper.selectOne(new LambdaQueryWrapper<Individual>()
                    .eq(Individual::getName, name)
                    .eq(Individual::getSpeciesId, task.getSpeciesId())
                    .last("limit 1"));
        }
        if (matched != null) {
            task.setIndividualId(matched.getId());
        }
        if (confidence == null || confidence.compareTo(recognitionProperties.getKnownThreshold()) >= 0) {
            finish(task, upload, "KNOWN", "NOT_REQUIRED", name, confidence);
            return;
        }
        if (confidence != null && confidence.compareTo(recognitionProperties.getReviewThreshold()) >= 0) {
            finish(task, upload, "PENDING_REVIEW", "OPEN", name, confidence);
            return;
        }
        finish(task, upload, "UNKNOWN", "NOT_REQUIRED", name, confidence);
    }

    private void finish(RecognitionTask task, MediaUpload upload, String resultKind, String reviewState, String name, BigDecimal confidence) {
        task.setResultKind(resultKind);
        task.setReviewState(reviewState);
        task.setTaskState("PENDING_REVIEW".equals(resultKind) ? "REVIEW_PENDING" : "COMPLETED");
        task.setPredictedName(name);
        task.setConfidence(confidence);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        saveRecord(task, upload, "FAILED".equals(resultKind) ? "failed" : "success");
        audit("system", "TASK_COMPLETED", "recognition_task", task.getId(), resultKind);
    }

    private void saveRecord(RecognitionTask task, MediaUpload upload, String status) {
        RecognitionRecord record = new RecognitionRecord();
        record.setMediaType(task.getMediaType());
        record.setSource("miniprogram");
        record.setSourceType(upload == null ? "album" : upload.getSource());
        record.setOperatorName("游客");
        record.setUserId(null);
        record.setMediaUrl(mediaUrl(upload));
        record.setPredictedName(displayName(task));
        record.setConfidence(task.getConfidence());
        record.setIndividualId(task.getIndividualId());
        record.setStatus(status);
        record.setErrorMessage(task.getErrorMessage());
        record.setElephantCount("MULTIPLE".equals(task.getResultKind()) ? 2 : null);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.insert(record);
        task.setRecordId(record.getId());
        taskMapper.updateById(task);
    }

    private void expireIfNeeded(RecognitionTask task) {
        if (task.getVisible() != null && task.getVisible() == 0) {
            return;
        }
        if (!Set.of("QUEUED", "PROCESSING").contains(task.getTaskState())) {
            return;
        }
        if (task.getUpdatedAt() != null && task.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(3))) {
            task.setTaskState("FAILED");
            task.setResultKind("TIMED_OUT");
            task.setReviewState("NOT_REQUIRED");
            task.setErrorMessage("识别等待超时");
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            audit("system", "TASK_TIMED_OUT", "recognition_task", task.getId(), "超过 3 分钟没有结果");
        }
    }

    private Long speciesIdOf(String animalId) {
        String speciesName = MiniProgramAnimals.speciesName(animalId);
        if (!StringUtils.hasText(speciesName)) {
            return null;
        }
        Species species = speciesMapper.selectOne(new LambdaQueryWrapper<Species>()
                .eq(Species::getName, speciesName)
                .last("limit 1"));
        return species == null ? null : species.getId();
    }

    private Map<String, Object> individualProfile(String kind, Long individualId) {
        if (individualId == null || Set.of("MULTIPLE", "NO_ELEPHANT", "FAILED", "TIMED_OUT", "PROCESSING").contains(kind)) {
            return null;
        }
        Individual individual = individualMapper.selectById(individualId);
        if (individual == null) {
            return null;
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", String.valueOf(individual.getId()));
        profile.put("name", filled(individual.getName()));
        profile.put("genderLabel", StringUtils.hasText(individual.getGender()) ? sexLabel(individual.getGender()) : "未填写");
        profile.put("birthDate", individual.getBirthDate() == null ? "未填写" : individual.getBirthDate().toString());
        profile.put("birthPlace", filled(individual.getBirthPlace()));
        profile.put("personality", filled(individual.getPersonality()));
        profile.put("father", filled(individual.getFather()));
        profile.put("mother", filled(individual.getMother()));
        profile.put("description", filled(individual.getDescription()));
        profile.put("imageUrl", StringUtils.hasText(individual.getImageUrl()) ? individual.getImageUrl() : null);
        return profile;
    }

    private static String filled(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未填写";
    }

    private Map<String, Object> taskView(RecognitionTask task) {
        String kind = task.getResultKind() == null ? "PROCESSING" : task.getResultKind();
        boolean terminal = Set.of("COMPLETED", "FAILED").contains(task.getTaskState())
                && !"PENDING_REVIEW".equals(kind)
                && !"REVIEW_PENDING".equals(task.getTaskState());
        String[] presentation = presentation(kind, task.getPredictedName());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", task.getId());
        view.put("taskState", task.getTaskState());
        view.put("resultKind", kind);
        view.put("reviewState", task.getReviewState());
        view.put("terminal", terminal || "FAILED".equals(task.getTaskState()));
        view.put("title", presentation[0]);
        view.put("description", presentation[1]);
        view.put("statusLabel", presentation[2]);
        view.put("tone", presentation[3]);
        view.put("icon", presentation[4]);
        view.put("animalName", historyName(kind, task.getPredictedName(), presentation[2]));
        view.put("animalIcon", "🐘");
        view.put("updatedAtLabel", task.getUpdatedAt() == null ? "" : TIME.format(task.getUpdatedAt()));
        view.put("publicProfileRef", "KNOWN".equals(kind) && task.getIndividualId() != null ? String.valueOf(task.getIndividualId()) : null);
        view.put("individual", individualProfile(kind, task.getIndividualId()));
        view.put("supportsFeedback", !Set.of("FAILED", "TIMED_OUT", "PROCESSING").contains(kind));
        view.put("targets", targets(task));
        view.put("qualityReasons", StringUtils.hasText(task.getQualityReason()) ? List.of(task.getQualityReason()) : List.of());
        view.put("speciesResults", List.of());
        view.put("resultSource", "RESOLVED".equals(task.getReviewState()) ? "reviewed" : "automatic");
        if (StringUtils.hasText(task.getFeedbackVerdict())) {
            Map<String, Object> feedback = new LinkedHashMap<>();
            feedback.put("verdict", verdictLabel(task.getFeedbackVerdict()));
            feedback.put("status", "已提交");
            view.put("feedback", feedback);
        } else {
            view.put("feedback", null);
        }
        return view;
    }

    private List<Map<String, Object>> targets(RecognitionTask task) {
        if (!"MULTIPLE".equals(task.getResultKind())) {
            return List.of();
        }
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("id", task.getId() + "-1");
        target.put("order", 1);
        target.put("title", StringUtils.hasText(task.getPredictedName()) ? task.getPredictedName() : "未单独确认");
        target.put("statusLabel", "多目标");
        target.put("tone", "info");
        target.put("publicProfileRef", task.getIndividualId() == null ? null : String.valueOf(task.getIndividualId()));
        return List.of(target);
    }

    private Map<String, Object> adminView(RecognitionTask task) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", task.getId());
        row.put("mediaType", task.getMediaType());
        row.put("taskState", task.getTaskState());
        row.put("resultKind", task.getResultKind());
        row.put("reviewState", task.getReviewState());
        row.put("predictedName", task.getPredictedName());
        row.put("confidence", task.getConfidence());
        row.put("individualId", task.getIndividualId());
        row.put("visible", task.getVisible());
        row.put("errorMessage", task.getErrorMessage());
        row.put("createdAt", task.getCreatedAt() == null ? "" : TIME.format(task.getCreatedAt()));
        row.put("statusLabel", presentation(task.getResultKind() == null ? "PROCESSING" : task.getResultKind(), task.getPredictedName())[2]);
        return row;
    }

    private String historyName(String kind, String predictedName, String statusLabel) {
        return switch (kind) {
            case "KNOWN" -> StringUtils.hasText(predictedName) ? predictedName : "已确认";
            case "PENDING_REVIEW" -> "暂未确认";
            case "MULTIPLE" -> "多只大象";
            case "NO_ELEPHANT" -> "未检测到大象";
            case "UNKNOWN" -> "无法确认";
            default -> statusLabel;
        };
    }

    private String[] presentation(String kind, String name) {
        return switch (kind) {
            case "KNOWN" -> new String[]{"认识它了", StringUtils.hasText(name) ? "这是「" + name + "」。" : "已确认到公开档案中的个体。", "已确认", "success", "🐘"};
            case "PENDING_REVIEW" -> new String[]{"暂未确认识别结果", "你可以稍后在最近识别里查看。", "待复核", "warning", "🔎"};
            case "UNKNOWN" -> new String[]{"暂时认不出它", "可以换一张更清楚的照片再试。", "无法确认", "neutral", "🌿"};
            case "MULTIPLE" -> new String[]{"画面里有多只大象", "请尽量拍摄单只大象后再识别。", "多目标", "info", "🐘"};
            case "NO_ELEPHANT" -> new String[]{"没有找到大象", "请确认大象已经进入画面。", "未检测到", "neutral", "👀"};
            case "TIMED_OUT" -> new String[]{"识别等待超时", "请重新选择照片再试。", "已超时", "danger", "⌛"};
            case "FAILED" -> new String[]{"识别没有完成", "请稍后重新选择照片再试。", "处理失败", "danger", "↻"};
            default -> new String[]{"正在识别", "照片已受理，结果出来后会显示在这里。", "处理中", "info", "···"};
        };
    }

    private String displayName(RecognitionTask task) {
        return switch (task.getResultKind() == null ? "" : task.getResultKind()) {
            case "KNOWN" -> task.getPredictedName();
            case "PENDING_REVIEW" -> "暂未确认";
            case "MULTIPLE" -> "多只大象";
            case "NO_ELEPHANT" -> "未检测到大象";
            case "UNKNOWN" -> "无法确认";
            default -> task.getErrorMessage() == null ? "识别未完成" : task.getErrorMessage();
        };
    }

    private String mediaUrl(MediaUpload upload) {
        if (upload == null || !StringUtils.hasText(upload.getStoragePath())) {
            return null;
        }
        String fileName = Path.of(upload.getStoragePath()).getFileName().toString();
        String base = recognitionProperties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + uploadProperties.getAccessPath() + "/visitor/" + fileName;
    }

    private List<Individual> activeElephants() {
        return individualMapper.selectList(new LambdaQueryWrapper<Individual>()
                .orderByAsc(Individual::getName))
                .stream()
                .filter(this::isElephant)
                .toList();
    }

    private static String ageLabel(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return "未公开";
        }
        return Period.between(birthDate, LocalDate.now()).getYears() + "岁";
    }

    private boolean isElephant(Individual individual) {
        String species = individual.getSpeciesName() == null ? "" : individual.getSpeciesName();
        String name = individual.getName() == null ? "" : individual.getName();
        return species.contains("象") || name.contains("象") || !StringUtils.hasText(species);
    }

    private MediaUpload ownedUpload(String uploadId) {
        MediaUpload upload = uploadMapper.selectById(uploadId);
        if (upload == null || !requiredSession().equals(upload.getSessionId())) {
            throw new BaseException("上传会话不存在");
        }
        if (upload.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BaseException("上传已过期，请重新选择照片");
        }
        return upload;
    }

    private RecognitionTask ownedTask(String taskId, boolean allowHidden) {
        RecognitionTask task = taskMapper.selectById(taskId);
        if (task == null || !requiredSession().equals(task.getSessionId())) {
            throw new BaseException("没有找到这条识别记录");
        }
        if (!allowHidden && task.getVisible() != null && task.getVisible() == 0) {
            throw new BaseException("这条记录已删除");
        }
        return task;
    }

    private String requiredSession() {
        String sessionId = VisitorContext.get();
        if (!StringUtils.hasText(sessionId)) {
            throw new BaseException("游客会话无效");
        }
        return sessionId;
    }

    private void audit(String actor, String action, String resourceType, String resourceId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setDetail(limit(detail, 500, ""));
        event.setCreatedAt(LocalDateTime.now());
        auditMapper.insert(event);
    }

    private Map<String, Object> uploadView(MediaUpload upload) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", upload.getId());
        view.put("mediaAssetId", upload.getMediaAssetId());
        view.put("state", upload.getState());
        view.put("expiresAt", upload.getExpiresAt().toString());
        return view;
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private static BigDecimal confidenceOf(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        BigDecimal value = node.decimalValue();
        if (value.compareTo(BigDecimal.ONE) <= 0) {
            value = value.multiply(new BigDecimal("100"));
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String extension(String contentType) {
        if ("image/png".equals(contentType)) return "png";
        if ("image/webp".equals(contentType)) return "webp";
        if ("video/quicktime".equals(contentType)) return "mov";
        if (contentType != null && contentType.startsWith("video/")) return "mp4";
        return "jpg";
    }

    private static String sexLabel(String gender) {
        if (!StringUtils.hasText(gender)) return "未公开";
        String value = gender.toLowerCase();
        if (value.contains("female") || "f".equals(value) || gender.contains("母") || gender.contains("女")) return "雌性";
        if (value.contains("male") || "m".equals(value) || gender.contains("公") || gender.contains("男")) return "雄性";
        return gender;
    }

    private static String verdictLabel(String verdict) {
        return switch (verdict) {
            case "CORRECT" -> "正确";
            case "INCORRECT" -> "不正确";
            case "UNSURE" -> "不确定";
            default -> verdict;
        };
    }

    private static String limit(String value, int max, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
