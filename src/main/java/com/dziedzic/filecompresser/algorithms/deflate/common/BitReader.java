package com.dziedzic.filecompresser.algorithms.deflate.common;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitReader {
    private final int BITS_IN_BYTE = 8;

    public byte[] getBits(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return new byte[]{0};
        BitSet bitSet = BitSet.valueOf(content);
        byte[] newContent =  rewind(bitSet.get(offset, offset + bitsNumber).toByteArray());
        if (newContent.length == 0)
            return new byte[]{0};
        if (bitsNumber % BITS_IN_BYTE != 0) {
            int shiftBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
            return new BigInteger(newContent).shiftRight(shiftBits).toByteArray();
        } else
            return newContent;

    }

    public byte[] getBitsLittleEndian(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return new byte[]{0};
        BitSet bitSet = BitSet.valueOf(content);
        byte[] newContent =  bitSet.get(offset, offset + bitsNumber).toByteArray();
        if (newContent.length == 0)
            return new byte[]{0};
        return newContent;
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


    public byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    public int fromByteArray(byte[] bytes, int bitsNumber) {
        byte[] byteArray = new byte[Integer.BYTES];

        int i = Integer.BYTES - bytes.length;
        for (byte item: bytes) {
            byteArray[i] = item;
            i++;
        }
        return ByteBuffer.wrap(byteArray).getInt();
    }
}
