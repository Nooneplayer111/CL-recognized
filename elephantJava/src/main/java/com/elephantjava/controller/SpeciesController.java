package com.elephantjava.controller;

import com.elephantjava.dto.SpeciesDTO;
import com.elephantjava.result.Result;
import com.elephantjava.service.ISpeciesService;
import com.elephantjava.vo.SpeciesVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物种管理接口
 */
@RestController
@RequestMapping("/species")
@Slf4j
public class SpeciesController {

    @Autowired
    private ISpeciesService speciesService;

    /**
     * 获取物种列表
     * 前端 Species.vue 期望直接返回数组，因此不使用 Result 包装
     */
    @GetMapping
    public List<SpeciesVO> list() {
        log.info("查询物种列表");
        return speciesService.listAll();
    }

    /**
     * 添加物种
     */
    @PostMapping
    public Result<SpeciesVO> add(@RequestBody SpeciesDTO speciesDTO) {
        log.info("添加物种: {}", speciesDTO);
        SpeciesVO speciesVO = speciesService.saveSpecies(speciesDTO);
        return Result.success(speciesVO);
    }

    /**
     * 更新物种
     */
    @PutMapping("/{id}")
    public Result<SpeciesVO> update(@PathVariable Long id, @RequestBody SpeciesDTO speciesDTO) {
        log.info("更新物种 id={}, data={}", id, speciesDTO);
        SpeciesVO speciesVO = speciesService.updateSpecies(id, speciesDTO);
        return Result.success(speciesVO);
    }

    /**
     * 删除物种
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除物种 id={}", id);
        speciesService.deleteSpecies(id);
        return Result.success();
    }
}
