package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.CameraNotFoundException;
import com.elephantjava.dto.CameraDTO;
import com.elephantjava.entity.Camera;
import com.elephantjava.mapper.CameraMapper;
import com.elephantjava.service.ICameraService;
import com.elephantjava.vo.CameraVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 摄像头业务实现类
 */
@Service
public class CameraServiceImpl extends ServiceImpl<CameraMapper, Camera> implements ICameraService {

    private static final String MASKED_PASSWORD = "******";

    /**
     * 查询全部摄像头，按创建时间倒序
     */
    @Override
    public List<CameraVO> listAll() {
        LambdaQueryWrapper<Camera> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Camera::getCreateTime);
        return this.list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 新增摄像头
     */
    @Override
    public CameraVO saveCamera(CameraDTO cameraDTO) {
        validateCameraDTO(cameraDTO);

        Camera camera = buildCamera(cameraDTO);
        camera.setStatus("offline");
        camera.setPreviewUrl(buildPreviewUrl(camera));
        camera.setCreateTime(LocalDateTime.now());
        camera.setUpdateTime(LocalDateTime.now());
        this.save(camera);
        return convertToVO(camera);
    }

    /**
     * 更新摄像头
     */
    @Override
    public CameraVO updateCamera(Long id, CameraDTO cameraDTO) {
        Camera camera = getCameraOrThrow(id);
        validateCameraDTO(cameraDTO);

        String oldPassword = camera.getPassword();
        BeanUtils.copyProperties(cameraDTO, camera);

        // 编辑时前端可能回传脱敏密码，此时保留原密码
        if (!StringUtils.hasText(cameraDTO.getPassword()) || MASKED_PASSWORD.equals(cameraDTO.getPassword())) {
            camera.setPassword(oldPassword);
        }

        if (camera.getPort() == null) {
            camera.setPort(8080);
        }
        camera.setPreviewUrl(buildPreviewUrl(camera));
        camera.setUpdateTime(LocalDateTime.now());
        this.updateById(camera);
        return convertToVO(camera);
    }

    /**
     * 删除摄像头
     */
    @Override
    public void deleteCamera(Long id) {
        getCameraOrThrow(id);
        this.removeById(id);
    }

    /**
     * 校验必填字段
     */
    private void validateCameraDTO(CameraDTO cameraDTO) {
        if (!StringUtils.hasText(cameraDTO.getName())) {
            throw new IllegalArgumentException("摄像头名称不能为空");
        }
        if (!StringUtils.hasText(cameraDTO.getIpAddress())) {
            throw new IllegalArgumentException("IP地址不能为空");
        }
        if (!StringUtils.hasText(cameraDTO.getLocation())) {
            throw new IllegalArgumentException("安装位置不能为空");
        }
    }

    /**
     * 根据ID查询摄像头
     */
    private Camera getCameraOrThrow(Long id) {
        Camera camera = this.getById(id);
        if (camera == null) {
            throw new CameraNotFoundException("摄像头不存在");
        }
        return camera;
    }

    /**
     * 构建摄像头实体
     */
    private Camera buildCamera(CameraDTO cameraDTO) {
        Camera camera = new Camera();
        BeanUtils.copyProperties(cameraDTO, camera);
        if (camera.getPort() == null) {
            camera.setPort(8080);
        }
        return camera;
    }

    /**
     * 根据 IP 和端口生成预览地址
     */
    private String buildPreviewUrl(Camera camera) {
        return "http://" + camera.getIpAddress() + ":" + camera.getPort() + "/stream";
    }

    /**
     * 实体转 VO，密码做脱敏处理
     */
    private CameraVO convertToVO(Camera camera) {
        return CameraVO.builder()
                .id(camera.getId())
                .name(camera.getName())
                .ipAddress(camera.getIpAddress())
                .location(camera.getLocation())
                .port(camera.getPort())
                .username(camera.getUsername())
                .password(StringUtils.hasText(camera.getPassword()) ? MASKED_PASSWORD : null)
                .status(camera.getStatus())
                .previewUrl(camera.getPreviewUrl())
                .build();
    }
}
