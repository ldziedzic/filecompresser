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
import com.dziedzic.filecompresser.algorithms.deflate.entity.HuffmanCodeLengthData;
import com.dziedzic.filecompresser.algorithms.deflate.entity.LengthCode;

import java.util.ArrayList;
import java.util.List;

public class Deflater {

    private static final int END_OF_BLOCK = 256;
    private static final int MAX_BLOCK_SIZE = 32768;

    public byte[] compress(byte[] content) {
        byte[] output = new byte[content.length + 1];

        output = compressContent(content, output);

        return output;
    }


    public byte[] decompress(byte[] content, Long outputSize) {
        byte[] output = new byte[Math.toIntExact(outputSize)];

        readCompressedContent(content, output, outputSize);

        return output;
    }

    private byte[] compressContent(byte[] content, byte[] output) {
        if (content.length < 1000)
            return compressWithStaticsHuffmanCodes(content, output);
        else
            compressWithDynamicsHuffmanCodes(content, output);
        return output;
    }

    private byte[] compressWithStaticsHuffmanCodes(byte[] content, byte[] output) {
        BitReader bitReader = new BitReader();
        FilePosition filePosition = new FilePosition(0, 0);


        boolean isLastBlock = false;
        if (content.length < MAX_BLOCK_SIZE)
            isLastBlock = true;
        BlockHeader blockHeader = new BlockHeader(isLastBlock, CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES);

        CodeTreesRepresener codeTreesRepresener =
                new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
        codeTreesRepresener.generateCodeTreesRepresentation();

        List<HuffmanCodeLengthData> compressedContent = new ArrayList<>();
        for (int i = 0; i < content.length; i++) {
            for (HuffmanCodeLengthData huffmanLengthCode: codeTreesRepresener.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getIndex() == (int)content[i]) {
                    compressedContent.add(huffmanLengthCode);
                }
            }
        }
        for (HuffmanCodeLengthData huffmanLengthCode: codeTreesRepresener.getHuffmanLengthCodes()) {
            if (huffmanLengthCode.getIndex() == END_OF_BLOCK) {
                compressedContent.add(huffmanLengthCode);
            }
        }

        output = writeBlockHeader(output, filePosition, isLastBlock, blockHeader);
        for (HuffmanCodeLengthData huffmanLengthCode: compressedContent) {
            output = bitReader.setBits(output, filePosition.getPosition(), huffmanLengthCode.bitsNumber,
                    huffmanLengthCode.huffmanCode);
            filePosition.increasePosition(huffmanLengthCode.bitsNumber);
        }

