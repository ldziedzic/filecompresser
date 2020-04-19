package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.BlockHeader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CompressionType;

import java.util.Arrays;

public class Deflater {

    public void deflate(byte[] content) {
        BitReader bitReader = new BitReader();

        BlockHeader blockHeader = readBlockHeader(bitReader.getBits(content, 0, 3)[0]);

        byte[] a = bitReader.getBits(content, 0, 3);
        byte[] b = bitReader.getBits(content, 3, 15);
        byte[] c = bitReader.getBits(content, 11, 8);
        byte[] d = bitReader.getBits(content, 19, 7);
        return;
    }

    private BlockHeader readBlockHeader(byte content) {
        int isLastBlock = (content & 0x80) >> 7;
        CompressionType compressionType = CompressionType.valueOf((content & 0x60)  >> 5).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
