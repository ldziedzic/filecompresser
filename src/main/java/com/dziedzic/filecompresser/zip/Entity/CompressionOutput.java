package com.dziedzic.filecompresser.zip.Entity;

public class CompressionOutput {
    byte[] content;
    int additionalBits;

    public CompressionOutput(byte[] content, int additionalBits) {
        this.content = content;
        this.additionalBits = additionalBits;
    }

    public byte[] getContent() {
        return content;
    }

    public int getAdditionalBits() {
        return additionalBits;
    }
}
