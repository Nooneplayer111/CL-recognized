package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.IndividualCodeDuplicateException;
import com.elephantjava.Exception.IndividualNotFoundException;
import com.elephantjava.Exception.SpeciesNotFoundException;
import com.elephantjava.dto.IndividualDTO;
import com.elephantjava.entity.Individual;
import com.elephantjava.entity.Species;
import com.elephantjava.mapper.IndividualMapper;
import com.elephantjava.mapper.SpeciesMapper;
import com.elephantjava.service.IIndividualService;
import com.elephantjava.vo.IndividualPageResult;
import com.elephantjava.vo.IndividualVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个体业务实现类
 */
@Service
public class IndividualServiceImpl extends ServiceImpl<IndividualMapper, Individual> implements IIndividualService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private SpeciesMapper speciesMapper;

    /**
     * 分页查询个体，支持按名称/编号关键词和物种ID筛选
     */
    @Override
    public IndividualPageResult pageQuery(Integer page, Integer size, String keyword, Long speciesId) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Individual> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Individual::getName, keyword)
                    .or()
                    .like(Individual::getCode, keyword));
        }
        if (speciesId != null) {
            queryWrapper.eq(Individual::getSpeciesId, speciesId);
        }
        queryWrapper.orderByDesc(Individual::getCreateTime);

        Page<Individual> pageResult = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<IndividualVO> content = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return IndividualPageResult.builder()
                .content(content)
                .totalElements(pageResult.getTotal())
                .totalPages(pageResult.getPages())
                .number(pageNum - 1L)
                .size(pageSize)
                .build();
    }

    /**
     * 新增个体
     */
    @Override
    public IndividualVO saveIndividual(IndividualDTO individualDTO) {
        validateIndividualDTO(individualDTO);
        checkCodeDuplicate(individualDTO.getCode(), null);

        Species species = getSpeciesOrThrow(individualDTO.getSpeciesId());

        Individual individual = buildIndividual(individualDTO, species.getName());
        individual.setCreateTime(LocalDateTime.now());
        individual.setUpdateTime(LocalDateTime.now());
        this.save(individual);
        return convertToVO(individual);
    }

    /**
     * 更新个体
     */
    @Override
    public IndividualVO updateIndividual(Long id, IndividualDTO individualDTO) {
        Individual individual = getIndividualOrThrow(id);
        validateIndividualDTO(individualDTO);
        checkCodeDuplicate(individualDTO.getCode(), id);

        Species species = getSpeciesOrThrow(individualDTO.getSpeciesId());

        BeanUtils.copyProperties(individualDTO, individual);
        individual.setSpeciesName(species.getName());
        if (!StringUtils.hasText(individual.getStatus())) {
            individual.setStatus("active");
        }
        individual.setUpdateTime(LocalDateTime.now());
        this.updateById(individual);
        return convertToVO(individual);
    }

    /**
     * 删除个体
     */
    @Override
    public void deleteIndividual(Long id) {
        getIndividualOrThrow(id);
        this.removeById(id);
    }

    /**
     * 校验必填字段
     */
    private void validateIndividualDTO(IndividualDTO individualDTO) {
        if (!StringUtils.hasText(individualDTO.getName())) {
            throw new IllegalArgumentException("个体名称不能为空");
        }
        if (!StringUtils.hasText(individualDTO.getCode())) {
            throw new IllegalArgumentException("个体编号不能为空");
        }
        if (individualDTO.getSpeciesId() == null) {
            throw new IllegalArgumentException("所属物种不能为空");
        }
        if (!StringUtils.hasText(individualDTO.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
    }

    /**
     * 校验个体编号是否重复
     */
    private void checkCodeDuplicate(String code, Long excludeId) {
        LambdaQueryWrapper<Individual> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Individual::getCode, code);
        if (excludeId != null) {
            queryWrapper.ne(Individual::getId, excludeId);
        }
        if (this.count(queryWrapper) > 0) {
            throw new IndividualCodeDuplicateException("个体编号已存在");
        }
    }

    /**
     * 根据ID查询个体
     */
    private Individual getIndividualOrThrow(Long id) {
        Individual individual = this.getById(id);
        if (individual == null) {
            throw new IndividualNotFoundException("个体不存在");
        }
        return individual;
    }

    /**
     * 根据ID查询物种
     */
    private Species getSpeciesOrThrow(Long speciesId) {
        Species species = speciesMapper.selectById(speciesId);
        if (species == null) {
            throw new SpeciesNotFoundException("所属物种不存在");
        }
        return species;
    }

    /**
     * 构建个体实体
     */
    private Individual buildIndividual(IndividualDTO individualDTO, String speciesName) {
        Individual individual = new Individual();
        BeanUtils.copyProperties(individualDTO, individual);
        individual.setSpeciesName(speciesName);
        if (!StringUtils.hasText(individual.getStatus())) {
            individual.setStatus("active");
        }
        return individual;
    }

    /**
     * 实体转 VO
     */
    private IndividualVO convertToVO(Individual individual) {
        String createTime = individual.getCreateTime() == null
                ? null
                : individual.getCreateTime().format(DATE_FORMATTER);
        return IndividualVO.builder()
                .id(individual.getId())
                .name(individual.getName())
                .code(individual.getCode())
                .speciesId(individual.getSpeciesId())
                .speciesName(individual.getSpeciesName())
                .gender(individual.getGender())
                .age(individual.getAge())
                .status(individual.getStatus())
                .description(individual.getDescription())
                .createTime(createTime)
                .build();
    }
}
