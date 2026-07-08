package com.elephantjava.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.SpeciesDTO;
import com.elephantjava.entity.Species;
import com.elephantjava.vo.SpeciesVO;

import java.util.List;

/**
 * 物种业务接口
 */
public interface ISpeciesService extends IService<Species> {

    /**
     * 查询全部物种列表
     */
    List<SpeciesVO> listAll();

    /**
     * 新增物种
     */
    SpeciesVO saveSpecies(SpeciesDTO speciesDTO);

    /**
     * 更新物种
     */
    SpeciesVO updateSpecies(Long id, SpeciesDTO speciesDTO);

    /**
     * 删除物种
     */
    void deleteSpecies(Long id);
}
