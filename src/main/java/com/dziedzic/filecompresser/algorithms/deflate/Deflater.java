package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.*;
import com.dziedzic.filecompresser.zip.Entity.CompressionOutput;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Deflater {

    private static final int END_OF_BLOCK = 256;
    private final int BITS_IN_BYTE = 8;
    private final int MAX_BLOCK_SIZE = 32768;

    public CompressionOutput compress(byte[] content, byte additionalByte, int additionalBitsNumber, boolean isLastDataSet, String huffmanCodesMode, int maxBlockSize) {
        if (maxBlockSize > MAX_BLOCK_SIZE)
            maxBlockSize = MAX_BLOCK_SIZE;
        FilePosition filePosition = new FilePosition(0, additionalBitsNumber);
        int maxBlockHeaderSize = 286 * 8 + 32 + 19;
        int outputSize = 2 * content.length + maxBlockHeaderSize;

        if (content.length < 512) {
            byte[] output = new byte[outputSize];
            if (additionalBitsNumber > 0)
                output[0] = additionalByte;
            return new CompressionOutput(compressWithStaticsHuffmanCodes(content, filePosition, output, isLastDataSet), 0);
        } else if (content.length < 65000) {
            byte[] output = new byte[outputSize];
            if (additionalBitsNumber > 0)
                output[0] = additionalByte;
            if (huffmanCodesMode.equals("static"))
                return new CompressionOutput(compressWithStaticsHuffmanCodes(content, filePosition, output, isLastDataSet), 0);
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
                if (huffmanCodesMode.equals("static"))
                    compressWithStaticsHuffmanCodes(
                            Arrays.copyOfRange(content, i * maxBlockSize, i * maxBlockSize + blockSize),
                            filePosition,
                            compressedBlocks,
                            isLastBlock);
                else
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

        List<LZ77Output> compressedContent = performLZ77compression(content);

        return writeHuffmanCodes(content, filePosition, output, bitReader, isLastBlock, blockHeader, compressedContent);
    }


    private byte[] compressWithDynamicsHuffmanCodes(byte[] content, FilePosition filePosition, byte[] output, boolean isLastBlock) {
        BitReader bitReader = new BitReader();
        BlockHeader blockHeader = new BlockHeader(isLastBlock, CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES);

        List<LZ77Output> compressedContent = performLZ77compression(content);

        return writeHuffmanCodes(content, filePosition, output, bitReader, isLastBlock, blockHeader, compressedContent);
    }

    private List<LZ77Output> performLZ77compression(byte[] content) {
        List<LZ77Output> compressedContent = new ArrayList<>();
        CodeTreesRepresener codeTreesRepresener =
                new CodeTreesRepresener(content);
        codeTreesRepresener.generateStaticDistanceCodes();
        codeTreesRepresener.generateStaticLengthCodes();

        Multimap<Integer, Integer> hashDictionary = HashMultimap.create();

        for (int i = 0; i < content.length; i++) {
            if (i >= 5 && i + 2 < content.length) {
                int key = (int)content[i] * 100000 + (int)content[i + 1] * 1000 + (int)content[i + 2];
                int maxMatchedElements = 0;
                int indexOfMatchedSubstring = 0;
                if (hashDictionary.containsKey(key)) {
                    RepeatedElementFinder repeatedElementFinder = new RepeatedElementFinder(content, hashDictionary, i, key, maxMatchedElements, indexOfMatchedSubstring).invoke();
                    maxMatchedElements = repeatedElementFinder.getMaxMatchedElements();
                    indexOfMatchedSubstring = repeatedElementFinder.getIndexOfMatchedSubstring();

                    LengthCode lengthCode = codeTreesRepresener.findLengthCodeByLength(maxMatchedElements);
                    DistanceCode distanceCode = codeTreesRepresener.findDistanceCode(i - indexOfMatchedSubstring);
                    if (lengthCode != null && distanceCode != null) {
                        compressedContent.add(new LZ77Output(lengthCode.getCode(), lengthCode.getExtraBits(), maxMatchedElements - lengthCode.getLength()));
                        compressedContent.add(new LZ77Output(distanceCode.getCode(), distanceCode.getExtraBits(),
                                i - indexOfMatchedSubstring - distanceCode.getDistance(), distanceCode.getBitsNumber(), true));
                    } else {
                        maxMatchedElements = 0;
                    }

                }

                i += maxMatchedElements;
                if (i >= content.length)
                    continue;
            }
            if (i >= 2) {
                int key = (int)content[i - 2] * 100000 + (int)content[i - 1] * 1000 + (int)content[i];
                hashDictionary.put(key, i - 2);
            }
            compressedContent.add(new LZ77Output(((int) content[i]) & 0x0ff, 0, 0));
        }
        compressedContent.add(new LZ77Output(END_OF_BLOCK, 0, 0));
        return compressedContent;
    }


    private byte[] writeHuffmanCodes(byte[] content, FilePosition filePosition, byte[] output, BitReader bitReader,
                                     boolean isLastBlock, BlockHeader blockHeader, List<LZ77Output> compressedContent) {
        List<Integer> compressedLengthCodes = new ArrayList<>();
        Set<Integer> compressedDistanceCodes = new HashSet<Integer>() {
        };
        for (LZ77Output lz77Output : compressedContent) {
            compressedLengthCodes.add(lz77Output.getCode());
            if (lz77Output.isDistanceCode())
                compressedDistanceCodes.add(lz77Output.getCode());
        }

        CodeTreesRepresener codeTreesRepresener =
                new CodeTreesRepresener(content, blockHeader, filePosition.getOffset());
        codeTreesRepresener.generateCodeTreesRepresentation(compressedLengthCodes, compressedDistanceCodes);

        for (LZ77Output value : compressedContent) {
            if (!value.isDistanceCode()) {
                for (HuffmanCodeLengthData huffmanLengthCode : codeTreesRepresener.getHuffmanLengthCodes()) {
                    if (huffmanLengthCode.getIndex() == value.getCode()) {
                        value.setBitsNumber(huffmanLengthCode.bitsNumber);
                        value.setHuffmanCode(huffmanLengthCode.huffmanCode);
                    }
                }
            } else {
                List<DistanceCode> distanceCodes = codeTreesRepresener.getDistanceCodes();
                value.setBitsNumber(distanceCodes.get(value.getCode()).getBitsNumber());
                value.setHuffmanCode(distanceCodes.get(value.getCode()).getCode());
            }
        }

        output = writeBlockHeader(output, filePosition, isLastBlock, blockHeader);
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES) {
            output = writeHeaderForDynamicsHuffmanCodes(filePosition, output, bitReader, codeTreesRepresener);

        }
        for (LZ77Output lz77Output: compressedContent) {
            if (lz77Output.isDistanceCode()) {
                output = bitReader.setBits(output, filePosition.getPosition(), lz77Output.getBitsNumber(),
                        lz77Output.getHuffmanCode());
            } else {
                output = bitReader.setBits(output, filePosition.getPosition(), lz77Output.getBitsNumber(),
                        lz77Output.getHuffmanCode());
            }
            filePosition.increasePosition(lz77Output.getBitsNumber());
            if (lz77Output.getExtraBits() > 0) {
                output = bitReader.setBitsLittleEndian(output, filePosition.getPosition(), lz77Output.getExtraBits(),
                        lz77Output.getAdditionalValue());
                filePosition.increasePosition(lz77Output.getExtraBits());
            }
        }
        int endPosition = (int) (filePosition.getPosition() / BITS_IN_BYTE);
        if (filePosition.getPosition() % BITS_IN_BYTE != 0)
            endPosition++;
        return Arrays.copyOfRange(output, 0, endPosition);
    }

    private byte[] writeHeaderForDynamicsHuffmanCodes(FilePosition filePosition, byte[] output, BitReader bitReader, CodeTreesRepresener codeTreesRepresener) {
        List<Integer> headerHuffmanLengthCodes = new ArrayList<>(CodeTreesRepresener.NUMBER_OF_HUFFMAN_CODES);
        List<Integer> headerHuffmanDistanceCodes = new ArrayList<>(CodeTreesRepresener.NUMBER_OF_DISTANCE_CODES);
        for (HuffmanCodeLengthData code : codeTreesRepresener.getHuffmanLengthCodes()) {
            headerHuffmanLengthCodes.add(code.bitsNumber);
        }
        int distanceCodesNumber = codeTreesRepresener.getDistanceCodes().size();
        for (DistanceCode distanceCode : codeTreesRepresener.getDistanceCodes()) { // TODO : only temporary - it should be replaced with generating distance codes
            headerHuffmanDistanceCodes.add(distanceCode.getBitsNumber());
        }

        int literalLengthAlphabetLength = Math.max(getNonZeroElementsNumber(headerHuffmanLengthCodes), 257) + 1;

        headerHuffmanLengthCodes = Stream.concat(headerHuffmanLengthCodes.stream(), headerHuffmanDistanceCodes.stream())
                .collect(Collectors.toList());
        headerHuffmanLengthCodes.add(17);
        headerHuffmanLengthCodes.add(18);


        List<HuffmanCodeLengthData> compressedHeaderHuffmanCodes =
                Arrays.asList(
                        Arrays.copyOfRange(
                                codeTreesRepresener.initializeHuffmanCodeLengthData(headerHuffmanLengthCodes), 0, 19));

        int codeAlphabetLength = 0;
        for (int i = 0; i < compressedHeaderHuffmanCodes.size(); i++) {
            if (compressedHeaderHuffmanCodes.get(i).bitsNumber > 0) {
                if (codeTreesRepresener.getCodeLengthIndex(i) > codeAlphabetLength)
                    codeAlphabetLength = codeTreesRepresener.getCodeLengthIndex(i);
            }
        }
        output = copyAlphabetsLengthsToOutput(filePosition, output, codeTreesRepresener, distanceCodesNumber, literalLengthAlphabetLength, codeAlphabetLength);

        output = codeTreesRepresener.writeHeaderHuffmanCodeAlphabet(compressedHeaderHuffmanCodes, output, filePosition, codeAlphabetLength);
        codeTreesRepresener.generateDynamicsHuffmanLengthCodes(compressedHeaderHuffmanCodes);

        List<HuffmanCodeLengthData> huffmanCodeLengthData = codeTreesRepresener.getHuffmanLengthCodes();
        output = copyLiteralLengthAlphabetToOutput(filePosition, output, bitReader, literalLengthAlphabetLength, compressedHeaderHuffmanCodes, huffmanCodeLengthData);
        List<DistanceCode> distanceCodes = codeTreesRepresener.getDistanceCodes();
        for (int i = 0; i < distanceCodesNumber; i++) {
            DistanceCode code = distanceCodes.get(i);
            output = bitReader.setBits(output, filePosition.getPosition(), compressedHeaderHuffmanCodes.get(code.getBitsNumber()).bitsNumber,
                    compressedHeaderHuffmanCodes.get(code.getBitsNumber()).huffmanCode);
            filePosition.increasePosition(compressedHeaderHuffmanCodes.get(code.getBitsNumber()).bitsNumber);
        }

        return output;
    }

    private byte[] copyAlphabetsLengthsToOutput(FilePosition filePosition, byte[] output, CodeTreesRepresener codeTreesRepresener, int distanceCodesNumber, int literalLengthAlphabetLength, int codeAlphabetLength) {
        output = codeTreesRepresener.setLiteralLengthAlphabetLength(literalLengthAlphabetLength, output, filePosition.getPosition());
        filePosition.increasePosition(5);
        output = codeTreesRepresener.setDistanceAlphabetLength(distanceCodesNumber, output, filePosition.getPosition());
        filePosition.increasePosition(5);
        output = codeTreesRepresener.setCodeAlphabetLength(codeAlphabetLength, output, filePosition.getPosition());
        filePosition.increasePosition(4);
        return output;
    }

    private byte[] copyLiteralLengthAlphabetToOutput(FilePosition filePosition, byte[] output, BitReader bitReader, int literalLengthAlphabetLength, List<HuffmanCodeLengthData> compressedHeaderHuffmanCodes, List<HuffmanCodeLengthData> huffmanCodeLengthData) {
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
