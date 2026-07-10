package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 识别记录分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRecordPageResult implements Serializable {

    private List<RecognitionRecordVO> content;
    private long totalElements;
    private long totalPages;
    private long number;
    private long size;
}
