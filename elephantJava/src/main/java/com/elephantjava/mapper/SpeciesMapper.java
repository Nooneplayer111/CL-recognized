package com.elephantjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elephantjava.entity.Species;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物种数据访问层
 */
@Mapper
public interface SpeciesMapper extends BaseMapper<Species> {
}
