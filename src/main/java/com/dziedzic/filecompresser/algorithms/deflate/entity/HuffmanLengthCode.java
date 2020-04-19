package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

public class HuffmanLengthCode {
    private int lengthCode;
    private int bitsNumber;
    private int prefixCode;

    public HuffmanLengthCode(int lengthCode, int bitsNumber, int prefixCode) {
        this.lengthCode = lengthCode;
        this.bitsNumber = bitsNumber;
        this.prefixCode = prefixCode;
    }

    public int getLengthCode() {
        return lengthCode;
    }

    public int getBitsNumber() {
        return bitsNumber;
    }

    public int getPrefixCode() {
        return prefixCode;
    }
}
