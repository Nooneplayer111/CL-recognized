package com.elephantjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elephantjava.entity.RecognitionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 识别记录数据访问层
 */
@Mapper
public interface RecognitionRecordMapper extends BaseMapper<RecognitionRecord> {
}
