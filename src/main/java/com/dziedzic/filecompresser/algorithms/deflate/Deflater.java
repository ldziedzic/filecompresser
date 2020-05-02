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
import com.dziedzic.filecompresser.algorithms.deflate.entity.DistanceCodeOutput;
import com.dziedzic.filecompresser.algorithms.deflate.entity.FilePosition;
import com.dziedzic.filecompresser.algorithms.deflate.entity.HuffmanLengthCode;
import com.dziedzic.filecompresser.algorithms.deflate.entity.LengthCode;

public class Deflater {

    private static final int END_OF_BLOCK = 256;

    public byte[] deflate(byte[] content, Long outputSize) {
        BitReader bitReader = new BitReader();

        BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, 0, 3)[0]);

        CodeTreesRepresentation codeTreesRepresentation = new CodeTreesRepresentation(content, blockHeader);
        codeTreesRepresentation.generateCodeTreesRepresentation();

        int smallestHuffmanCodeLength = codeTreesRepresentation.getSmallestHuffmanLength();
        byte[] output = new byte[Math.toIntExact(outputSize)];

        readCompressedContent(content, bitReader, codeTreesRepresentation, smallestHuffmanCodeLength, output, outputSize);

        return output;
    }

    private void readCompressedContent(byte[] content, BitReader bitReader,
                                       CodeTreesRepresentation codeTreesRepresentation, int smallestHuffmanCodeLength,
                                       byte[] output,  Long outputSize) {

        FilePosition filePosition = new FilePosition(3, 0);

        while (isNextBlockExists(outputSize, filePosition))
            readBlock(content, bitReader, codeTreesRepresentation, smallestHuffmanCodeLength, output, filePosition);
    }

    private boolean isNextBlockExists(Long outputSize, FilePosition filePosition) {
        return filePosition.getPosition() < outputSize;
    }

    private void readBlock(byte[] content, BitReader bitReader, CodeTreesRepresentation codeTreesRepresentation,
                           int smallestHuffmanCodeLength, byte[] output, FilePosition filePosition) {
        boolean endOfBlock = false;
        int bitsNumber = smallestHuffmanCodeLength;

        while (!endOfBlock) {
            byte[] code = bitReader.getBits(content, filePosition.getOffset(), bitsNumber);
            int codeInt = bitReader.fromByteArray(code);

            for (HuffmanLengthCode huffmanLengthCode: codeTreesRepresentation.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getPrefixCode() == codeInt && huffmanLengthCode.getBitsNumber() == bitsNumber) {
                    filePosition.setOffset(filePosition.getOffset() + bitsNumber);

                    if (huffmanLengthCode.getLengthCode() < END_OF_BLOCK) {
                        copyByteToOutputStream(output, filePosition, huffmanLengthCode);
                    }
                    else if (huffmanLengthCode.getLengthCode() == END_OF_BLOCK)
                        endOfBlock = true;
                    else {
                        CopyMultipleBytesToOutputStream(content, bitReader, codeTreesRepresentation, output,
                                filePosition, huffmanLengthCode);
                    }
                    bitsNumber = smallestHuffmanCodeLength - 1;
                }
            }
            bitsNumber++;
        }
    }

    private void copyByteToOutputStream(byte[] output, FilePosition filePosition, HuffmanLengthCode huffmanLengthCode) {
        output[filePosition.getPosition()] = (byte) huffmanLengthCode.getLengthCode();
        filePosition.setPosition(filePosition.getPosition() + 1);
    }

    private void CopyMultipleBytesToOutputStream(byte[] content, BitReader bitReader, CodeTreesRepresentation codeTreesRepresentation, byte[] output, FilePosition filePosition, HuffmanLengthCode huffmanLengthCode) {
        LengthCode lengthCode =
                codeTreesRepresentation.findLengthCode(huffmanLengthCode.getLengthCode());
        DistanceCodeOutput distanceCodeOutput =
                getDistance(content, bitReader, codeTreesRepresentation, filePosition.getOffset());
        filePosition.setOffset(distanceCodeOutput.getOffset());

        int copyPosition = filePosition.getPosition() - distanceCodeOutput.getDistance();
        for (int i = 0; i < lengthCode.getLength(); i++) {
            output[filePosition.getPosition()] = output[copyPosition];
            copyPosition++;
            filePosition.setPosition(filePosition.getPosition() + 1);
        }
    }

    private DistanceCodeOutput getDistance(byte[] content, BitReader bitReader,
                                           CodeTreesRepresentation codeTreesRepresentation, Integer offset) {

        int distance = 0;
        for (int bitsNumber = codeTreesRepresentation.MIN_DISTANCE_CODE_LENGTH;
             bitsNumber <= codeTreesRepresentation.getBiggestDistanceCodeLength(); bitsNumber++) {

            byte[] distanceCodeBits = bitReader.getBits(content, offset, bitsNumber);
            int distanceCodeInt = bitReader.fromByteArray(distanceCodeBits);

            for (DistanceCode distanceCode: codeTreesRepresentation.getDistanceCodes()) {
                if (distanceCode.getCode() == distanceCodeInt &&
                        codeTreesRepresentation.MIN_DISTANCE_CODE_LENGTH + distanceCode.getExtraBits() == bitsNumber) {
                    System.out.println(distanceCode.getDistance());
                    offset += bitsNumber;
                    distance = distanceCode.getDistance();
                }
            }
            bitsNumber++;
        }
        return new DistanceCodeOutput(offset, distance);
    }

    private BlockHeader readBlockHeader(byte content) {
        int isLastBlock = (content & 0x04) >> 2;
        CompressionType compressionType = CompressionType.valueOf((content & 0x03)).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
