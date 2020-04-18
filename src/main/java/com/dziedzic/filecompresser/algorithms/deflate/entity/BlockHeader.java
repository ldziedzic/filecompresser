package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 12.04.2020
 */

public class BlockHeader {
    private boolean isLastBlock;
    private CompressionType compressionType;

    public BlockHeader(boolean isLastBlock, CompressionType compressionType) {
        this.isLastBlock = isLastBlock;
        this.compressionType = compressionType;
    }

    public boolean isLastBlock() {
        return isLastBlock;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }
}
