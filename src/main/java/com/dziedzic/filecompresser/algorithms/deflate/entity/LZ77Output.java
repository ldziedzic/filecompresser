package com.dziedzic.filecompresser.algorithms.deflate.entity;

public class LZ77Output {
    int code;
    int extraBits;
    int additionalValue;
    int huffmanCode;
    int bitsNumber;
    boolean isDistanceCode;

    public LZ77Output(int code, int extraBits, int additionalValue) {
        this.code = code;
        this.extraBits = extraBits;
        this.additionalValue = additionalValue;
        this.isDistanceCode = false;
    }

    public LZ77Output(int code, int extraBits, int additionalValue, int bitsNumber, boolean isDistanceCode) {
        this.code = code;
        this.extraBits = extraBits;
        this.additionalValue = additionalValue;
        this.bitsNumber = bitsNumber;
        this.isDistanceCode = isDistanceCode;
    }

    public int getCode() {
        return code;
    }

    public int getExtraBits() {
        return extraBits;
    }

    public int getAdditionalValue() {
        return additionalValue;
    }

    public int getHuffmanCode() {
        return huffmanCode;
    }

    public void setHuffmanCode(int huffmanCode) {
        this.huffmanCode = huffmanCode;
    }

    public int getBitsNumber() {
        return bitsNumber;
    }

    public void setBitsNumber(int bitsNumber) {
        this.bitsNumber = bitsNumber;
    }

    public boolean isDistanceCode() {
        return isDistanceCode;
    }
}
