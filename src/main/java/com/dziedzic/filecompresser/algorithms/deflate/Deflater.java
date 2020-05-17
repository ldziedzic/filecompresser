package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.BlockHeader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CompressionType;
import com.dziedzic.filecompresser.algorithms.deflate.entity.DistanceCode;
import com.dziedzic.filecompresser.algorithms.deflate.entity.DistanceCodeOutput;
import com.dziedzic.filecompresser.algorithms.deflate.entity.FilePosition;
import com.dziedzic.filecompresser.algorithms.deflate.entity.HuffmanLengthCode;
import com.dziedzic.filecompresser.algorithms.deflate.entity.LengthCode;

public class Deflater {

    private static final int END_OF_BLOCK = 256;

    public byte[] deflate(byte[] content, Long outputSize) {
        byte[] output = new byte[Math.toIntExact(outputSize)];

        readCompressedContent(content, output, outputSize);

        return output;
    }

    private void readCompressedContent(byte[] content, byte[] output,  Long outputSize) {

        BitReader bitReader = new BitReader();

        FilePosition filePosition = new FilePosition(0, 0);

        while (isNextBlockExists(outputSize, filePosition)) {
            BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, filePosition.getOffset(), 3)[0]);
            filePosition.setOffset(filePosition.getOffset() + 3);

            CodeTreesRepresener codeTreesRepresener = new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
            codeTreesRepresener.generateCodeTreesRepresentation();

            int smallestHuffmanCodeLength = codeTreesRepresener.getSmallestHuffmanLength();
            readBlock(content, bitReader, codeTreesRepresener, smallestHuffmanCodeLength, output, filePosition);
        }
    }

    private boolean isNextBlockExists(Long outputSize, FilePosition filePosition) {
        return filePosition.getPosition() < outputSize;
    }

    private void readBlock(byte[] content, BitReader bitReader, CodeTreesRepresener codeTreesRepresener,
                           int smallestHuffmanCodeLength, byte[] output, FilePosition filePosition) {
        boolean endOfBlock = false;
        int bitsNumber = smallestHuffmanCodeLength;

        while (!endOfBlock) {
            byte[] code = bitReader.getBits(content, filePosition.getOffset(), bitsNumber);
            int codeInt = bitReader.fromByteArray(code, bitsNumber);

            for (HuffmanLengthCode huffmanLengthCode: codeTreesRepresener.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getPrefixCode() == codeInt && huffmanLengthCode.getBitsNumber() == bitsNumber) {
                    filePosition.setOffset(filePosition.getOffset() + bitsNumber);

                    if (huffmanLengthCode.getLengthCode() < END_OF_BLOCK) {
                        copyByteToOutputStream(output, filePosition, huffmanLengthCode);
                    }
                    else if (huffmanLengthCode.getLengthCode() == END_OF_BLOCK)
                        endOfBlock = true;
                    else {
                        CopyMultipleBytesToOutputStream(content, bitReader, codeTreesRepresener, output,
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

    private void CopyMultipleBytesToOutputStream(byte[] content, BitReader bitReader, CodeTreesRepresener codeTreesRepresener, byte[] output, FilePosition filePosition, HuffmanLengthCode huffmanLengthCode) {
        LengthCode lengthCode =
                codeTreesRepresener.findLengthCode(huffmanLengthCode.getLengthCode());
        DistanceCodeOutput distanceCodeOutput =
                getDistance(content, bitReader, codeTreesRepresener, filePosition.getOffset());
        filePosition.setOffset(distanceCodeOutput.getOffset());

        int copyPosition = filePosition.getPosition() - distanceCodeOutput.getDistance();
        for (int i = 0; i < lengthCode.getLength(); i++) {
            output[filePosition.getPosition()] = output[copyPosition];
            copyPosition++;
            filePosition.setPosition(filePosition.getPosition() + 1);
        }
    }

    private DistanceCodeOutput getDistance(byte[] content, BitReader bitReader,
                                           CodeTreesRepresener codeTreesRepresener, Integer offset) {

        int distance = 0;
        for (int bitsNumber = codeTreesRepresener.MIN_DISTANCE_CODE_LENGTH;
             bitsNumber <= codeTreesRepresener.getBiggestDistanceCodeLength(); bitsNumber++) {

            byte[] distanceCodeBits = bitReader.getBits(content, offset, bitsNumber);
            int distanceCodeInt = bitReader.fromByteArray(distanceCodeBits, bitsNumber);

            for (DistanceCode distanceCode: codeTreesRepresener.getDistanceCodes()) {
                if (distanceCode.getCode() == distanceCodeInt &&
                        codeTreesRepresener.MIN_DISTANCE_CODE_LENGTH + distanceCode.getExtraBits() == bitsNumber) {
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
