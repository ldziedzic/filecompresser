package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

public class HuffmanLengthCode {
    private int lengthCode;
    private int bitsNumber;
    private byte[] firstPrefixCode;
    private byte[] lastPrefixCode;

    public HuffmanLengthCode(int lengthCode, int bitsNumber, byte[] firstPrefixCode, byte[] lastPrefixCode) {
        this.lengthCode = lengthCode;
        this.bitsNumber = bitsNumber;
        this.firstPrefixCode = firstPrefixCode;
        this.lastPrefixCode = lastPrefixCode;
    }

    public int getLengthCode() {
        return lengthCode;
    }

    public int getBitsNumber() {
        return bitsNumber;
    }

    public byte[] getFirstPrefixCode() {
        return firstPrefixCode;
    }

    public byte[] getLastPrefixCode() {
        return lastPrefixCode;
    }
}
