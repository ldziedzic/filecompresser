package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 18.04.2020
 */

import java.util.ArrayList;
import java.util.List;

public class CodeTreesRepresentation {
    public final int MIN_DISTANCE_CODE_LENGTH = 5;
    private List<DistanceCode> distanceCodes;
    private List<LengthCode> lengthCodes;
    private List<HuffmanLengthCode> huffmanLengthCodes;
    private int smallestHuffmanLength;
    private int biggestDistanceCodeLength;
    private byte[] blockContent;
    private BlockHeader blockHeader;

    public CodeTreesRepresentation(byte[] blockContent, BlockHeader blockHeader) {
        this.blockContent = blockContent;
        this.blockHeader = blockHeader;
        smallestHuffmanLength = 0;
        distanceCodes = new ArrayList<>();
        lengthCodes = new ArrayList<>();
        huffmanLengthCodes = new ArrayList<>();

    }

    public void generateCodeTreesRepresentation() {
        if (blockHeader.getCompressionType() == CompressionType.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES)
            generateDynamicCodeTreesRepresentation();
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

    public List<HuffmanLengthCode> getHuffmanLengthCodes() {
        return huffmanLengthCodes;
    }

    private void generateDynamicCodeTreesRepresentation() {

    }

    private void generateStaticCodeTreesRepresentation() {
        generateStaticDistanceCodes();
        generateStaticLengthCodes();
        generateStaticHuffmanLengthCodes();
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

    private void generateStaticDistanceCodes() {
        distanceCodes.add(new DistanceCode(	0,0,1));
        distanceCodes.add(new DistanceCode(	1,0,2));
        distanceCodes.add(new DistanceCode(	2,0,3));
        distanceCodes.add(new DistanceCode(	3,0,4));
        distanceCodes.add(new DistanceCode(	4,1,5));
        distanceCodes.add(new DistanceCode(	5,1,7));
        distanceCodes.add(new DistanceCode(	6,2,9));
        distanceCodes.add(new DistanceCode(	7,2,13));
        distanceCodes.add(new DistanceCode(	8,3,17));
        distanceCodes.add(new DistanceCode(	9,3,25));
        distanceCodes.add(new DistanceCode(	10,4,33));
        distanceCodes.add(new DistanceCode(	11,4,49));
        distanceCodes.add(new DistanceCode(	12,5,65));
        distanceCodes.add(new DistanceCode(	13,5,97));
        distanceCodes.add(new DistanceCode(	14,6,129));
        distanceCodes.add(new DistanceCode(	15,6,193));
        distanceCodes.add(new DistanceCode(	16,7,257));
        distanceCodes.add(new DistanceCode(	17,7,385));
        distanceCodes.add(new DistanceCode(	18,8,513));
        distanceCodes.add(new DistanceCode(	19,8,769));
        distanceCodes.add(new DistanceCode(	20,9,1025));
        distanceCodes.add(new DistanceCode(	21,9,1537));
        distanceCodes.add(new DistanceCode(	22,10,2049));
        distanceCodes.add(new DistanceCode(	23,10,3073));
        distanceCodes.add(new DistanceCode(	24,11,4097));
        distanceCodes.add(new DistanceCode(	25,11,6145));
        distanceCodes.add(new DistanceCode(	26,12,8193));
        distanceCodes.add(new DistanceCode(	27,12,12289));
        distanceCodes.add(new DistanceCode(	28,13,16385));
        distanceCodes.add(new DistanceCode(	29,13,24577));

        biggestDistanceCodeLength = 18;
    }

    private void generateStaticHuffmanLengthCodes() {
        for (int i = 0; i < 144; i++)
            huffmanLengthCodes.add(new HuffmanLengthCode(i, 8, 0b00110000 + i));
        for (int i = 0; i < 112; i++)
                    huffmanLengthCodes.add(new HuffmanLengthCode(144 + i, 9, 0b110010000 + i));
        for (int i = 0; i < 24; i++)
                    huffmanLengthCodes.add(new HuffmanLengthCode(256 + i, 7, i));
        for (int i = 0; i < 8; i++)
                    huffmanLengthCodes.add(new HuffmanLengthCode(280 + i, 8, 0b11000000 + i));
        smallestHuffmanLength = 7;
    }
}
