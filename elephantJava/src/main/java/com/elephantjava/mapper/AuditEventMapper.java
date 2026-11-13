package com.elephantjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elephantjava.entity.AuditEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditEventMapper extends BaseMapper<AuditEvent> {
}
