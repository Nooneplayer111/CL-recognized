package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个体信息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private Long speciesId;
    private String speciesName;
    private String gender;
    private Integer age;
    private String status;
    private String description;
    /** 创建时间，格式 yyyy-MM-dd */
    private String createTime;
}
