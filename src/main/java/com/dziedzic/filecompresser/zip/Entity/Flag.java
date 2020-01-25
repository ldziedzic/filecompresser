package com.dziedzic.filecompresser.zip.Entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 24.01.2020
 */

import java.util.Arrays;
import java.util.Optional;

public enum Flag {
    ENCRYPTED_FILE(new byte[]{0x0, 0x0}),
    COMPRESSION_OPTION_1(new byte[]{0x1, 0x0}),
    COMPRESSION_OPTION_2(new byte[]{0x2, 0x0}),
    DATA_DESCRIPTOR(new byte[]{0x3, 0x0}),
    ENHANCED_DEFLATION(new byte[]{0x4, 0x0}),
    COMPRESSED_PATCHED_DATA(new byte[]{0x5, 0x0}),
    STRONG_ENCRYPTOR(new byte[]{0x6, 0x0}),
    LANGUAGE_ENCODING(new byte[]{0x1, 0x1}),
    RESERVED(new byte[]{0x2, 0x1}),
    MASK_HEADER_VALUES(new byte[]{0x3, 0x1});

    private final byte[] flag;

    Flag(byte[] flag) {
        this.flag = flag;
    }

    public static Optional<Flag> valueOf(byte[] code) {
        return Arrays.stream(values())
                .filter(flag -> Arrays.equals(code, flag.flag))
                .findFirst();
    }
}
