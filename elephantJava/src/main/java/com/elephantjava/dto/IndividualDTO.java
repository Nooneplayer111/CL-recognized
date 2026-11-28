package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 个体新增/修改请求参数
 */
@Data
public class IndividualDTO implements Serializable {

    /** 个体名称 */
    private String name;

    /** 所属物种ID */
    private Long speciesId;

    /** 性别：male / female */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 出生地 */
    private String birthPlace;

    /** 性格 */
    private String personality;

    /** 父亲，存名字，不关联其他个体 */
    private String father;

    /** 母亲，存名字，不关联其他个体 */
    private String mother;

    /** 描述信息 */
    private String description;

    /** 个体图片地址，来自上传接口 */
    private String imageUrl;
}
