package com.elephantjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elephantjava.entity.Individual;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个体数据访问层（删除物种时校验是否有关联个体）
 */
@Mapper
public interface IndividualMapper extends BaseMapper<Individual> {
}
