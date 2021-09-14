package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 02.05.2020
 */

public class FilePosition {
    private int offset;
    private long position;

    public FilePosition(int offset, long position) {
        this.offset = offset;
        this.position = position;
    }

    public int getOffset() {
        return offset;
    }

    public void increaseOffset(int value) {
        offset += value;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getPosition() {
        return position;
    }

    public void increasePosition(int value) {
        position += value;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
