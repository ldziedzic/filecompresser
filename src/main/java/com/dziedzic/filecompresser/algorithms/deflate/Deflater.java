package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.BlockHeader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CodeTreesRepresentation;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CompressionType;
import com.dziedzic.filecompresser.algorithms.deflate.entity.DistanceCode;
import com.dziedzic.filecompresser.algorithms.deflate.entity.HuffmanLengthCode;

public class Deflater {

    public byte[] deflate(byte[] content, Long outputSize) {
        BitReader bitReader = new BitReader();

        BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, 0, 3)[0]);

        CodeTreesRepresentation codeTreesRepresentation = new CodeTreesRepresentation(content, blockHeader);
        codeTreesRepresentation.generateCodeTreesRepresentation();

        int smallestHuffmanCodeLength = codeTreesRepresentation.getSmallestHuffmanLength();
        byte[] output = new byte[Math.toIntExact(outputSize)];

        readCompressedContent(content, bitReader, codeTreesRepresentation, smallestHuffmanCodeLength, output);

        return output;
    }

    private void readCompressedContent(byte[] content, BitReader bitReader, CodeTreesRepresentation codeTreesRepresentation, int smallestHuffmanCodeLength, byte[] output) {
        boolean endOfBlock = false;
        int offset = 3;
        int bitsNumber = smallestHuffmanCodeLength;
        int position = 0;

        while (!endOfBlock) {
            byte[] code = bitReader.getBits(content, offset, bitsNumber);
            int codeInt = bitReader.fromByteArray(code);

            for (HuffmanLengthCode huffmanLengthCode: codeTreesRepresentation.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getPrefixCode() == codeInt && huffmanLengthCode.getBitsNumber() == bitsNumber) {
                    System.out.println(huffmanLengthCode.getLengthCode());
                    offset += bitsNumber;


                    if (huffmanLengthCode.getLengthCode() < 256)
                        output[position] = (byte) huffmanLengthCode.getLengthCode();
                    else if (huffmanLengthCode.getLengthCode() == 256)
                        endOfBlock = true;
                    else {
                        bitsNumber = 0;
                        while (!endOfBlock) {

                            byte[] distanceCode = bitReader.getBits(content, offset, bitsNumber);
                            int distanceCodeInt = bitReader.fromByteArray(distanceCode);

                            for (DistanceCode distanceCode1: codeTreesRepresentation.getDistanceCodes()) {
                                if (distanceCode1.getCode() == distanceCodeInt && distanceCode1.getExtraBits() == bitsNumber) {
                                    System.out.println(distanceCode1.getDistance());
                                    offset += bitsNumber;
                                    bitsNumber = smallestHuffmanCodeLength - 1;
                                }
                            }
                            bitsNumber++;
                        }
                    }
                    bitsNumber = smallestHuffmanCodeLength - 1;
                }
            }
            bitsNumber++;
        }
    }

    private BlockHeader readBlockHeader(byte content) {
        int isLastBlock = (content & 0x80) >> 7;
        CompressionType compressionType = CompressionType.valueOf((content & 0x60)  >> 5).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
