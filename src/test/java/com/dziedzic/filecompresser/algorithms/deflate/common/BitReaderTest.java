package com.dziedzic.filecompresser.algorithms.deflate.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;

import static org.junit.Assert.assertEquals;

/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 31.05.2020
 */class BitReaderTest {

    @Test
    void testSetBitsBigEndian_WithoutOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        int offset = 0;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBits(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)3);
    }

    @Test
    void testSetBitsBigEndian_WithOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        int offset = 4;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBits(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)48);
    }

    @Test
    void testSetBitsBigEndian_WithContentAndWithOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 3;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBits(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)27);
    }

    @Test
    void testSetBitsBigEndian_WithContentAndWithMultibytesOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 6;
        int bitsNumber = 5;
        int newContent = 29;

        output = bitReader.setBits(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)-61);
        assertEquals(output[1], (byte)5);
    }

    @Test
    void testSetBitsBigEndian_WithContentAndBigNewContent() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 6;
        int bitsNumber = 10;
        int newContent = 995;

        output = bitReader.setBits(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)-61);
        assertEquals(output[1], (byte)-57);
    }






    @Test
    void testSetBitsLittleEndian_WithoutOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        int offset = 0;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)12);
    }

    @Test
    void testSetBitsLittleEndian_WithOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        int offset = 4;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)-64);
    }

    @Test
    void testSetBitsLittleEndian_WithContentAndWithOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 3;
        int bitsNumber = 4;
        int newContent = 12;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)99);
    }

    @Test
    void testSetBitsLittleEndian_WithContentAndWithMultibytesOffset() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 6;
        int bitsNumber = 5;
        int newContent = 29;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)67);
        assertEquals(output[1], (byte)7);
    }

    @Test
    void testSetBitsLittleEndian_BigNewContent() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 0;
        int offset = 0;
        int bitsNumber = 12;
        int newContent = 520;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)2);
        assertEquals(output[1], (byte)8);
    }

    @Test
    void testSetBitsLittleEndian_WithContentAndBigNewContent() {
        BitReader bitReader = new BitReader();

        byte[] output = new byte[4];
        output[0] = 3;
        int offset = 6;
        int bitsNumber = 10;
        int newContent = 995;

        output = bitReader.setBitsLittleEndian(output, offset, bitsNumber, newContent);

        assertEquals(output[0], (byte)-61);
        assertEquals(output[1], (byte)-64);
        assertEquals(output[2], (byte)56);
    }
}
