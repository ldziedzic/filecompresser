package com.dziedzic.filecompresser.algorithms.deflate;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.entity.BlockHeader;
import com.dziedzic.filecompresser.algorithms.deflate.entity.CompressionType;

public class Deflater {

    public void deflate(byte[] content) {
        BlockHeader blockHeader = readBlockHeader(content[0]);

        return;
    }

    private BlockHeader readBlockHeader(byte content) {
        int isLastBlock = content & 0x1;
        CompressionType compressionType = CompressionType.valueOf((content & 0x06)  >> 1).orElse(null);

        return new BlockHeader(isLastBlock > 0, compressionType);
    }
}
