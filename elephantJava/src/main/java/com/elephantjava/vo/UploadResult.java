package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件上传响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult implements Serializable {

    /** 状态码：1成功，0失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 上传后的访问地址 */
    private String url;
}
