package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

public class DistanceCode {
    private int code;
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

    public int getExtraBits() {
        return extraBits;
    }

    public int getDistance() {
        return distance;
    }
}
