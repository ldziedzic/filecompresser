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
import com.dziedzic.filecompresser.zip.Entity.CompressionOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Deflater {

    private static final int END_OF_BLOCK = 256;
    private final int BITS_IN_BYTE = 8;

    public CompressionOutput compress(byte[] content, byte additionalByte, int additionalBitsNumber, boolean isLastDataSet) {
        int maxBlockSize = 32768; // 2^15
        FilePosition filePosition = new FilePosition(0, additionalBitsNumber);
        int maxBlockHeaderSize = 286 * 8 + 32 + 19;
        int outputSize = 2 * content.length + maxBlockHeaderSize;
        if (content.length < 65000) {
            byte[] output = new byte[outputSize];
            if (additionalBitsNumber > 0)
                output[0] = additionalByte;
            return new CompressionOutput(compressWithDynamicsHuffmanCodes(content, filePosition, output, isLastDataSet), 0);
        } else {
            int blockNumber = content.length / maxBlockSize;
            if (content.length % maxBlockSize != 0)
                blockNumber++;
            int compressedOutputBytes = 0;
            int processedBytes = 0;
            byte[] compressedBlocks = new byte[outputSize];
            if (additionalBitsNumber > 0)
                compressedBlocks[0] = additionalByte;

            for (int i = 0; i < blockNumber; i++) {
                int blockSize = Math.min(content.length - processedBytes, maxBlockSize);
                processedBytes += blockSize;
                boolean isLastBlock = false;
                if (content.length - processedBytes == 0 && isLastDataSet)
                    isLastBlock = true;
                compressWithDynamicsHuffmanCodes(
                        Arrays.copyOfRange(content, i * maxBlockSize, i * maxBlockSize + blockSize),
                        filePosition,
                        compressedBlocks,
                        isLastBlock);
                compressedOutputBytes = (int) (filePosition.getPosition() / BITS_IN_BYTE);
                if (filePosition.getPosition() % BITS_IN_BYTE != 0)
                    compressedOutputBytes += 1;
            }

            byte[] output  = new byte[compressedOutputBytes];
            if (compressedOutputBytes >= 0)
                System.arraycopy(compressedBlocks, 0, output, 0, compressedOutputBytes);
            return new CompressionOutput(output, (int) (filePosition.getPosition() % BITS_IN_BYTE));
        }
    }


    public byte[] decompress(byte[] content, int outputSize) {
        byte[] output = new byte[Math.toIntExact(outputSize)];

        readCompressedContent(content, output, outputSize);

        return output;
    }


    private byte[] compressWithStaticsHuffmanCodes(byte[] content, FilePosition filePosition, byte[] output, boolean isLastBlock) {
        BitReader bitReader = new BitReader();

        BlockHeader blockHeader = new BlockHeader(isLastBlock, CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES);

        List<Integer> compressedContent = new ArrayList<>();
        for (int i = 0; i < content.length; i++) {
            compressedContent.add((((int) content[i]) & 0x0ff)); // change this line to LZ77 compression
        }
        compressedContent.add(END_OF_BLOCK);

        return writeHuffmanCodes(content, filePosition, output, bitReader, isLastBlock, blockHeader, compressedContent);
    }


    private byte[] compressWithDynamicsHuffmanCodes(byte[] content, FilePosition filePosition, byte[] output, boolean isLastBlock) {
        BitReader bitReader = new BitReader();

        BlockHeader blockHeader = new BlockHeader(isLastBlock, CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES);

        List<Integer> compressedContent = new ArrayList<>();
        for (int i = 0; i < content.length; i++) {
            compressedContent.add((((int) content[i]) & 0x0ff)); // change this line to LZ77 compression
        }
        compressedContent.add(END_OF_BLOCK);

        return writeHuffmanCodes(content, filePosition, output, bitReader, isLastBlock, blockHeader, compressedContent);
    }


    private byte[] writeHuffmanCodes(byte[] content, FilePosition filePosition, byte[] output, BitReader bitReader,
                                     boolean isLastBlock, BlockHeader blockHeader, List<Integer> compressedContent) {
        CodeTreesRepresener codeTreesRepresener =
                new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
        codeTreesRepresener.generateCodeTreesRepresentation(compressedContent);

        HuffmanCodeLengthData[] compressedHuffmanCodes = new HuffmanCodeLengthData[compressedContent.size()];
        int compressedHuffmanCodesPosition;
        for (compressedHuffmanCodesPosition = 0;
             compressedHuffmanCodesPosition < compressedContent.size();
             compressedHuffmanCodesPosition++) {
            for (HuffmanCodeLengthData huffmanLengthCode : codeTreesRepresener.getHuffmanLengthCodes()) {
                if (huffmanLengthCode.getIndex() == compressedContent.get(compressedHuffmanCodesPosition)) {
                    compressedHuffmanCodes[compressedHuffmanCodesPosition] = huffmanLengthCode;
                }
            }
        }
//        for (HuffmanCodeLengthData huffmanLengthCode: codeTreesRepresener.getHuffmanLengthCodes()) {
//            if (huffmanLengthCode.getIndex() == END_OF_BLOCK) {
//                compressedHuffmanCodes[compressedHuffmanCodesPosition] = huffmanLengthCode;
//                compressedHuffmanCodesPosition++;
//            }
//        }

        output = writeBlockHeader(output, filePosition, isLastBlock, blockHeader);
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES) {
            output = writeHeaderForDynamicsHuffmanCodes(filePosition, output, bitReader, codeTreesRepresener);

        }
        for (HuffmanCodeLengthData huffmanLengthCode: compressedHuffmanCodes) {
            output = bitReader.setBits(output, filePosition.getPosition(), huffmanLengthCode.bitsNumber,
                    huffmanLengthCode.huffmanCode);
            filePosition.increasePosition(huffmanLengthCode.bitsNumber);
        }
        int endPosition = (int) (filePosition.getPosition() / BITS_IN_BYTE);
        if (filePosition.getPosition() % BITS_IN_BYTE != 0)
            endPosition++;
        return Arrays.copyOfRange(output, 0, endPosition); //invalid
    }

    private byte[] writeHeaderForDynamicsHuffmanCodes(FilePosition filePosition, byte[] output, BitReader bitReader, CodeTreesRepresener codeTreesRepresener) {
        List<Integer> headerHuffmanLengthCodes = new ArrayList<>(CodeTreesRepresener.NUMBER_OF_HUFFMAN_CODES);
        List<Integer> headerHuffmanDistanceCodes = new ArrayList<>(CodeTreesRepresener.NUMBER_OF_DISTANCE_CODES);
        for (HuffmanCodeLengthData code : codeTreesRepresener.getHuffmanLengthCodes()) {
            headerHuffmanLengthCodes.add(code.bitsNumber);
        }
        int distanceCodesNumber = 1;
        for (int i = 0; i < distanceCodesNumber; i++) { // TODO : only temporary - it should be replaced with generating distance codes
            headerHuffmanDistanceCodes.add(i);
        }

        int literalLengthAlphabetLength = Math.max(getNonZeroElementsNumber(headerHuffmanLengthCodes), 257);

        headerHuffmanLengthCodes = Stream.concat(headerHuffmanLengthCodes.stream(), headerHuffmanDistanceCodes.stream())
                .collect(Collectors.toList());
        headerHuffmanLengthCodes.add(17);
        headerHuffmanLengthCodes.add(18);


        List<HuffmanCodeLengthData> compressedHeaderHuffmanCodes =
                Arrays.asList(
                        Arrays.copyOfRange(
                                codeTreesRepresener.initializeHuffmanCodeLengthData(headerHuffmanLengthCodes), 0, 19));

//        List<HuffmanCodeLengthData> compressedDistanceCodes =
//                Arrays.asList(
//                        Arrays.copyOfRange(
//                                codeTreesRepresener.initializeHuffmanCodeLengthData(headerHuffmanDistanceCodes), 0, 19));

        int codeAlphabetLength = 18;
        output = codeTreesRepresener.setLiteralLengthAlphabetLength(literalLengthAlphabetLength, output, filePosition.getPosition());
        filePosition.increasePosition(5);
        output = codeTreesRepresener.setDistanceAlphabetLength(distanceCodesNumber, output, filePosition.getPosition());
        filePosition.increasePosition(5);
        output = codeTreesRepresener.setCodeAlphabetLength(codeAlphabetLength, output, filePosition.getPosition());
        filePosition.increasePosition(4);

        output = codeTreesRepresener.writeHeaderHuffmanCodeAlphabet(compressedHeaderHuffmanCodes, output, filePosition, codeAlphabetLength);
        codeTreesRepresener.generateDynamicsHuffmanLengthCodes(compressedHeaderHuffmanCodes);

        List<HuffmanCodeLengthData> huffmanCodeLengthData = codeTreesRepresener.getHuffmanLengthCodes();
        for (int i = 0; i < literalLengthAlphabetLength; i++) {
            HuffmanCodeLengthData code = huffmanCodeLengthData.get(i);
            output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(code.bitsNumber).bitsNumber,
                    compressedHeaderHuffmanCodes.get(code.bitsNumber).huffmanCode);
            filePosition.increasePosition(compressedHeaderHuffmanCodes.get(code.bitsNumber).bitsNumber);
            if (code.bitsNumber == 0) {
                int zerosToCopy = 0;
                // 138 is maximum allowed number of copying 0
                for (int j = i + 1; j <= Math.min(i + 138, literalLengthAlphabetLength); j++) {
                    if (huffmanCodeLengthData.get(j).bitsNumber == 0)
                        zerosToCopy = j - i;
                    else
                        break;
                }
                if (zerosToCopy >= 3 && zerosToCopy <= 10) {
                    output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(17).bitsNumber,
                            compressedHeaderHuffmanCodes.get(17).huffmanCode);
                    filePosition.increasePosition(compressedHeaderHuffmanCodes.get(17).bitsNumber);
                    output = bitReader.setBitsLittleEndian(output, filePosition.getPosition(), 3, zerosToCopy - 3);
                    filePosition.increasePosition(3);
                    i += zerosToCopy;
                } else if (zerosToCopy >= 11) {
                    output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(18).bitsNumber,
                            compressedHeaderHuffmanCodes.get(18).huffmanCode);
                    filePosition.increasePosition(compressedHeaderHuffmanCodes.get(18).bitsNumber);
                    output = bitReader.setBitsLittleEndian(output, filePosition.getPosition(), 7, zerosToCopy - 11);
                    filePosition.increasePosition(7);
                    i += zerosToCopy;
                }
            }
        }
