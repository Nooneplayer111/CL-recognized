package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 物种新增/修改请求参数
 */
@Data
public class SpeciesDTO implements Serializable {

    /** 物种名称 */
    private String name;

    /** 物种编码（唯一标识） */
    private String code;

    /** 物种描述 */
    private String description;

    /** 物种图片URL */
    private String imageUrl;
}
