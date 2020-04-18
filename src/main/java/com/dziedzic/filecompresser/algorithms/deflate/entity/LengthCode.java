package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

public class LengthCode {
    private int code;
    private int extraBits;
    private int length;

    public LengthCode(int code, int extraBits, int length) {
        this.code = code;
        this.extraBits = extraBits;
        this.length = length;
    }

    public int getCode() {
        return code;
    }

    public int getExtraBits() {
        return extraBits;
    }

    public int getLength() {
        return length;
    }
}