//        for (int i = 0; i < distanceCodesNumber; i++) {
//            HuffmanCodeLengthData code = huffmanCodeLengthData.get(i);
//            output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(code.bitsNumber).bitsNumber,
//                    compressedHeaderHuffmanCodes.get(code.bitsNumber).huffmanCode);
//            filePosition.increasePosition(compressedHeaderHuffmanCodes.get(code.bitsNumber).bitsNumber);
//        }

        output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(0).bitsNumber,
                compressedHeaderHuffmanCodes.get(0).huffmanCode);
        filePosition.increasePosition(compressedHeaderHuffmanCodes.get(0).bitsNumber); // TODO change to method which generates distance codes

        return output;
    }

    private int getNonZeroElementsNumber(List<Integer> headerHuffmanLengthCodes) {
        int lastNonZeroIndex = 0;
        for (int i = 0; i < headerHuffmanLengthCodes.size(); i++) {
            if (headerHuffmanLengthCodes.get(i) != 0)
                lastNonZeroIndex = i;
        }
        return lastNonZeroIndex;
    }

    private byte[] writeBlockHeader(byte[] output, FilePosition filePosition, boolean isLastBlock, BlockHeader blockHeader) {
        BitReader bitReader = new BitReader();

        output = bitReader.setBits(output, filePosition.getPosition(), 1, isLastBlock ? 1 : 0);
        filePosition.increasePosition(1);
        output = bitReader.setBits(output, filePosition.getPosition(), 2, blockHeader.getCompressionType().getCompressionTypeCode());
        filePosition.increasePosition(2);

        return output;
    }


    private void readCompressedContent(byte[] content, byte[] output,  int outputSize) {
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

    private boolean isNextBlockExists(int outputSize, FilePosition filePosition) {
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
                bitsNumber =  codeTreesRepresener.getBiggestHuffmanLength();
                canReuseBits = false;
                continue;
            }
            bitsNumber--;
//            System.out.print(100 * filePosition.getPosition() / output.length + " %\r");
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
        System.arraycopy(content, filePosition.getOffset() / BITS_IN_BYTE, output, (int) filePosition.getPosition(), blockSize);

        for (int i = 0; i < blockSize; i++) {
            output[(int) filePosition.getPosition()] = content[filePosition.getOffset() / BITS_IN_BYTE];
            filePosition.increasePosition(1);
            filePosition.increaseOffset(BITS_IN_BYTE);
        }

//        System.out.print(100 * filePosition.getPosition() / output.length + " %\r");
    }

    private void copyByteToOutputStream(byte[] output, FilePosition filePosition, HuffmanCodeLengthData huffmanLengthCode) {
        output[(int) filePosition.getPosition()] = (byte) huffmanLengthCode.getIndex();
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

        int copyPosition = (int) (filePosition.getPosition() - distanceCodeOutput.getDistance());


        for (int i = 0; i < lengthCode.getLength() + additionalLength; i++) {
            output[(int) filePosition.getPosition()] = output[copyPosition];
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

            DistanceCode distanceCode = codeTreesRepresener.getDistanceCode(bitsNumber, distanceCodeInt);

            if (distanceCode != null) {
                offset += bitsNumber;
                distance = distanceCode.getDistance();
                int additionalDistance = bitReader.getBitsLittleEndian(content, offset, distanceCode.getExtraBits());
                offset += distanceCode.getExtraBits();
                return new DistanceCodeOutput(offset, distance + additionalDistance);
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
