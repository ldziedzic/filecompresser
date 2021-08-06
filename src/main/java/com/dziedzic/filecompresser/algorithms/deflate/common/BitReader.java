package com.dziedzic.filecompresser.algorithms.deflate.common;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class BitReader {
    private final int BITS_IN_BYTE = 8;

    public int getBits(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return 0;
        byte[] neededBytes = getNeededBytes(content, offset, bitsNumber);
        offset = offset % BITS_IN_BYTE;

        BitSet bitSet = BitSet.valueOf(neededBytes);
        String bitSetString = getBinaryString(bitSet.get(offset, offset + bitsNumber), bitsNumber);
        return Integer.parseInt(bitSetString, 2);
    }


    public int getBitsLittleEndian(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return 0;
        byte[] neededBytes = getNeededBytes(content, offset, bitsNumber);
        offset = offset % BITS_IN_BYTE;

        BitSet bitSet = BitSet.valueOf(neededBytes);
        String bitSetString = getBinaryStringFromLitleEndian(bitSet.get(offset, offset + bitsNumber), bitsNumber);

        return Integer.parseInt(bitSetString, 2);
    }

    public byte[] setBits(byte[] content, int offset, int bitsNumber, int newBitsInt) {
        String binaryString = Integer.toBinaryString(newBitsInt);

        int bytesNumer = (offset + bitsNumber + BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) / BITS_IN_BYTE;
        if ((offset + bitsNumber + BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) % BITS_IN_BYTE != 0) {
            bytesNumer++;
        }
        int additionalBits = offset % BITS_IN_BYTE + bitsNumber - binaryString.length();
        int shiftBits = bytesNumer * BITS_IN_BYTE - binaryString.length() - additionalBits;

        char[] additionalBitsChars = new char[additionalBits];
        Arrays.fill(additionalBitsChars, '0');
        char[] shiftBitsChars = new char[shiftBits];
        Arrays.fill(shiftBitsChars, '0');
        binaryString = new String(additionalBitsChars) + binaryString + new String(shiftBitsChars);

        StringBuilder bigEndianString = new StringBuilder();
        for (int i =  0; i < bytesNumer; i++) {
            StringBuilder nextByte = new StringBuilder();
            nextByte.append(binaryString, i * BITS_IN_BYTE, (i + 1) * BITS_IN_BYTE).reverse();
            bigEndianString.append(nextByte);
        }

        binaryString =  bigEndianString.toString();

        BitSet bitset = new BitSet(binaryString.length());
        int len = binaryString.length();
        for (int i = len-1; i >= 0; i--) {
            if (binaryString.charAt(i) == '1') {
                bitset.set(len-i-1);
            }
        }

        byte[] bytes = toByteArray(bitset);
        for (int i = 0; i < bytes.length; i++) {
            content[offset / BITS_IN_BYTE + additionalBits / BITS_IN_BYTE + i] = (byte) (content[offset / BITS_IN_BYTE + additionalBits / BITS_IN_BYTE + i] | bytes[i]);
        }
        return content;
    }


    byte[] setBitsLittleEndian(byte[] content, int offset, int bitsNumber, int newBitsInt) {
        String binaryString = Integer.toBinaryString(newBitsInt << (offset % BITS_IN_BYTE));

        int bytesNumer = (bitsNumber + BITS_IN_BYTE - (offset +bitsNumber) % BITS_IN_BYTE) / BITS_IN_BYTE;
        if ((offset + bitsNumber + BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) % BITS_IN_BYTE != 0) {
            bytesNumer++;
        }

        int shiftBits = bytesNumer * BITS_IN_BYTE - binaryString.length();

        char[] shiftBitsChars = new char[shiftBits];
        Arrays.fill(shiftBitsChars, '0');
        binaryString = new String(shiftBitsChars) +  binaryString;

        StringBuilder littleEndianString = new StringBuilder();

        for (int i =  0; i < binaryString.length() / BITS_IN_BYTE; i++) {
            StringBuilder nextByte = new StringBuilder();
            nextByte.append(binaryString, i * BITS_IN_BYTE, (i + 1) * BITS_IN_BYTE).reverse();
            littleEndianString.append(nextByte);
        }

        binaryString =  littleEndianString.reverse().toString();

        BitSet bitset = new BitSet(binaryString.length());
        int len = binaryString.length();
        for (int i = len-1; i >= 0; i--) {
            if (binaryString.charAt(i) == '1') {
                bitset.set(len-i-1);
            }
        }

        byte[] bytes = toByteArray(bitset);
        int additionalOffset = 0;
        for (int i =  0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '0') {
                additionalOffset += 1;
            } else {
                break;
            }
        }
        additionalOffset = additionalOffset / BITS_IN_BYTE;
        for (int i = 0; i < bytes.length; i++) {
            content[offset / BITS_IN_BYTE + additionalOffset+ i] = (byte) (content[offset / BITS_IN_BYTE + additionalOffset + i] | bytes[i]);
        }
        return content;
    }



    private static byte[] toByteArray(BitSet bits) {
        byte[] bytes = new byte[(bits.length() + 7) / 8];

        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8);
            }
        }
        return bytes;
    }


    private byte[] getNeededBytes(byte[] content, int offset, int bitsNumber) {
        int startPosition = offset / BITS_IN_BYTE;
        int endPosition = offset / BITS_IN_BYTE + (offset % BITS_IN_BYTE + bitsNumber) / BITS_IN_BYTE;
        if ((offset % BITS_IN_BYTE + bitsNumber) % BITS_IN_BYTE > 0)
            endPosition++;
        return Arrays.copyOfRange(content, startPosition, endPosition);
    }


    private String getBinaryString(BitSet bitSet, int bitsNumber) {
        char[] bitsChars = new char[bitsNumber];
        Arrays.fill(bitsChars, '0');

        for (int i = 0; i < bitsNumber; i++) {
            if (bitSet.get(i)){
                bitsChars[i] = '1';
            }
        }
        return new String(bitsChars);
    }


    private String getBinaryStringFromLitleEndian(BitSet bitSet, int bitsNumber) {
        int additionalBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
        char[] bitsChars = new char[bitsNumber + additionalBits];
        Arrays.fill(bitsChars, '0');

        for (int i = 0; i < bitsNumber + additionalBits; i++) {
            if (bitSet.get(i)){
                bitsChars[i] = '1';
            }
        }
        String binaryString = new String(bitsChars);
        int bytesNumer = (bitsNumber + BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) / BITS_IN_BYTE;
        StringBuilder bigEndianString = new StringBuilder();
        for (int i =  bytesNumer - 1; i >= 0; i--) {
            StringBuilder nextByte = new StringBuilder();
            nextByte.append(binaryString, i * BITS_IN_BYTE, (i + 1) * BITS_IN_BYTE).reverse();
            bigEndianString.append(nextByte);
        }

        return bigEndianString.toString();
    }
}
