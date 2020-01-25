package com.dziedzic.filecompresser.zip.Entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 20.01.2020
 */

import java.util.Arrays;
import java.util.Optional;

public enum CompressionMethod {
    NO_COMPRESSION(new byte[]{0x0, 0x0}),
    SHRUNK(new byte[]{0x1, 0x0}),
    REDUCED_WITH_COMPRESION_FACTOR_1(new byte[]{0x2, 0x0}),
    REDUCED_WITH_COMPRESION_FACTOR_2(new byte[]{0x3, 0x0}),
    REDUCED_WITH_COMPRESION_FACTOR_3(new byte[]{0x4, 0x0}),
    REDUCED_WITH_COMPRESION_FACTOR_4(new byte[]{0x5, 0x0}),
    IMPLODED(new byte[]{0x6, 0x0}),
    DEFLATED(new byte[]{0x8, 0x0}),
    ENHANCED_DEFLATED(new byte[]{0x9, 0x0}),

    PKWARE_DCL_IMPLODED(new byte[]{0x0, 0x1}),
    BZIP2(new byte[]{0x2, 0x1}),
    LZMA(new byte[]{0x4, 0x1}),
    IBM_TERSE(new byte[]{0x8, 0x1}),
    IBM_LZ77_Z(new byte[]{0x9, 0x1}),
    PPMD(new byte[]{0x8, 0x1});

    private final byte[] compressionMethodCode;

    CompressionMethod(byte[] compressionMethodCode) {
        this.compressionMethodCode = compressionMethodCode;
    }

    public static Optional<CompressionMethod> valueOf(byte[] code) {
        return Arrays.stream(values())
                .filter(compressionMethod -> Arrays.equals(code, compressionMethod.compressionMethodCode))
                .findFirst();
    }
}
