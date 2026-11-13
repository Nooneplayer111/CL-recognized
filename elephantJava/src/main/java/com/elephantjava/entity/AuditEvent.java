package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_event")
public class AuditEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String actor;

    private String action;

    private String resourceType;

    private String resourceId;

    private String detail;

    private LocalDateTime createdAt;
}
