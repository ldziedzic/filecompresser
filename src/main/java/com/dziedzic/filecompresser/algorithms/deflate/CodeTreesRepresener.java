package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.BlockHeader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CompressionType;
import com.dziedzic.filecompresser.algorithms.deflate.entity.DistanceCode;
import com.dziedzic.filecompresser.algorithms.deflate.entity.HuffmanCodeLengthData;
import com.dziedzic.filecompresser.algorithms.deflate.entity.LengthCode;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class CodeTreesRepresener {
    final int MIN_DISTANCE_CODE_LENGTH = 1;
    private final int MAX_HUFFMAN_LENGTH = 16;
    private List<DistanceCode> distanceCodes;
    private List<LengthCode> lengthCodes;
    private List<HuffmanCodeLengthData> huffmanLengthCodes;
    private int smallestHuffmanLength;
    private int biggestDistanceCodeLength;
    private byte[] blockContent;
    private BlockHeader blockHeader;
    private int offset;

    public CodeTreesRepresener(byte[] blockContent, BlockHeader blockHeader, int offset) {
        this.blockContent = blockContent;
        this.blockHeader = blockHeader;
        this.offset = offset;
        smallestHuffmanLength = 0;
        distanceCodes = new ArrayList<>();
        lengthCodes = new ArrayList<>();
        huffmanLengthCodes = new ArrayList<>();
    }

    public void generateCodeTreesRepresentation() {
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES) {

        }
        else if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES)
            generateStaticCodeTreesRepresentation();
    }

    public void readCodeTreesRepresentation() {
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES)
            readDynamicCodeTreesRepresentation();
        else if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_FIXED_HUFFMAN_CODES)
            generateStaticCodeTreesRepresentation();
    }

    public int getSmallestHuffmanLength() {
        return smallestHuffmanLength;
    }

    public int getBiggestDistanceCodeLength() {
        return biggestDistanceCodeLength;
    }

    public List<DistanceCode> getDistanceCodes() {
        return distanceCodes;
    }

    public List<LengthCode> getLengthCodes() {
        return lengthCodes;
    }

    public LengthCode findLengthCode(int code) {
        for (LengthCode lengthCode : lengthCodes) {
            if (lengthCode.getCode() == code)
                return lengthCode;
        }
        return null;
    }

    public List<HuffmanCodeLengthData> getHuffmanLengthCodes() {
        return huffmanLengthCodes;
    }

    private void generateStaticCodeTreesRepresentation() {
        generateStaticDistanceCodes();
        generateStaticLengthCodes();
        generateStaticHuffmanLengthCodes();
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

        List<HuffmanCodeLengthData> distanceHuffmanLengths = readHuffmanCodes(distanceAlphabetLength, huffmanCodeLengthDataList);
        generateDynamicsHuffmanLengthCodes(distanceHuffmanLengths);
        generateDynamicsDistanceCodes(distanceHuffmanLengths);

        generateStaticLengthCodes();
        return;
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

    private List<HuffmanCodeLengthData> readCodeLengthsForCodeLengthAlphabet(int codesNumber) {
        BitReader bitReader = new BitReader();
        List<HuffmanCodeLengthData> huffmanCodeLengthDataList = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            huffmanCodeLengthDataList.add(new HuffmanCodeLengthData(i,0));
        }
        for (int i = 0; i < codesNumber; i++) {
            huffmanCodeLengthDataList.get(getCodeLengthIndex(i)).lengthCode = bitReader.getBitsLittleEndian(blockContent, offset, 3);
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

    private void generateDynamicsHuffmanLengthCodes(List<HuffmanCodeLengthData> huffmanCodeLengthDataList) {
        int [] codeLengthOccurrences = new int[MAX_HUFFMAN_LENGTH];
        int [] nextCodes = new int[MAX_HUFFMAN_LENGTH];
        int [] huffmanCodes = new int[huffmanCodeLengthDataList.size()];

        for (HuffmanCodeLengthData huffmanCodeLengthData : huffmanCodeLengthDataList) {
            codeLengthOccurrences[huffmanCodeLengthData.lengthCode]++;
        }

        codeLengthOccurrences[0] = 0;

        for (int i = 1; i < MAX_HUFFMAN_LENGTH; i++) {
            nextCodes[i] = 2 * (nextCodes[i - 1] + codeLengthOccurrences[i - 1]);
        }

        for (int i = 0; i < huffmanCodes.length; i++) {
            if (huffmanCodeLengthDataList.get(i).lengthCode == 0)
                continue;
            huffmanCodes[i] = nextCodes[huffmanCodeLengthDataList.get(i).lengthCode];
            nextCodes[huffmanCodeLengthDataList.get(i).lengthCode]++;
        }

        for (int i = 0; i < huffmanCodes.length; i++) {
            if (huffmanCodeLengthDataList.get(i).lengthCode == 0)
                continue;
            huffmanCodeLengthDataList.get(i).huffmanCode = huffmanCodes[i];
            huffmanCodeLengthDataList.get(i).bitsNumber = max(getBitsNumber(huffmanCodes[i]), huffmanCodeLengthDataList.get(i).lengthCode);
        }
    }

    private List<HuffmanCodeLengthData> readHuffmanCodes(int elementsNumber, List<HuffmanCodeLengthData> huffmanCodeLengthDataList) {
        BitReader bitReader = new BitReader();
        int bitsNumber = 1;
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
                            int previousCodeValue = newHuffmanCodeLengthDataList.get(loadedElements - 1).lengthCode;
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 2, 3, previousCodeValue);
                        } else if (huffmanCodeLengthData.index == 17) {
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 3, 3, 0);
                        } else if (huffmanCodeLengthData.index == 18) {
                            loadedElements = repeatCodeLength(bitReader, loadedElements, newHuffmanCodeLengthDataList, 7, 11, 0);
                        }

                        bitsNumber = 0;
                        foundNext = true;
                        break;
                    }
                }
                bitsNumber++;
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
            if (huffmanCodeLengthData.lengthCode == 0)
                continue;
            if (huffmanCodeLengthData.bitsNumber < smallestHuffmanLength)
                smallestHuffmanLength = huffmanCodeLengthData.bitsNumber;
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

        biggestDistanceCodeLength = 18;
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
            huffmanLengthCodes.add(new HuffmanCodeLengthData(i, i, 8, 0b00110000 + i));
        for (int i = 0; i < 112; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(144 + i, 144 + i, 9, 0b110010000 + i));
        for (int i = 0; i < 24; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(256 + i, 256 + i, 7, i));
        for (int i = 0; i < 8; i++)
                    huffmanLengthCodes.add(new HuffmanCodeLengthData(280 + i, 280 + i, 8, 0b11000000 + i));
        smallestHuffmanLength = 7;
    }
}
