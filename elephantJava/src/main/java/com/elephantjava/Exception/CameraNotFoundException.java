package com.elephantjava.Exception;

/**
 * 摄像头不存在异常
 */
public class CameraNotFoundException extends BaseException {

    public CameraNotFoundException(String msg) {
        super(msg);
    }
}
