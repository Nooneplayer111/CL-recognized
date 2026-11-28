package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("individual")
public class Individual {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("species_id")
    private Long speciesId;

    @TableField("species_name")
    private String speciesName;

    @TableField("gender")
    private String gender;

    @TableField(value = "birth_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate birthDate;

    @TableField(value = "birth_place", updateStrategy = FieldStrategy.ALWAYS)
    private String birthPlace;

    @TableField(value = "personality", updateStrategy = FieldStrategy.ALWAYS)
    private String personality;

    @TableField(value = "father", updateStrategy = FieldStrategy.ALWAYS)
    private String father;

    @TableField(value = "mother", updateStrategy = FieldStrategy.ALWAYS)
    private String mother;

    @TableField(value = "description", updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @TableField(value = "image_url", updateStrategy = FieldStrategy.ALWAYS)
    private String imageUrl;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
