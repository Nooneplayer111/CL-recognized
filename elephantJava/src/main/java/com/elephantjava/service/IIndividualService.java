package com.elephantjava.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.IndividualDTO;
import com.elephantjava.entity.Individual;
import com.elephantjava.vo.IndividualPageResult;
import com.elephantjava.vo.IndividualVO;

/**
 * 个体业务接口
 */
public interface IIndividualService extends IService<Individual> {

    /**
     * 分页查询个体列表
     */
    IndividualPageResult pageQuery(Integer page, Integer size, String keyword, Long speciesId);

    /**
     * 新增个体
     */
    IndividualVO saveIndividual(IndividualDTO individualDTO);

    /**
     * 更新个体
     */
    IndividualVO updateIndividual(Long id, IndividualDTO individualDTO);

    /**
     * 删除个体
     */
    void deleteIndividual(Long id);
}
