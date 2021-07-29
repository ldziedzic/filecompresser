package com.dziedzic.filecompresser.algorithms.deflate.common;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;

public class BitReader {
    private final int BITS_IN_BYTE = 8;

    public int getBits(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return 0;
        int startPosition = offset / BITS_IN_BYTE;
        int endPosition = offset / BITS_IN_BYTE + (offset % BITS_IN_BYTE + bitsNumber) / BITS_IN_BYTE;
        if ((offset % BITS_IN_BYTE + bitsNumber) % BITS_IN_BYTE > 0)
            endPosition++;
        byte[] neededBytes = Arrays.copyOfRange(content, startPosition, endPosition);
        BitSet bitSet = BitSet.valueOf(neededBytes);

        offset = offset % BITS_IN_BYTE;
        String bitSetString = getBinaryString(bitSet.get(offset, offset + bitsNumber), bitsNumber);
        return Integer.parseInt(bitSetString, 2);
    }


    private String getBinaryString(BitSet bitSet, int bitsNumber) {
        return IntStream
                .range(0, bitsNumber)
                .mapToObj(i -> bitSet.get(i) ? '1' : '0')
                .collect(
                        () -> new StringBuilder(bitsNumber),
                        (buffer, characterToAdd) -> buffer.append(characterToAdd),
                        StringBuilder::append
                )
                .toString();
    }


    private String getBinaryStringFromLitleEndian(BitSet bitSet, int bitsNumber) {
        String binaryString = IntStream
                .range(0, bitsNumber)
                .mapToObj(i -> bitSet.get(i) ? '1' : '0')
                .collect(
                        () -> new StringBuilder(bitsNumber),
                        (buffer, characterToAdd) -> buffer.append(characterToAdd),
                        StringBuilder::append
                )
                .toString();
        for (int i =  0; i < BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE; i++) {
            binaryString += "0";
        }
        int bytesNumer = (bitsNumber + BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) / BITS_IN_BYTE;
        String bigEndianString = "";
        for (int i =  bytesNumer - 1; i >= 0; i--) {
            StringBuilder nextByte = new StringBuilder();
            nextByte.append(binaryString.substring(i * BITS_IN_BYTE, (i + 1) * BITS_IN_BYTE)).reverse();
            bigEndianString += nextByte;
        }

        return bigEndianString;
    }


    public int getBitsLittleEndian(byte[] content, int offset, int bitsNumber) {
        if (bitsNumber == 0)
            return 0;
        BitSet bitSet = BitSet.valueOf(content);
        String bitSetString = getBinaryStringFromLitleEndian(bitSet.get(offset, offset + bitsNumber), bitsNumber);

        return Integer.parseInt(bitSetString, 2);
    }

    public byte[] setBitsLittleEndian(byte[] content, int offset, int bitsNumber, int newBitsInt) {
        int contentLength = content.length;
//        int shiftBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
//        return toByteArray(fromByteArray(newContent) >>> shiftBits);
        byte[] newBits = toByteArray(newBitsInt);
        newBits = Arrays.copyOfRange(newBits, newBits.length - (bitsNumber - 1) / BITS_IN_BYTE - 1, newBits.length);
//        byte[] newBits = rewind(toByteArray(newBitsInt));
        if (bitsNumber == 0 || bitsNumber > 4 * BITS_IN_BYTE)
            return newBits;

        BitSet bitSet = BitSet.valueOf(content);
        byte[] partOfContent =  bitSet.get(offset, offset + bitsNumber).toByteArray();
        if (partOfContent.length == 0)
            partOfContent =  new byte[((bitsNumber - 1) / BITS_IN_BYTE) + 1];


        for (int i = 0; i < partOfContent.length; i++) {
            partOfContent[i] |= newBits[i];
        }
        BitSet updatedPartOfContent = BitSet.valueOf(partOfContent);

        for (int i = updatedPartOfContent.nextSetBit(0); i >= 0; i = updatedPartOfContent.nextSetBit(i+1)) {
            bitSet.set(i + offset);
        }
        byte[] updatedContent = bitSet.toByteArray();
        content = ArrayUtils.addAll(updatedContent, new byte[contentLength - updatedContent.length]);
        return content;
    }


