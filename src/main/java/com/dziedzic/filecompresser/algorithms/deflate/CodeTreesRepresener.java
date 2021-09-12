package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.max;

public class CodeTreesRepresener {
    public static final int NUMBER_OF_HUFFMAN_CODES = 288;
    public static final int NUMBER_OF_DISTANCE_CODES = 32;
    final int MIN_DISTANCE_CODE_LENGTH = 1;
    private final int MAX_HUFFMAN_LENGTH = 16;
    private final int LENGTH_CODE_START_POSITION = 257;
    private List<DistanceCode> distanceCodes;
    private List<LengthCode> lengthCodes;
    private List<HuffmanCodeLengthData> huffmanLengthCodes;

    private ArrayList<ArrayList<HuffmanCodeLengthData>> huffmanLengthCodesByBitsNumber;
    private ArrayList<ArrayList<DistanceCode>> distanceCodesByBitsNumber;

    private int smallestHuffmanLength;
    private int biggestHuffmanLength;
    private int biggestDistanceCodeLength;
    private byte[] blockContent;
    private BlockHeader blockHeader;
    private int offset;

    CodeTreesRepresener(byte[] blockContent, BlockHeader blockHeader, int offset) {
        this.blockContent = blockContent;
        this.blockHeader = blockHeader;
        this.offset = offset;
        smallestHuffmanLength = 0;
        biggestHuffmanLength = 0;
        distanceCodes = new ArrayList<>();
        lengthCodes = new ArrayList<>();
        huffmanLengthCodes = new ArrayList<>();
        huffmanLengthCodesByBitsNumber = new ArrayList<>();
        distanceCodesByBitsNumber = new ArrayList<>();
    }

