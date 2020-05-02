package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 21.04.2020
 */

public class DistanceCodeOutput {
    int offset;
    int distance;

    public DistanceCodeOutput(int offset, int distance) {
        this.offset = offset;
        this.distance = distance;
    }

    public int getOffset() {
        return offset;
    }

    public int getDistance() {
        return distance;
    }
}
