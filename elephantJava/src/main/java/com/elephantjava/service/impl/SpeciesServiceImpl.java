package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.SpeciesCodeDuplicateException;
import com.elephantjava.Exception.SpeciesInUseException;
import com.elephantjava.Exception.SpeciesNotFoundException;
import com.elephantjava.dto.SpeciesDTO;
import com.elephantjava.entity.Individual;
import com.elephantjava.entity.Species;
import com.elephantjava.mapper.IndividualMapper;
import com.elephantjava.mapper.SpeciesMapper;
import com.elephantjava.service.ISpeciesService;
import com.elephantjava.vo.SpeciesVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物种业务实现类
 */
@Service
public class SpeciesServiceImpl extends ServiceImpl<SpeciesMapper, Species> implements ISpeciesService {

    @Autowired
    private IndividualMapper individualMapper;

    /**
     * 查询全部物种，按创建时间倒序
     */
    @Override
    public List<SpeciesVO> listAll() {
        LambdaQueryWrapper<Species> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Species::getCreateTime);
        return this.list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 新增物种
     */
    @Override
    public SpeciesVO saveSpecies(SpeciesDTO speciesDTO) {
        validateSpeciesDTO(speciesDTO);
        checkCodeDuplicate(speciesDTO.getCode(), null);

        Species species = new Species();
        BeanUtils.copyProperties(speciesDTO, species);
        species.setCreateTime(LocalDateTime.now());
        species.setUpdateTime(LocalDateTime.now());

        this.save(species);
        return convertToVO(species);
    }

    /**
     * 更新物种
     */
    @Override
    public SpeciesVO updateSpecies(Long id, SpeciesDTO speciesDTO) {
        Species species = getSpeciesOrThrow(id);
        validateSpeciesDTO(speciesDTO);
        checkCodeDuplicate(speciesDTO.getCode(), id);

        BeanUtils.copyProperties(speciesDTO, species);
        species.setUpdateTime(LocalDateTime.now());
        this.updateById(species);
        return convertToVO(species);
    }

    /**
     * 删除物种（有关联个体时不允许删除）
     */
    @Override
    public void deleteSpecies(Long id) {
        getSpeciesOrThrow(id);

        Long relatedCount = individualMapper.selectCount(
                new LambdaQueryWrapper<Individual>().eq(Individual::getSpeciesId, id));
        if (relatedCount != null && relatedCount > 0) {
            throw new SpeciesInUseException("该物种下存在关联个体，无法删除");
        }

        this.removeById(id);
    }

    /**
     * 校验必填字段
     */
    private void validateSpeciesDTO(SpeciesDTO speciesDTO) {
        if (!StringUtils.hasText(speciesDTO.getName())) {
            throw new IllegalArgumentException("物种名称不能为空");
        }
        if (!StringUtils.hasText(speciesDTO.getCode())) {
            throw new IllegalArgumentException("物种编码不能为空");
        }
    }

    /**
     * 校验物种编码是否重复
     */
    private void checkCodeDuplicate(String code, Long excludeId) {
        LambdaQueryWrapper<Species> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Species::getCode, code);
        if (excludeId != null) {
            queryWrapper.ne(Species::getId, excludeId);
        }
        if (this.count(queryWrapper) > 0) {
            throw new SpeciesCodeDuplicateException("物种编码已存在");
        }
    }

    /**
     * 根据ID查询物种，不存在则抛异常
     */
    private Species getSpeciesOrThrow(Long id) {
        Species species = this.getById(id);
        if (species == null) {
            throw new SpeciesNotFoundException("物种不存在");
        }
        return species;
    }

    /**
     * 实体转 VO
     */
    private SpeciesVO convertToVO(Species species) {
        return SpeciesVO.builder()
                .id(species.getId())
                .name(species.getName())
                .code(species.getCode())
                .description(species.getDescription())
                .imageUrl(species.getImageUrl())
                .build();
    }
}