    void generateCodeTreesRepresentation(List<Integer> compressedContent) {
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES) {
            huffmanLengthCodes = Arrays.asList(initializeHuffmanCodeLengthData(compressedContent));
            generateDynamicsHuffmanLengthCodes(huffmanLengthCodes);
            generateStaticLengthCodes();

            List<HuffmanCodeLengthData> distanceHuffmanLengths = new ArrayList<>(); // TODO
            generateDynamicsHuffmanLengthCodes(distanceHuffmanLengths);
            generateDynamicsDistanceCodes(distanceHuffmanLengths);


            groupHuffmanLengthCodesByBitsNumber();
            groupDistanceCodesByBitsNumber();

//            generateStaticCodeTreesRepresentation();
        }
        else if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES)
            generateStaticCodeTreesRepresentation();
    }

    HuffmanCodeLengthData[] initializeHuffmanCodeLengthData(List<Integer> compressedContent) {
        Integer[] codesCounter = new Integer[NUMBER_OF_HUFFMAN_CODES];
        Arrays.fill(codesCounter, 0);
        for (Integer element : compressedContent) {
            codesCounter[element]++;
        }
        Integer[] indices = new Integer[codesCounter.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, new Comparator<Integer>() {

            public int compare(Integer i1, Integer i2) {
                return codesCounter[i2].compareTo(codesCounter[i1]);
            }
        });

        HuffmanCodeLengthData[] huffmanCodeLengthData = new HuffmanCodeLengthData[codesCounter.length];
        int maxElementNumber = 4;
        int currentElement = 0;
        for (int bitsNumber = 4; bitsNumber <= MAX_HUFFMAN_LENGTH; bitsNumber++) {
            for (int j = 0; j < maxElementNumber; j++) {
                if (currentElement >= codesCounter.length)
                    break;
                if (codesCounter[indices[currentElement]] != 0)
                    huffmanCodeLengthData[indices[currentElement]] = new HuffmanCodeLengthData(indices[currentElement], bitsNumber);
                else
                    huffmanCodeLengthData[indices[currentElement]] = new HuffmanCodeLengthData(indices[currentElement], 0);
                currentElement++;
            }
            if (currentElement >= codesCounter.length)
                break;
            maxElementNumber *= 2;
        }
        return huffmanCodeLengthData;
    }

    void readCodeTreesRepresentation() {
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES)
            readDynamicCodeTreesRepresentation();
        else if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES)
            generateStaticCodeTreesRepresentation();
    }

    public int getSmallestHuffmanLength() {
        return smallestHuffmanLength;
    }

    int getBiggestDistanceCodeLength() {
        return biggestDistanceCodeLength;
    }

    List<DistanceCode> getDistanceCodes() {
        return distanceCodes;
    }


    LengthCode findLengthCode(int code) {
        for (LengthCode lengthCode : lengthCodes) {
            if (lengthCode.getCode() == code)
                return lengthCode;
        }
        return null;
    }

    List<HuffmanCodeLengthData> getHuffmanLengthCodes() {
        return huffmanLengthCodes;
    }

    HuffmanCodeLengthData getHuffmanLengthCode(int bitsNumber, int huffmanCode) {
        try {
            for (HuffmanCodeLengthData huffmanLengthCode: huffmanLengthCodesByBitsNumber.get(bitsNumber)) {
                if (huffmanLengthCode.getBitsNumber() != bitsNumber)
                    continue;
                if (huffmanLengthCode.getHuffmanCode() == huffmanCode)
                    return huffmanLengthCode;
            };
        } catch (Exception ex) {
            throw new RuntimeException("Error - failed to get HuffmanLengthCode");
        }
        return null;
    }

    DistanceCode getDistanceCode(int bitsNumber, int huffmanCode) {
        try {
            for (DistanceCode distanceCode: distanceCodesByBitsNumber.get(bitsNumber)) {
                if (distanceCode.getBitsNumber() != bitsNumber)
                    continue;
                if (distanceCode.getCode() == huffmanCode)
                    return distanceCode;
            };
        } catch (Exception ex) {
            throw new RuntimeException("Error - failed to get DistanceCode");
        }
        return null;
    }

    private void generateStaticCodeTreesRepresentation() {
        generateStaticDistanceCodes();
        generateStaticLengthCodes();
        generateStaticHuffmanLengthCodes();
        groupHuffmanLengthCodesByBitsNumber();
        groupDistanceCodesByBitsNumber();
        findBiggestHuffmanLength();
    }

    public int getOffset() {
        return offset;
    }

    private void readDynamicCodeTreesRepresentation() {
        int alphabetLength = getLiteralLengthAlphabetLength();
        int distanceAlphabetLength = getDistanceAlphabetLength();
        int codeAlphabetLength = getCodeAlphabetLength();
        List<HuffmanCodeLengthData> huffmanCodeLengthDataList = readCodeLengthsForCodeLengthAlphabet(codeAlphabetLength);
        generateDynamicsHuffmanLengthCodes(huffmanCodeLengthDataList);

        // ToDo return output
        huffmanLengthCodes = readHuffmanCodes(alphabetLength, huffmanCodeLengthDataList);
        generateDynamicsHuffmanLengthCodes(huffmanLengthCodes);
        findSmallestHuffmanLength();
        findBiggestHuffmanLength();

        List<HuffmanCodeLengthData> distanceHuffmanLengths = readHuffmanCodes(distanceAlphabetLength, huffmanCodeLengthDataList);
        generateDynamicsHuffmanLengthCodes(distanceHuffmanLengths);
        generateDynamicsDistanceCodes(distanceHuffmanLengths);

        generateStaticLengthCodes();
        groupHuffmanLengthCodesByBitsNumber();
        groupDistanceCodesByBitsNumber();
    }


    private void groupDistanceCodesByBitsNumber() {
        int maxBitsNumber = 0;
        for (DistanceCode distanceCode : distanceCodes) {
            if (distanceCode.getBitsNumber() > maxBitsNumber)
                maxBitsNumber = distanceCode.getBitsNumber();
        }
        for (int i = 0; i <= maxBitsNumber; i++) {
            distanceCodesByBitsNumber.add(new ArrayList<>());
        }
        for (DistanceCode distanceCode : distanceCodes) {
            distanceCodesByBitsNumber.get(distanceCode.getBitsNumber()).add(distanceCode);
        }
    }


    private void groupHuffmanLengthCodesByBitsNumber() {
        int maxBitsNumber = 0;
        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanLengthCodes) {
            if (huffmanCodeLengthData.getBitsNumber() > maxBitsNumber)
                maxBitsNumber = huffmanCodeLengthData.bitsNumber;
        }
        for (int i = 0; i <= maxBitsNumber; i++) {
            huffmanLengthCodesByBitsNumber.add(new ArrayList<>());
        }
        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanLengthCodes) {
            huffmanLengthCodesByBitsNumber.get(huffmanCodeLengthData.getBitsNumber()).add(huffmanCodeLengthData);
        }
    }


    private int getLiteralLengthAlphabetLength() {
        BitReader bitReader = new BitReader();

        int code = bitReader.getBitsLittleEndian(blockContent, offset, 5);
        offset += 5;

        return code + 257;
    }

    private int getDistanceAlphabetLength() {
        BitReader bitReader = new BitReader();

        int code = bitReader.getBitsLittleEndian(blockContent, offset, 5);
        offset += 5;

        return code + 1;
    }

    private int getCodeAlphabetLength() {
        BitReader bitReader = new BitReader();

        int code = bitReader.getBitsLittleEndian(blockContent, offset, 4);
        offset += 4;

        return code + 4;
    }


    byte[] setLiteralLengthAlphabetLength(int value, byte[] output, int offset) {
        BitReader bitReader = new BitReader();

        return bitReader.setBitsLittleEndian(output, offset, 5, value - 257);
    }

    byte[] setDistanceAlphabetLength(int value, byte[] output, int offset) {
        BitReader bitReader = new BitReader();

        return bitReader.setBitsLittleEndian(output, offset, 5, value - 1);
    }

    byte[] setCodeAlphabetLength(int value, byte[] output, int offset) {
        BitReader bitReader = new BitReader();

        return bitReader.setBitsLittleEndian(output, offset, 4, value - 4);
    }

    byte[] writeHeaderHuffmanCodeAlphabet(List<HuffmanCodeLengthData> compressedHeaderHuffmanCodes, byte[] output, FilePosition filePosition, int codeAlphabetLength) {
        BitReader bitReader = new BitReader();
        for (int i = 0; i < codeAlphabetLength; i++) {
            output = bitReader.setBitsLittleEndian(
                    output, filePosition.getPosition(), 3, compressedHeaderHuffmanCodes.get(getCodeLengthIndex(i)).bitsNumber);
            filePosition.increasePosition(3);
        }
        return output;
    }

    private List<HuffmanCodeLengthData> readCodeLengthsForCodeLengthAlphabet(int codesNumber) {
        BitReader bitReader = new BitReader();
        List<HuffmanCodeLengthData> huffmanCodeLengthDataList = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            huffmanCodeLengthDataList.add(new HuffmanCodeLengthData(i,0));
        }
        for (int i = 0; i < codesNumber; i++) {
            huffmanCodeLengthDataList.get(getCodeLengthIndex(i)).bitsNumber = bitReader.getBitsLittleEndian(blockContent, offset, 3);
            offset += 3;
        }
        return huffmanCodeLengthDataList;
    }

    private int getCodeLengthIndex(int position) {
        switch (position) {
            case 0:
                return 16;
            case 1:
                return 17;
            case 2:
                return 18;
            case 3:
                return 0;
            case 4:
                return 8;
            case 5:
                return 7;
            case 6:
                return 9;
            case 7:
                return 6;
            case 8:
                return 10;
            case 9:
                return 5;
            case 10:
                return 11;
            case 11:
                return 4;
            case 12:
                return 12;
            case 13:
                return 3;
            case 14:
                return 13;
            case 15:
                return 2;
            case 16:
                return 14;
            case 17:
                return 1;
            case 18:
                return 15;
            default:
                return -1;
        }
    }

    void generateDynamicsHuffmanLengthCodes(List<HuffmanCodeLengthData> huffmanCodeLengthDataList) {
        int [] codeLengthOccurrences = new int[MAX_HUFFMAN_LENGTH];
        int [] nextCodes = new int[MAX_HUFFMAN_LENGTH];
        int [] huffmanCodes = new int[huffmanCodeLengthDataList.size()];

        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanCodeLengthDataList) {
            codeLengthOccurrences[huffmanCodeLengthData.bitsNumber]++;
        }

        codeLengthOccurrences[0] = 0;

        for (int i = 1; i < MAX_HUFFMAN_LENGTH; i++) {
            nextCodes[i] = 2 * (nextCodes[i - 1] + codeLengthOccurrences[i - 1]);
        }

        for (int i = 0; i < huffmanCodes.length; i++) {
            if (huffmanCodeLengthDataList.get(i).bitsNumber == 0)
                continue;
            huffmanCodes[i] = nextCodes[huffmanCodeLengthDataList.get(i).bitsNumber];
            nextCodes[huffmanCodeLengthDataList.get(i).bitsNumber]++;
        }

        for (int i = 0; i < huffmanCodes.length; i++) {
            if (huffmanCodeLengthDataList.get(i).bitsNumber == 0)
                continue;
            huffmanCodeLengthDataList.get(i).huffmanCode = huffmanCodes[i];
            huffmanCodeLengthDataList.get(i).bitsNumber = max(getBitsNumber(huffmanCodes[i]), huffmanCodeLengthDataList.get(i).bitsNumber);
        }
    }

    private List<HuffmanCodeLengthData> readHuffmanCodes(int elementsNumber, List<HuffmanCodeLengthData> huffmanCodeLengthDataList) {
        BitReader bitReader = new BitReader();
        int bitsNumber = 15;
        int loadedElements = 0;
        List<HuffmanCodeLengthData> newHuffmanCodeLengthDataList = new ArrayList<>();

        while (loadedElements < elementsNumber) {
            boolean foundNext = false;
            while (!foundNext) {
                int codeInt = bitReader.getBits(blockContent, offset, bitsNumber);

                for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanCodeLengthDataList) {
                    if (huffmanCodeLengthData.getBitsNumber() == 0)
                        continue;

                    if (huffmanCodeLengthData.huffmanCode == codeInt && huffmanCodeLengthData.bitsNumber == bitsNumber) {
                        offset += bitsNumber;
                        if (huffmanCodeLengthData.index <= 15) {
                            newHuffmanCodeLengthDataList.add(new HuffmanCodeLengthData(loadedElements, huffmanCodeLengthData.index));
                            loadedElements++;
                        }
                        if (huffmanCodeLengthData.index == 16) {
                            int previousCodeValue = newHuffmanCodeLengthDataList.get(loadedElements - 1).bitsNumber;
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 2, 3, previousCodeValue);
                        } else if (huffmanCodeLengthData.index == 17) {
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 3, 3, 0);
                        } else if (huffmanCodeLengthData.index == 18) {
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 7, 11, 0);
                        }

                        bitsNumber = 15;
                        foundNext = true;
                        break;
                    }
                }
                bitsNumber--;
            }
        }
        return newHuffmanCodeLengthDataList;
    }

    private int repeatCodeLength(BitReader bitReader, int loadedElements, List<HuffmanCodeLengthData> newHuffmanCodeLengthDataList, int nextBits, int increaseValueBy, int valueToCopy) {
        int additionalBits = bitReader.getBitsLittleEndian(blockContent, offset, nextBits);
        int additionalBitsInt = additionalBits + increaseValueBy;
        offset += nextBits;
        for (int i = 0; i < additionalBitsInt; i++) {
            newHuffmanCodeLengthDataList.add(new HuffmanCodeLengthData(loadedElements, valueToCopy));
            loadedElements++;
        }
        return loadedElements;
    }

    private int getBitsNumber(int number) {
        if (number < 0)
            return 0;
        else if (number == 0)
            return 1;
        return (int)(Math.log(number) / Math.log(2) + 1);
    }

    private void findSmallestHuffmanLength() {
        smallestHuffmanLength = 1000000;
        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanLengthCodes) {
            if (huffmanCodeLengthData.bitsNumber == 0)
                continue;
            if (huffmanCodeLengthData.bitsNumber < smallestHuffmanLength)
                smallestHuffmanLength = huffmanCodeLengthData.bitsNumber;
        }
    }


    private void findBiggestHuffmanLength() {
        biggestHuffmanLength = 0;
        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanLengthCodes) {
            if (huffmanCodeLengthData.bitsNumber == 0)
                continue;
            if (huffmanCodeLengthData.bitsNumber > biggestHuffmanLength)
                biggestHuffmanLength = huffmanCodeLengthData.bitsNumber;
        }
    }

    private void generateStaticLengthCodes() {
        lengthCodes.add(new LengthCode(	257,0,3));
        lengthCodes.add(new LengthCode(	258,0,4));
        lengthCodes.add(new LengthCode(	259,0,5));
        lengthCodes.add(new LengthCode(	260,0,6));
        lengthCodes.add(new LengthCode(	261,0,7));
        lengthCodes.add(new LengthCode(	262,0,8));
        lengthCodes.add(new LengthCode(	263,0,9));
        lengthCodes.add(new LengthCode(	264,0,10));
        lengthCodes.add(new LengthCode(	265,1,11));
        lengthCodes.add(new LengthCode(	266,1,13));
        lengthCodes.add(new LengthCode(	267,1,15));
        lengthCodes.add(new LengthCode(	268,1,17));
        lengthCodes.add(new LengthCode(	269,2,19));
        lengthCodes.add(new LengthCode(	270,2,23));
        lengthCodes.add(new LengthCode(	271,2,27));
        lengthCodes.add(new LengthCode(	272,2,31));
        lengthCodes.add(new LengthCode(	273,3,35));
        lengthCodes.add(new LengthCode(	274,3,43));
        lengthCodes.add(new LengthCode(	275,3,51));
        lengthCodes.add(new LengthCode(	276,3,59));
        lengthCodes.add(new LengthCode(	277,4,67));
        lengthCodes.add(new LengthCode(	278,4,83));
        lengthCodes.add(new LengthCode(	279,4,99));
        lengthCodes.add(new LengthCode(	280,4,115));
        lengthCodes.add(new LengthCode(	281,5,131));
        lengthCodes.add(new LengthCode(	282,5,163));
        lengthCodes.add(new LengthCode(	283,5,195));
        lengthCodes.add(new LengthCode(	284,5,227));
        lengthCodes.add(new LengthCode(	285,0,258));
    }

    private void generateDynamicsDistanceCodes(List<HuffmanCodeLengthData> distanceHuffmanLengths) {
        List<DistanceCode> staticDistanceCodes = getStaticDistanceCodes();
        biggestDistanceCodeLength = 0;

        int index = 0;
        for (HuffmanCodeLengthData distanceHuffmanLength : distanceHuffmanLengths) {
            if (distanceHuffmanLength.bitsNumber > 0) {
                staticDistanceCodes.get(index).setCode(distanceHuffmanLength.huffmanCode);
                staticDistanceCodes.get(index).setBitsNumber(distanceHuffmanLength.bitsNumber);
                if (biggestDistanceCodeLength < distanceHuffmanLength.bitsNumber)
                    biggestDistanceCodeLength = distanceHuffmanLength.bitsNumber;
                distanceCodes.add(staticDistanceCodes.get(index));
            }
            index++;
        }
    }

    private void generateStaticDistanceCodes() {
        distanceCodes = getStaticDistanceCodes();

        biggestDistanceCodeLength = 5;
    }

    private List<DistanceCode> getStaticDistanceCodes() {
        List<DistanceCode> staticDistanceCodes = new ArrayList<>();

        staticDistanceCodes.add(new DistanceCode(	0,0,1));
        staticDistanceCodes.add(new DistanceCode(	1,0,2));
        staticDistanceCodes.add(new DistanceCode(	2,0,3));
        staticDistanceCodes.add(new DistanceCode(	3,0,4));
        staticDistanceCodes.add(new DistanceCode(	4,1,5));
        staticDistanceCodes.add(new DistanceCode(	5,1,7));
        staticDistanceCodes.add(new DistanceCode(	6,2,9));
        staticDistanceCodes.add(new DistanceCode(	7,2,13));
        staticDistanceCodes.add(new DistanceCode(	8,3,17));
        staticDistanceCodes.add(new DistanceCode(	9,3,25));
        staticDistanceCodes.add(new DistanceCode(	10,4,33));
        staticDistanceCodes.add(new DistanceCode(	11,4,49));
        staticDistanceCodes.add(new DistanceCode(	12,5,65));
        staticDistanceCodes.add(new DistanceCode(	13,5,97));
        staticDistanceCodes.add(new DistanceCode(	14,6,129));
        staticDistanceCodes.add(new DistanceCode(	15,6,193));
        staticDistanceCodes.add(new DistanceCode(	16,7,257));
        staticDistanceCodes.add(new DistanceCode(	17,7,385));
        staticDistanceCodes.add(new DistanceCode(	18,8,513));
        staticDistanceCodes.add(new DistanceCode(	19,8,769));
        staticDistanceCodes.add(new DistanceCode(	20,9,1025));
        staticDistanceCodes.add(new DistanceCode(	21,9,1537));
        staticDistanceCodes.add(new DistanceCode(	22,10,2049));
        staticDistanceCodes.add(new DistanceCode(	23,10,3073));
        staticDistanceCodes.add(new DistanceCode(	24,11,4097));
        staticDistanceCodes.add(new DistanceCode(	25,11,6145));
        staticDistanceCodes.add(new DistanceCode(	26,12,8193));
        staticDistanceCodes.add(new DistanceCode(	27,12,12289));
        staticDistanceCodes.add(new DistanceCode(	28,13,16385));
        staticDistanceCodes.add(new DistanceCode(	29,13,24577));

        return staticDistanceCodes;
    }

    private void generateStaticHuffmanLengthCodes() {
        for (int i = 0; i < 144; i++)
            huffmanLengthCodes.add(new HuffmanCodeLengthData(i, 8, 0b00110000 + i));
        for (int i = 0; i < 112; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(144 + i, 9, 0b110010000 + i));
        for (int i = 0; i < 24; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(256 + i, 7, i));
        for (int i = 0; i < 8; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(280 + i, 8, 0b11000000 + i));
        smallestHuffmanLength = 7;
    }

    int getBiggestHuffmanLength() {
        return biggestHuffmanLength;
    }
}
