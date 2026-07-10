package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 个体新增/修改请求参数
 */
@Data
public class IndividualDTO implements Serializable {

    /** 个体名称 */
    private String name;

    /** 个体编号（唯一标识） */
    private String code;

    /** 所属物种ID */
    private Long speciesId;

    /** 性别：male / female */
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 描述信息 */
    private String description;

    /** 状态：active / inactive，默认 active */
    private String status;
}
