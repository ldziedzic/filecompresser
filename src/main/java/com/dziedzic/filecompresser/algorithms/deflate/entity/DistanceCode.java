package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

public class DistanceCode {
    private int code;
    private int bitsNumber = 5; // Default value for statics Distance Codes
    private int extraBits;
    private int distance;

    public DistanceCode(int code, int extraBits, int distance) {
        this.code = code;
        this.extraBits = extraBits;
        this.distance = distance;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getBitsNumber() {
        return bitsNumber;
    }

    public void setBitsNumber(int bitsNumber) {
        this.bitsNumber = bitsNumber;
    }

    public int getDistance() {
        return distance;
    }

    public int getExtraBits() {
        return extraBits;
    }

    public void setExtraBits(int extraBits) {
        this.extraBits = extraBits;
    }
}
