package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

import java.time.LocalDate;
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
     * 分页查询个体，支持按名称关键词和物种ID筛选
     */
    @Override
    public IndividualPageResult pageQuery(Integer page, Integer size, String keyword, Long speciesId) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Individual> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Individual::getName, keyword.trim());
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
        normalize(individualDTO);
        validateIndividualDTO(individualDTO);
        Species species = getSpeciesOrThrow(individualDTO.getSpeciesId());

        Individual individual = new Individual();
        BeanUtils.copyProperties(individualDTO, individual);
        individual.setSpeciesName(species.getName());
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
        normalize(individualDTO);
        validateIndividualDTO(individualDTO);
        Species species = getSpeciesOrThrow(individualDTO.getSpeciesId());

        BeanUtils.copyProperties(individualDTO, individual);
        individual.setSpeciesName(species.getName());
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

    private void normalize(IndividualDTO individualDTO) {
        individualDTO.setName(trimToNull(individualDTO.getName()));
        individualDTO.setGender(trimToNull(individualDTO.getGender()));
        individualDTO.setBirthPlace(trimToNull(individualDTO.getBirthPlace()));
        individualDTO.setPersonality(trimToNull(individualDTO.getPersonality()));
        individualDTO.setFather(trimToNull(individualDTO.getFather()));
        individualDTO.setMother(trimToNull(individualDTO.getMother()));
        individualDTO.setDescription(trimToNull(individualDTO.getDescription()));
        individualDTO.setImageUrl(trimToNull(individualDTO.getImageUrl()));
    }

    /**
     * 校验必填字段和长度
     */
    private void validateIndividualDTO(IndividualDTO individualDTO) {
        if (!StringUtils.hasText(individualDTO.getName())) {
            throw new IllegalArgumentException("个体名称不能为空");
        }
        checkLength(individualDTO.getName(), 64, "个体名称");
        if (individualDTO.getSpeciesId() == null) {
            throw new IllegalArgumentException("所属物种不能为空");
        }
        if (!"male".equals(individualDTO.getGender()) && !"female".equals(individualDTO.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (individualDTO.getBirthDate() != null && individualDTO.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("出生日期不能晚于今天");
        }
        checkLength(individualDTO.getBirthPlace(), 128, "出生地");
        checkLength(individualDTO.getPersonality(), 255, "性格");
        checkLength(individualDTO.getFather(), 64, "父亲");
        checkLength(individualDTO.getMother(), 64, "母亲");
        checkLength(individualDTO.getDescription(), 1000, "描述");
        checkLength(individualDTO.getImageUrl(), 500, "个体图片");
    }

    private void checkLength(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(label + "不能超过" + max + "个字");
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
     * 实体转 VO
     */
    private IndividualVO convertToVO(Individual individual) {
        String createTime = individual.getCreateTime() == null
                ? null
                : individual.getCreateTime().format(DATE_FORMATTER);
        String birthDate = individual.getBirthDate() == null
                ? null
                : individual.getBirthDate().format(DATE_FORMATTER);
        return IndividualVO.builder()
                .id(individual.getId())
                .name(individual.getName())
                .speciesId(individual.getSpeciesId())
                .speciesName(individual.getSpeciesName())
                .gender(individual.getGender())
                .birthDate(birthDate)
                .birthPlace(individual.getBirthPlace())
                .personality(individual.getPersonality())
                .father(individual.getFather())
                .mother(individual.getMother())
                .description(individual.getDescription())
                .imageUrl(individual.getImageUrl())
                .createTime(createTime)
                .build();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
