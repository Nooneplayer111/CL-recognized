package com.elephantjava.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.CameraDTO;
import com.elephantjava.entity.Camera;
import com.elephantjava.vo.CameraVO;

import java.util.List;

/**
 * 摄像头业务接口
 */
public interface ICameraService extends IService<Camera> {

    /**
     * 查询全部摄像头列表
     */
    List<CameraVO> listAll();

    /**
     * 新增摄像头
     */
    CameraVO saveCamera(CameraDTO cameraDTO);

    /**
     * 更新摄像头
     */
    CameraVO updateCamera(Long id, CameraDTO cameraDTO);

    /**
     * 删除摄像头
     */
    void deleteCamera(Long id);
}
