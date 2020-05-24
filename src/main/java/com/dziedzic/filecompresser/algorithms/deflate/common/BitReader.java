package com.dziedzic.filecompresser.algorithms.deflate.common;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import java.nio.ByteBuffer;
import java.util.Arrays;
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
            return toByteArray(fromByteArray(newContent) >>> shiftBits);
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

    public void setBits(byte[] content, int offset, int bitsNumber, byte[] newBits) {
        if (bitsNumber == 0 || bitsNumber > 4 * BITS_IN_BYTE)
            return;
        int startByte = offset / BITS_IN_BYTE;
        int endByte = (offset + bitsNumber) / BITS_IN_BYTE + 1;

        if (bitsNumber % BITS_IN_BYTE != 0) {
            int shiftBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
            int newBitsLength = newBits.length;
            newBits = Arrays.copyOfRange(toByteArray(fromByteArray(newBits) << shiftBits), Integer.BYTES - newBitsLength, Integer.BYTES);
        }

        BitSet contentBitSet = BitSet.valueOf(Arrays.copyOfRange(content, startByte, endByte));
        BitSet newContentBitSet = BitSet.valueOf((newBits));


        for (int i = newContentBitSet.nextSetBit(0); i >= 0; i = newContentBitSet.nextSetBit(i+1)) {
            int additionalBits = 0;
            while ( startByte * BITS_IN_BYTE + i - offset + additionalBits < 0)
                additionalBits +=  2 * BITS_IN_BYTE;
            contentBitSet.set(startByte * BITS_IN_BYTE + i - offset + additionalBits);
        }

        byte[] newContent =  contentBitSet.toByteArray();

        int i = endByte - newContent.length;
        try {
            for (byte item: newContent) {
                content[i] = item;
                i++;
            }
        } catch (Exception ex) {
            System.out.println(Arrays.toString(newContent));
        }
    }

    public void setBitsLittleEndian(byte[] content, int offset, int bitsNumber, byte[] newBits) {
        if (bitsNumber == 0)
            return;
        int startByte = offset / BITS_IN_BYTE;
        int endByte = (offset + bitsNumber) / BITS_IN_BYTE + 1;

        BitSet contentBitSet = BitSet.valueOf(Arrays.copyOfRange(content, startByte, endByte));
        BitSet newContentBitSet = BitSet.valueOf(rewind(newBits));

        for (int i = newContentBitSet.nextSetBit(0); i >= 0; i = newContentBitSet.nextSetBit(i+1)) {
            int additionalBits = 0;
            while ( startByte * BITS_IN_BYTE + i - offset + additionalBits < 0)
                additionalBits +=  2 * BITS_IN_BYTE;
            contentBitSet.set(startByte * BITS_IN_BYTE + i - offset + additionalBits);
        }

        byte[] newContent =  contentBitSet.toByteArray();

        int i = endByte - newContent.length;
        try {
            for (byte item: newContent) {
                content[i] = item;
                i++;
            }
        } catch (Exception ex) {
            System.out.println(Arrays.toString(newContent));
        }
    }


    private byte[] rewind(byte[] content) {
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

    public int fromByteArray(byte[] bytes) {
        if (bytes.length == 0)
            return 0;
        if (bytes.length > 4)
            throw new IndexOutOfBoundsException("Failed to convert bytes array to int");
        byte[] byteArray = new byte[Integer.BYTES];

        int i = Integer.BYTES - bytes.length;
        try {
            for (byte item: bytes) {
                byteArray[i] = item;
                i++;
            }
        } catch (Exception ex) {
            System.out.println(bytes);
        }
        return ByteBuffer.wrap(byteArray).getInt();
    }
}
