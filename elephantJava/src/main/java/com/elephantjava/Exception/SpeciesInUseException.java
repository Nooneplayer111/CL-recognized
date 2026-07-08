package com.elephantjava.Exception;

/**
 * 物种已被个体引用，无法删除
 */
public class SpeciesInUseException extends BaseException {

    public SpeciesInUseException(String msg) {
        super(msg);
    }
}