    public byte[] setBits(byte[] content, int offset, int bitsNumber, int newBitsInt) {
        int contentLength = content.length;
        int shiftBits = (BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE) % BITS_IN_BYTE;
//        return toByteArray(fromByteArray(newContent) >>> shiftBits);
        byte[] newBits = rewind(toByteArray(newBitsInt << shiftBits));
        newBits = Arrays.copyOfRange(newBits, newBits.length - (bitsNumber - 1) / BITS_IN_BYTE - 1, newBits.length);
//        byte[] newBits = rewind(toByteArray(newBitsInt));
        if (bitsNumber == 0 || bitsNumber > 4 * BITS_IN_BYTE)
            return newBits;

        BitSet bitSet = BitSet.valueOf(content);
        byte[] partOfContent =  bitSet.get(offset, offset + bitsNumber).toByteArray();
        if (partOfContent.length == 0)
            partOfContent =  new byte[((bitsNumber - 1) / BITS_IN_BYTE) + 1];


        for (int i = 0; i < partOfContent.length; i++) {
            partOfContent[i] |= newBits[i];
        }
        BitSet updatedPartOfContent = BitSet.valueOf(partOfContent);

        for (int i = updatedPartOfContent.nextSetBit(0); i >= 0; i = updatedPartOfContent.nextSetBit(i+1)) {
            bitSet.set(i + offset);
        }
        byte[] updatedContent = bitSet.toByteArray();
        content = ArrayUtils.addAll(updatedContent, new byte[contentLength - updatedContent.length]);
        return content;
    }


//    public void setBits(byte[] content, int offset, int bitsNumber, int newBitsInt) {
//        byte [] newBits = toByteArray(newBitsInt);
//        if (bitsNumber == 0 || bitsNumber > 4 * BITS_IN_BYTE)
//            return;
//        int startByte = offset / BITS_IN_BYTE;
//        int endByte = (offset + bitsNumber) / BITS_IN_BYTE + 1;
//
//        int newBitsLength = (bitsNumber / BITS_IN_BYTE) + (bitsNumber % BITS_IN_BYTE > 0 ? 1: 0);
////        if (bitsNumber % BITS_IN_BYTE != 0) {
////            int shiftBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
////
////            newBits = Arrays.copyOfRange(toByteArray(fromByteArray(newBits) << shiftBits), Integer.BYTES - newBitsLength, Integer.BYTES);
////        } else
////            newBits = Arrays.copyOfRange(newBits, Integer.BYTES - newBitsLength, Integer.BYTES);
//
//        newBits = Arrays.copyOfRange(newBits, Integer.BYTES - newBitsLength, Integer.BYTES);
//
//        byte[] exetndedNewBits = toByteArray(fromByteArray(ArrayUtils.addAll(newBits, new byte[]{0})) >> offset % BITS_IN_BYTE);
//        exetndedNewBits = Arrays.copyOfRange(exetndedNewBits, exetndedNewBits.length - newBits.length - 1, exetndedNewBits.length);
//
//        try {
//            for (int i = startByte; i < endByte; i++) {
//                content[(offset / 8) + i] = (byte) (content[(offset / 8) + i] | exetndedNewBits[i - startByte]);
//
//            }
//        } catch (Exception ex) {
//            System.out.println(Arrays.toString(newBits));
//        }
//
////        BitSet contentBitSet = BitSet.valueOf(rewind(Arrays.copyOfRange(content, startByte, endByte)));
////        BitSet newContentBitSet = BitSet.valueOf(newBits);
////
////        saveBits(content, offset, startByte, endByte, contentBitSet, newContentBitSet);
//    }


//    public void setBitsLittleEndian(byte[] content, int offset, int bitsNumber, int newBitsInt) {
//        byte [] newBits = rewind(toByteArray(newBitsInt));
//        if (bitsNumber == 0 || bitsNumber > 4 * BITS_IN_BYTE)
//            return;
//        int startByte = offset / BITS_IN_BYTE;
//        int endByte = (offset + bitsNumber) / BITS_IN_BYTE + 1;
//
//        int newBitsLength = (bitsNumber / BITS_IN_BYTE) + (bitsNumber % BITS_IN_BYTE > 0 ? 1: 0);
//        if (bitsNumber % BITS_IN_BYTE != 0) {
//            int shiftBits = BITS_IN_BYTE - bitsNumber % BITS_IN_BYTE;
//
//            newBits = Arrays.copyOfRange(toByteArray(fromByteArray(newBits) >>> shiftBits), Integer.BYTES - newBitsLength, Integer.BYTES);
//        } else
//            newBits = Arrays.copyOfRange(newBits, Integer.BYTES - newBitsLength, Integer.BYTES);
////        newBits = Arrays.copyOfRange(newBits, Integer.BYTES - newBitsLength, Integer.BYTES);
//
//        byte[] exetndedNewBits = toByteArray(fromByteArray(ArrayUtils.addAll(newBits)) << offset % BITS_IN_BYTE );
//        exetndedNewBits = Arrays.copyOfRange(exetndedNewBits, exetndedNewBits.length - newBits.length - (offset % BITS_IN_BYTE + bitsNumber - 1) / BITS_IN_BYTE, exetndedNewBits.length);
//
//        try {
//            for (int i = startByte; i < endByte; i++) {
//                content[(offset / 8) + i] = (byte) (content[(offset / 8) + i] | exetndedNewBits[i - startByte]);
//            }
//        } catch (Exception ex) {
//            System.out.println(Arrays.toString(newBits));
//        }
//
////        BitSet contentBitSet = BitSet.valueOf(rewind(Arrays.copyOfRange(content, startByte, endByte)));
////        BitSet newContentBitSet = BitSet.valueOf(newBits);
////
////        saveBits(content, offset, startByte, endByte, contentBitSet, newContentBitSet);
//    }


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
