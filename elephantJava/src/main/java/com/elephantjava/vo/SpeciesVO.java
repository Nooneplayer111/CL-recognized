package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 物种信息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeciesVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String imageUrl;
}
