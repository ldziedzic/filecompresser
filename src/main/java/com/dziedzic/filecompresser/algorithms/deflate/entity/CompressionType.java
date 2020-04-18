package com.dziedzic.filecompresser.algorithms.deflate.entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 12.04.2020
 */

import java.util.Arrays;
import java.util.Optional;

public enum CompressionType {
    NO_COMPRESSION(0),
    COMPRESSED_WITH_FIXED_HUFFMAN_CODES(0b1),
    COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES(0b10),
    ERROR(0b11);


    private final int compressionTypeCode;

    CompressionType(int compressionTypeCode) {
        this.compressionTypeCode = compressionTypeCode;
    }

    public static Optional<CompressionType> valueOf(int code) {
        return Arrays.stream(values())
                .filter(compressionType -> code == compressionType.compressionTypeCode)
                .findFirst();
    }
}
