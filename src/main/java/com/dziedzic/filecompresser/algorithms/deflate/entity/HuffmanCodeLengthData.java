package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 17.05.2020
 */

public class HuffmanCodeLengthData {
    public int index;
    public int counter;
    public int huffmanCode = 0;
    public int bitsNumber = 0;

    public HuffmanCodeLengthData(int index, int counter) {
        this.index = index;
        this.counter = counter;
    }
}
