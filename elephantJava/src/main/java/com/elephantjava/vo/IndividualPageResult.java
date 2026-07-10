package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 个体分页响应对象（适配前端分页格式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPageResult implements Serializable {

    /** 当前页数据 */
    private List<IndividualVO> content;

    /** 总记录数 */
    private long totalElements;

    /** 总页数 */
    private long totalPages;

    /** 当前页码（从0开始） */
    private long number;

    /** 每页条数 */
    private long size;
}
