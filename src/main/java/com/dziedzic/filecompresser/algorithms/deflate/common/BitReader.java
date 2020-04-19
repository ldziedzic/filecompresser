package com.dziedzic.filecompresser.algorithms.deflate.common;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import java.util.BitSet;

public class BitReader {
    private final int BITS_IN_BYTE = 8;

    public byte[] getBits(byte[] content, int offset, int bitsNumber) {
        BitSet bitSet = BitSet.valueOf(content);
        return rewind(bitSet.get(offset, offset + bitsNumber).toByteArray());
    }

    public byte[] rewind(byte[] content) {
        int i = 0;
        for (byte byteContent : content) {
            content[i] = (byte) (Integer.reverse(byteContent) >> (Integer.SIZE - Byte.SIZE));
            i++;
        }
        return content;
    }

    public int getByteIndex(int bitOffset) {
        return bitOffset / BITS_IN_BYTE;
    }
}
