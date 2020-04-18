package com.dziedzic.filecompresser.zip.Entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import java.time.LocalDateTime;
import java.util.List;

public class FileData {
    private int offset;
    private int fileDataSize;
    private boolean isZip;
    private List<Flag> flags;
    private CompressionMethod compresionMethod;
    private LocalDateTime modificationDateTime;
    private Long crc32Checksum;
    private Long compressedSize;
    private Long uncompressedSize;
    private short fileNameLength;
    private short extraFieldLength;
    private String filename;
    private String extraFields;
    private int fileHeaderSize;


    public FileData(int offset, int fileDataSize, boolean isZip, List<Flag> flags, CompressionMethod compresionMethod,
                    LocalDateTime modificationDateTime, Long crc32Checksum, Long compressedSize, Long uncompressedSize,
                    short fileNameLength, short extraFieldLength, String filename, String extraFields,
                    int fileHeaderSize) {
        this.offset = offset;
        this.fileDataSize = fileDataSize;
        this.isZip = isZip;
        this.flags = flags;
        this.compresionMethod = compresionMethod;
        this.modificationDateTime = modificationDateTime;
        this.crc32Checksum = crc32Checksum;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.fileNameLength = fileNameLength;
        this.extraFieldLength = extraFieldLength;
        this.filename = filename;
        this.extraFields = extraFields;
        this.fileHeaderSize = fileHeaderSize;
    }

    public int getOffset() {
        return offset;
    }

    public int getFileDataSize() {
        return fileDataSize;
    }

    public boolean isZip() {
        return isZip;
    }

    public List<Flag> getFlags() {
        return flags;
    }

    public CompressionMethod getCompresionMethod() {
        return compresionMethod;
    }

    public LocalDateTime getModificationDateTime() {
        return modificationDateTime;
    }

    public Long getCrc32Checksum() {
        return crc32Checksum;
    }

    public Long getCompressedSize() {
        return compressedSize;
    }

    public Long getUncompressedSize() {
        return uncompressedSize;
    }

    public short getFileNameLength() {
        return fileNameLength;
    }

    public short getExtraFieldLength() {
        return extraFieldLength;
    }

    public String getFilename() {
        return filename;
    }

    public String getExtraFields() {
        return extraFields;
    }

    public int getFileHeaderSize() {
        return fileHeaderSize;
    }
}
