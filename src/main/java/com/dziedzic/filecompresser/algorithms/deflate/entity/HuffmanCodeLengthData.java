package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 17.05.2020
 */

public class HuffmanCodeLengthData {
    public int index = -1;
    public int lengthCode;
    public int huffmanCode = 0;
    public int bitsNumber = 0;

    public HuffmanCodeLengthData(int index, int lengthCode) {
        this.index = index;
        this.lengthCode = lengthCode;
    }

    public HuffmanCodeLengthData(int index, int lengthCode, int bitsNumber, int huffmanCode) {
        this.lengthCode = lengthCode;
        this.bitsNumber = bitsNumber;
        this.huffmanCode = huffmanCode;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLengthCode() {
        return lengthCode;
    }

    public void setLengthCode(int lengthCode) {
        this.lengthCode = lengthCode;
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
}
