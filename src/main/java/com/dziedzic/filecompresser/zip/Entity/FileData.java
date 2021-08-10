package com.dziedzic.filecompresser.zip.Entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 11.04.2020
 */

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class FileData {
    private int offset;
    private int fileDataSize;
    private boolean isZip;
    private List<Flag> flags;
    private CompressionMethod compresionMethod;
    private LocalDateTime modificationDateTime;
    private int crc32Checksum;
    private int compressedSize;
    private int uncompressedSize;
    private short fileNameLength;
    private short extraFieldLength;
    private String filename;
    private String extraFields;
    private int fileHeaderSize;

    public FileData() {
    }

    public FileData(int offset, int fileDataSize, boolean isZip, List<Flag> flags, CompressionMethod compresionMethod,
                    LocalDateTime modificationDateTime, int crc32Checksum, int compressedSize, int uncompressedSize,
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

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getFileDataSize() {
        return fileDataSize;
    }

    public void setFileDataSize(int fileDataSize) {
        this.fileDataSize = fileDataSize;
    }

    public boolean isZip() {
        return isZip;
    }

    public void setZip(boolean zip) {
        isZip = zip;
    }

    public List<Flag> getFlags() {
        return flags;
    }

    public void setFlags(List<Flag> flags) {
        this.flags = flags;
    }

    public CompressionMethod getCompresionMethod() {
        return compresionMethod;
    }

    public void setCompresionMethod(CompressionMethod compresionMethod) {
        this.compresionMethod = compresionMethod;
    }

    public LocalDateTime getModificationDateTime() {
        return modificationDateTime;
    }

    public void setModificationDateTime(FileTime modificationDateTime) {
        this.modificationDateTime =  LocalDateTime.ofInstant( modificationDateTime.toInstant(), ZoneId.systemDefault());
    }

    public int getCrc32Checksum() {
        return crc32Checksum;
    }

    public void setCrc32Checksum(int crc32Checksum) {
        this.crc32Checksum = crc32Checksum;
    }

    public int getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(int compressedSize) {
        this.compressedSize = compressedSize;
    }

    public int getUncompressedSize() {
        return uncompressedSize;
    }

    public void setUncompressedSize(int uncompressedSize) {
        this.uncompressedSize = uncompressedSize;
    }

    public short getFileNameLength() {
        return fileNameLength;
    }

    public void setFileNameLength(short fileNameLength) {
        this.fileNameLength = fileNameLength;
    }

    public short getExtraFieldLength() {
        return extraFieldLength;
    }

    public void setExtraFieldLength(short extraFieldLength) {
        this.extraFieldLength = extraFieldLength;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(String extraFields) {
        this.extraFields = extraFields;
    }

    public int getFileHeaderSize() {
        return fileHeaderSize;
    }

    public void setFileHeaderSize(int fileHeaderSize) {
        this.fileHeaderSize = fileHeaderSize;
    }
}
