package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.RecognitionRecordNotFoundException;
import com.elephantjava.contex.BaseContext;
import com.elephantjava.dto.RecognitionRecordSaveDTO;
import com.elephantjava.entity.Individual;
import com.elephantjava.entity.RecognitionRecord;
import com.elephantjava.entity.User;
import com.elephantjava.mapper.IndividualMapper;
import com.elephantjava.mapper.RecognitionRecordMapper;
import com.elephantjava.mapper.UserMapper;
import com.elephantjava.service.IRecognitionRecordService;
import com.elephantjava.utils.MediaStorageUtil;
import com.elephantjava.vo.RecognitionRecordPageResult;
import com.elephantjava.vo.RecognitionRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 识别记录业务实现类
 */
@Service
public class RecognitionRecordServiceImpl extends ServiceImpl<RecognitionRecordMapper, RecognitionRecord>
        implements IRecognitionRecordService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MediaStorageUtil mediaStorageUtil;

    @Autowired
    private IndividualMapper individualMapper;

    @Autowired
    private UserMapper userMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 分页查询识别记录
     */
    @Override
    public RecognitionRecordPageResult pageQuery(Integer page, Integer size, String keyword, String mediaType) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<RecognitionRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(RecognitionRecord::getPredictedName, keyword)
                    .or()
                    .like(RecognitionRecord::getOperatorName, keyword));
        }
        if (StringUtils.hasText(mediaType)) {
            queryWrapper.eq(RecognitionRecord::getMediaType, mediaType);
        }
        queryWrapper.orderByDesc(RecognitionRecord::getCreateTime);

        Page<RecognitionRecord> pageResult = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<RecognitionRecordVO> content = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return RecognitionRecordPageResult.builder()
                .content(content)
                .totalElements(pageResult.getTotal())
                .totalPages(pageResult.getPages())
                .number(pageNum - 1L)
                .size(pageSize)
                .build();
    }

    /**
     * 查询识别记录详情
     */
    @Override
    public RecognitionRecordVO getDetail(Long id) {
        RecognitionRecord record = getRecordOrThrow(id);
        return convertToVO(record);
    }

    /**
     * 保存识别记录
     */
    @Override
    public RecognitionRecordVO saveRecord(RecognitionRecordSaveDTO saveDTO) {
        if (!StringUtils.hasText(saveDTO.getMediaType())) {
            throw new IllegalArgumentException("媒体类型不能为空");
        }

        RecognitionRecord record = new RecognitionRecord();
        record.setMediaType(saveDTO.getMediaType());
        record.setSource(StringUtils.hasText(saveDTO.getSource()) ? saveDTO.getSource() : "miniprogram");
        record.setSourceType(saveDTO.getSourceType());
        record.setOperatorName(resolveOperatorName(saveDTO));
        record.setUserId(BaseContext.getCurrentId());
        record.setPredictedName(saveDTO.getPredictedName());
        record.setConfidence(saveDTO.getConfidence());
        record.setElephantCount(saveDTO.getElephantCount());
        record.setFrameCount(saveDTO.getFrameCount());
        record.setProcessedVideoUrl(saveDTO.getProcessedVideoUrl());
        record.setStatus(StringUtils.hasText(saveDTO.getStatus()) ? saveDTO.getStatus() : "success");
        record.setErrorMessage(saveDTO.getErrorMessage());

        // 保存图片到本地
        try {
            if (StringUtils.hasText(saveDTO.getImageBase64())) {
                record.setMediaUrl(mediaStorageUtil.saveBase64Image(saveDTO.getImageBase64()));
            } else if (StringUtils.hasText(saveDTO.getMediaUrl())) {
                record.setMediaUrl(saveDTO.getMediaUrl());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("识别图片保存失败");
        }

        // JSON 字段序列化
        record.setAllProbs(toJson(saveDTO.getAllProbs()));
        record.setElephants(toJson(saveDTO.getElephants()));

        // 视频类型时，主结果名可取第一头大象名称
        if ("video".equals(saveDTO.getMediaType()) && !StringUtils.hasText(record.getPredictedName())) {
            record.setPredictedName(buildVideoPredictedName(saveDTO));
        }

        // 按名称关联个体
        record.setIndividualId(matchIndividualId(record.getPredictedName()));
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        this.save(record);
        return convertToVO(record);
    }

    /**
     * 删除识别记录
     */
    @Override
    public void deleteRecord(Long id) {
        getRecordOrThrow(id);
        this.removeById(id);
    }

    private RecognitionRecord getRecordOrThrow(Long id) {
        RecognitionRecord record = this.getById(id);
        if (record == null) {
            throw new RecognitionRecordNotFoundException("识别记录不存在");
        }
        return record;
    }

    /**
     * 解析操作用户名称
     */
    private String resolveOperatorName(RecognitionRecordSaveDTO saveDTO) {
        if (StringUtils.hasText(saveDTO.getOperatorName())) {
            return saveDTO.getOperatorName();
        }
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
            }
        }
        return "小程序用户";
    }

    /**
     * 根据识别名称匹配个体ID
     */
    private Long matchIndividualId(String predictedName) {
        if (!StringUtils.hasText(predictedName)) {
            return null;
        }
        Individual individual = individualMapper.selectOne(
                new LambdaQueryWrapper<Individual>().eq(Individual::getName, predictedName).last("LIMIT 1"));
        return individual == null ? null : individual.getId();
    }

    /**
     * 构建视频识别主结果名称
     */
    private String buildVideoPredictedName(RecognitionRecordSaveDTO saveDTO) {
        if (saveDTO.getElephantCount() != null && saveDTO.getElephantCount() > 0) {
            return "识别到" + saveDTO.getElephantCount() + "头大象";
        }
        return "视频识别";
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("识别结果JSON序列化失败");
        }
    }

    /**
     * 实体转 VO
     */
    private RecognitionRecordVO convertToVO(RecognitionRecord record) {
        String individualName = null;
        if (record.getIndividualId() != null) {
            Individual individual = individualMapper.selectById(record.getIndividualId());
            if (individual != null) {
                individualName = individual.getName();
            }
        }

        String createTime = record.getCreateTime() == null
                ? null
                : record.getCreateTime().format(DATE_TIME_FORMATTER);

        return RecognitionRecordVO.builder()
                .id(record.getId())
                .mediaType(record.getMediaType())
                .source(record.getSource())
                .sourceType(record.getSourceType())
                .operatorName(record.getOperatorName())
                .mediaUrl(record.getMediaUrl())
                .processedVideoUrl(record.getProcessedVideoUrl())
                .predictedName(record.getPredictedName())
                .confidence(record.getConfidence())
                .allProbs(record.getAllProbs())
                .elephantCount(record.getElephantCount())
                .frameCount(record.getFrameCount())
                .elephants(record.getElephants())
                .individualId(record.getIndividualId())
                .individualName(individualName)
                .status(record.getStatus())
                .errorMessage(record.getErrorMessage())
                .createTime(createTime)
                .build();
    }
}
