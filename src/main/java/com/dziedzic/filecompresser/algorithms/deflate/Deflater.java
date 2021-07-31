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
    private final int BITS_IN_BYTE = 8;

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
            BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, filePosition.getOffset(), 3));
            filePosition.increaseOffset(3);

            CodeTreesRepresener codeTreesRepresener = new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
            codeTreesRepresener.readCodeTreesRepresentation();
            filePosition.setOffset(codeTreesRepresener.getOffset());

            if (blockHeader.getCompressionType() == CompressionType.ERROR) {
                throw new RuntimeException("Compression type = ERROR");
            }
            if (blockHeader.getCompressionType() != CompressionType.NO_COMPRESSION)
                readBlock(content, bitReader, codeTreesRepresener, output, filePosition);
            else
                readBlockWithoutCompression(content, bitReader, output, filePosition);
        }
    }

    private boolean isNextBlockExists(Long outputSize, FilePosition filePosition) {
        return filePosition.getPosition() < outputSize;
    }

    private void readBlock(byte[] content, BitReader bitReader, CodeTreesRepresener codeTreesRepresener,
                           byte[] output, FilePosition filePosition) {
        boolean endOfBlock = false;
        int bitsNumber = codeTreesRepresener.getBiggestHuffmanLength();
        boolean canReuseBits = false;
        int codeInt = 0;

        while (!endOfBlock) {
            if (!canReuseBits)
                codeInt = bitReader.getBits(content, filePosition.getOffset(), bitsNumber);
            else
                codeInt = codeInt >> 1;
            canReuseBits = true;

            HuffmanCodeLengthData huffmanLengthCode = codeTreesRepresener.getHuffmanLengthCode(bitsNumber, codeInt);

            if (huffmanLengthCode != null) {
                filePosition.increaseOffset(bitsNumber);
                if (huffmanLengthCode.getIndex() < END_OF_BLOCK) {
                    copyByteToOutputStream(output, filePosition, huffmanLengthCode);
                }
                else if (huffmanLengthCode.getIndex() == END_OF_BLOCK)
                    endOfBlock = true;
                else {
                    copyMultipleBytesToOutputStream(content, bitReader, codeTreesRepresener, output,
                            filePosition, huffmanLengthCode);
                }
                bitsNumber =  codeTreesRepresener.getBiggestHuffmanLength() +  1;
                canReuseBits = false;
            }
            bitsNumber--;
            System.out.print(100 * filePosition.getPosition() / output.length + " %\r");
        }
    }
    private void readBlockWithoutCompression(byte[] content, BitReader bitReader,
                                             byte[] output, FilePosition filePosition) {
        if (filePosition.getOffset() % BITS_IN_BYTE != 0)
            filePosition.increaseOffset(BITS_IN_BYTE - filePosition.getOffset() % BITS_IN_BYTE);
        int blockSize = bitReader.getBitsLittleEndian(content, filePosition.getOffset(), 2 * BITS_IN_BYTE);
        filePosition.increaseOffset(2 * BITS_IN_BYTE);
        int complementSize = bitReader.getBitsLittleEndian(content, filePosition.getOffset(), 2 * BITS_IN_BYTE);
        filePosition.increaseOffset(2 * BITS_IN_BYTE);
        if (filePosition.getOffset() % BITS_IN_BYTE != 0)
            System.out.println("Error - invalid value for offset in block without compression");
        if (blockSize + complementSize != 65535)
            System.out.println("Error - invalid value for block size in block without compression");
        System.arraycopy(content, filePosition.getOffset() / BITS_IN_BYTE, output, filePosition.getPosition(), blockSize);

        for (int i = 0; i < blockSize; i++) {
            output[filePosition.getPosition()] = content[filePosition.getOffset() / BITS_IN_BYTE];
            filePosition.increasePosition(1);
            filePosition.increaseOffset(BITS_IN_BYTE);
        }

        System.out.print(100 * filePosition.getPosition() / output.length + " %\r");
    }

    private void copyByteToOutputStream(byte[] output, FilePosition filePosition, HuffmanCodeLengthData huffmanLengthCode) {
        output[filePosition.getPosition()] = (byte) huffmanLengthCode.getIndex();
        filePosition.increasePosition(1);
    }

    private void copyMultipleBytesToOutputStream(byte[] content, BitReader bitReader, CodeTreesRepresener codeTreesRepresener, byte[] output, FilePosition filePosition, HuffmanCodeLengthData huffmanLengthCode) {
        LengthCode lengthCode =
                codeTreesRepresener.findLengthCode(huffmanLengthCode.getIndex());
        int additionalLength = bitReader.getBitsLittleEndian(content, filePosition.getOffset(), lengthCode.getExtraBits());
        filePosition.increaseOffset(lengthCode.getExtraBits());
        DistanceCodeOutput distanceCodeOutput =
                getDistance(content, bitReader, codeTreesRepresener, filePosition.getOffset());
        filePosition.setOffset(distanceCodeOutput.getOffset());

        int copyPosition = filePosition.getPosition() - distanceCodeOutput.getDistance();


        for (int i = 0; i < lengthCode.getLength() + additionalLength; i++) {
            output[filePosition.getPosition()] = output[copyPosition];
            copyPosition++;
            filePosition.increasePosition(1);
        }
    }

    private DistanceCodeOutput getDistance(byte[] content, BitReader bitReader,
                                           CodeTreesRepresener codeTreesRepresener, Integer offset) {
        int distance = 0;
        int distanceCodeInt = 0;

        for (int bitsNumber = codeTreesRepresener.getBiggestDistanceCodeLength();
             bitsNumber >= codeTreesRepresener.MIN_DISTANCE_CODE_LENGTH; bitsNumber--) {

            if (bitsNumber == codeTreesRepresener.getBiggestDistanceCodeLength())
                distanceCodeInt = bitReader.getBits(content, offset, bitsNumber);
            else
                distanceCodeInt = distanceCodeInt >> 1;

            for (DistanceCode distanceCode: codeTreesRepresener.getDistanceCodes()) {
                if (distanceCode.getCode() == distanceCodeInt && distanceCode.getBitsNumber() == bitsNumber) {
                    offset += bitsNumber;
                    distance = distanceCode.getDistance();
                    int additionalDistance = bitReader.getBitsLittleEndian(content, offset, distanceCode.getExtraBits());
                    offset += distanceCode.getExtraBits();
                    return new DistanceCodeOutput(offset, distance + additionalDistance);
                }
            }
        }
        return new DistanceCodeOutput(offset, distance);
    }

    private BlockHeader readBlockHeader(int content) {
        int isLastBlock = (content & 0x04) >> 2;
        CompressionType compressionType = CompressionType.valueOf((content & 0x03)).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
