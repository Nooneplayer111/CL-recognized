package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_session")
public class VisitorSession {

    @TableId(type = IdType.INPUT)
    private String id;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
