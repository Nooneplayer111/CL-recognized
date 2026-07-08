package com.elephantjava.Exception;

/**
 * 物种不存在异常
 */
public class SpeciesNotFoundException extends BaseException {

    public SpeciesNotFoundException(String msg) {
        super(msg);
    }
}
