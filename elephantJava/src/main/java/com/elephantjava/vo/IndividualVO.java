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
    private Long speciesId;
    private String speciesName;
    private String gender;
    /** 出生日期，格式 yyyy-MM-dd */
    private String birthDate;
    private String birthPlace;
    private String personality;
    private String father;
    private String mother;
    private String description;
    private String imageUrl;
    /** 创建时间，格式 yyyy-MM-dd */
    private String createTime;
}