        return output;
    }

    private byte[] writeBlockHeader(byte[] output, FilePosition filePosition, boolean isLastBlock, BlockHeader blockHeader) {
        BitReader bitReader = new BitReader();

        output = bitReader.setBits(output, filePosition.getPosition(), 1, isLastBlock ? 1 : 0);
        filePosition.increasePosition(1);
        output = bitReader.setBits(output, filePosition.getPosition(), 2, blockHeader.getCompressionType().getCompressionTypeCode());
        filePosition.increasePosition(2);

        return output;
    }

    private void compressWithDynamicsHuffmanCodes(byte[] content, byte[] output) {

    }


    private void readCompressedContent(byte[] content, byte[] output,  Long outputSize) {

        BitReader bitReader = new BitReader();

        FilePosition filePosition = new FilePosition(0, 0);

        while (isNextBlockExists(outputSize, filePosition)) {
            BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, filePosition.getOffset(), 3)[3]);
            filePosition.increaseOffset(3);

            CodeTreesRepresener codeTreesRepresener = new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
            codeTreesRepresener.readCodeTreesRepresentation();
            filePosition.setOffset(codeTreesRepresener.getOffset());

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
            int codeInt = bitReader.fromByteArray(code);

            for (HuffmanCodeLengthData huffmanLengthCode: codeTreesRepresener.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getBitsNumber() == 0)
                    continue;
                if (huffmanLengthCode.getHuffmanCode() == codeInt) {
//                    System.out.println(huffmanLengthCode.getIndex());
                    int temp = codeInt;
                }
                if (huffmanLengthCode.getHuffmanCode() == codeInt && huffmanLengthCode.getBitsNumber() == bitsNumber) {
                    filePosition.increaseOffset(bitsNumber);


                    if (huffmanLengthCode.getIndex() < END_OF_BLOCK) {
//                        System.out.print((char)huffmanLengthCode.getIndex());
                        copyByteToOutputStream(output, filePosition, huffmanLengthCode);
                    }
                    else if (huffmanLengthCode.getIndex() == END_OF_BLOCK)
                        endOfBlock = true;
                    else {
//                        System.out.println(huffmanLengthCode.getIndex());
                        CopyMultipleBytesToOutputStream(content, bitReader, codeTreesRepresener, output,
                                filePosition, huffmanLengthCode);
                    }
                    bitsNumber = smallestHuffmanCodeLength - 1;
                    break;
                }
            }
            bitsNumber++;
        }
    }

    private void copyByteToOutputStream(byte[] output, FilePosition filePosition, HuffmanCodeLengthData huffmanLengthCode) {
        output[filePosition.getPosition()] = (byte) huffmanLengthCode.getIndex();
        filePosition.increasePosition(1);
    }

    private void CopyMultipleBytesToOutputStream(byte[] content, BitReader bitReader, CodeTreesRepresener codeTreesRepresener, byte[] output, FilePosition filePosition, HuffmanCodeLengthData huffmanLengthCode) {
        LengthCode lengthCode =
                codeTreesRepresener.findLengthCode(huffmanLengthCode.getIndex());
        byte[] additionalBits = bitReader.getBitsLittleEndian(content, filePosition.getOffset(), lengthCode.getExtraBits());
        int additionalLength = bitReader.fromByteArray(additionalBits);
        filePosition.increaseOffset(lengthCode.getExtraBits());
        DistanceCodeOutput distanceCodeOutput =
                getDistance(content, bitReader, codeTreesRepresener, filePosition.getOffset());
        filePosition.setOffset(distanceCodeOutput.getOffset());

        int copyPosition = filePosition.getPosition() - distanceCodeOutput.getDistance();
        for (int i = 0; i < lengthCode.getLength() + additionalLength; i++) {
            output[filePosition.getPosition()] = output[copyPosition];
//            System.out.print((char)output[copyPosition]);
            copyPosition++;
            filePosition.increasePosition(1);
        }
    }

    private DistanceCodeOutput getDistance(byte[] content, BitReader bitReader,
                                           CodeTreesRepresener codeTreesRepresener, Integer offset) {

        int distance = 0;
        for (int bitsNumber = codeTreesRepresener.MIN_DISTANCE_CODE_LENGTH;
             bitsNumber <= codeTreesRepresener.getBiggestDistanceCodeLength(); bitsNumber++) {

            byte[] distanceCodeBits = bitReader.getBits(content, offset, bitsNumber);
            int distanceCodeInt = bitReader.fromByteArray(distanceCodeBits);

            for (DistanceCode distanceCode: codeTreesRepresener.getDistanceCodes()) {
                if (distanceCode.getCode() == distanceCodeInt && distanceCode.getBitsNumber() == bitsNumber) {
                    offset += bitsNumber;
                    distance = distanceCode.getDistance();
                    byte[] additionalBits = bitReader.getBitsLittleEndian(content, offset, distanceCode.getExtraBits());
                    int additionalDistance = bitReader.fromByteArray(additionalBits);
                    offset += distanceCode.getExtraBits();
                    return new DistanceCodeOutput(offset, distance + additionalDistance);
                }
            }
        }
        return new DistanceCodeOutput(offset, distance);
    }

    private BlockHeader readBlockHeader(byte content) {
        int isLastBlock = (content & 0x04) >> 2;
        CompressionType compressionType = CompressionType.valueOf((content & 0x03)).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
