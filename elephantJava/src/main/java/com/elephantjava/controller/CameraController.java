package com.elephantjava.controller;

import com.elephantjava.dto.CameraDTO;
import com.elephantjava.result.Result;
import com.elephantjava.service.ICameraService;
import com.elephantjava.vo.CameraVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 摄像头管理接口
 */
@RestController
@RequestMapping("/cameras")
@Slf4j
public class CameraController {

    @Autowired
    private ICameraService cameraService;

    /**
     * 获取摄像头列表
     * 前端期望直接返回数组，因此不使用 Result 包装
     */
    @GetMapping
    public List<CameraVO> list() {
        log.info("查询摄像头列表");
        return cameraService.listAll();
    }

    /**
     * 添加摄像头
     */
    @PostMapping
    public Result<CameraVO> add(@RequestBody CameraDTO cameraDTO) {
        log.info("添加摄像头: {}", cameraDTO);
        CameraVO cameraVO = cameraService.saveCamera(cameraDTO);
        return Result.success(cameraVO);
    }

    /**
     * 更新摄像头
     */
    @PutMapping("/{id}")
    public Result<CameraVO> update(@PathVariable Long id, @RequestBody CameraDTO cameraDTO) {
        log.info("更新摄像头 id={}, data={}", id, cameraDTO);
        CameraVO cameraVO = cameraService.updateCamera(id, cameraDTO);
        return Result.success(cameraVO);
    }

    /**
     * 删除摄像头
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除摄像头 id={}", id);
        cameraService.deleteCamera(id);
        return Result.success();
    }
}
