package com.elephantjava.controller;

import com.elephantjava.dto.IndividualDTO;
import com.elephantjava.result.Result;
import com.elephantjava.service.IIndividualService;
import com.elephantjava.vo.IndividualPageResult;
import com.elephantjava.vo.IndividualVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 个体管理接口
 */
@RestController
@RequestMapping("/individuals")
@Slf4j
public class IndividualController {

    @Autowired
    private IIndividualService individualService;

    /**
     * 分页查询个体列表
     * 前端期望直接返回分页对象，不使用 Result 包装
     */
    @GetMapping
    public IndividualPageResult page(
            @RequestParam Integer page,
            @RequestParam Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long speciesId) {
        log.info("分页查询个体: page={}, size={}, keyword={}, speciesId={}", page, size, keyword, speciesId);
        return individualService.pageQuery(page, size, keyword, speciesId);
    }

    /**
     * 添加个体
     */
    @PostMapping
    public Result<IndividualVO> add(@RequestBody IndividualDTO individualDTO) {
        log.info("添加个体: {}", individualDTO);
        IndividualVO individualVO = individualService.saveIndividual(individualDTO);
        return Result.success(individualVO);
    }

    /**
     * 更新个体
     */
    @PutMapping("/{id}")
    public Result<IndividualVO> update(@PathVariable Long id, @RequestBody IndividualDTO individualDTO) {
        log.info("更新个体 id={}, data={}", id, individualDTO);
        IndividualVO individualVO = individualService.updateIndividual(id, individualDTO);
        return Result.success(individualVO);
    }

    /**
     * 删除个体
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除个体 id={}", id);
        individualService.deleteIndividual(id);
        return Result.success();
    }
}
