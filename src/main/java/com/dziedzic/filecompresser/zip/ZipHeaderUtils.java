package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 07.04.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.common.BitReader;
import com.dziedzic.filecompresser.zip.Entity.CompressionMethod;
import com.dziedzic.filecompresser.zip.Entity.FileData;
import com.dziedzic.filecompresser.zip.Entity.Flag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ZipHeaderUtils {
    private static final int FILE_HEADER_SIZE = 30;
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
    private static final byte[] CENTRAL_DIRECTORY_SIGNATURE = {0x50, 0x4b, 0x01, 0x02};

    public FileData getLocalFileHeader(byte[] content, int offset) {
        boolean isZip = checkLocalFileHeaderSignature(Arrays.copyOfRange(content, offset, offset + 4));
        Flag flag = getFlag(Arrays.copyOfRange(content, offset + 6, offset + 8));
        List<Flag> flags = new ArrayList<>();
        CompressionMethod compresionMethod = getCompressionMethod(Arrays.copyOfRange(content, offset + 8,
                offset + 10));
        LocalDateTime modificationDateTime = getModificationDateTime(Arrays.copyOfRange(content, offset + 10, offset + 14));
        int crc32Checksum = getCrc32Checksum(Arrays.copyOfRange(content, offset + 14, offset + 18));
        int compressedSize = getFileSize(Arrays.copyOfRange(content, offset + 18, offset + 22));
        int uncompressedSize = getFileSize(Arrays.copyOfRange(content, offset + 22, offset + 26));
        short fileNameLength = getFilenameLength(Arrays.copyOfRange(content, offset + 26, offset + 28));
        short extraFieldLength = getExtraFieldLength(Arrays.copyOfRange(content, offset + 28, offset + 30));
        String filename = getFilename(Arrays.copyOfRange(content, offset + 30, offset + 30 + fileNameLength));
        String extraFields = "";
        // String extraFields = getFilename(Arrays.copyOfRange(content, offset + 30 + fileNameLength,
        // offset + 30 + fileNameLength + extraFieldLength));
        int fileHeaderSize = FILE_HEADER_SIZE + fileNameLength + extraFieldLength;

        int fileDataSize = FILE_HEADER_SIZE + fileNameLength + extraFieldLength + compressedSize;

        return new FileData(offset, fileDataSize, isZip, flags, compresionMethod,  modificationDateTime, crc32Checksum,
                compressedSize, uncompressedSize, fileNameLength, extraFieldLength, filename, extraFields,
                fileHeaderSize);
    }

    public boolean checkLocalFileHeaderSignature(byte[] localFileHeaderSignature) {
        return Arrays.equals(ZIP_SIGNATURE, localFileHeaderSignature);
    }

    public boolean checkCentralDirectoryHeaderSignature(byte[] localFileHeaderSignature) {
        return Arrays.equals(CENTRAL_DIRECTORY_SIGNATURE, localFileHeaderSignature);
    }


    int setZipSignature(byte[] output, int offset) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 32, 67324752);
        offset += 32;
        return offset;
    }

    int setVersion(byte[] output, int offset) {
        offset += 16;
        return offset;
    }

    int setFlags(byte[] output, int offset) {
        offset += 16;
        return offset;
    }

    int setDeflateCompressionMethod(byte[] output, int offset) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 16, 8);
        offset += 16;
        return offset;
    }

    int setModificationDateTime(byte[] output, int offset, LocalDateTime time) {
        /*
        File modification time	stored in standard MS-DOS format:
        Bits 00-04: seconds divided by 2
        Bits 05-10: minute
        Bits 11-15: hour
        File modification date	stored in standard MS-DOS format:
        Bits 00-04: day
        Bits 05-08: month
        Bits 09-15: years from 1980
         */
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 5, time.getSecond() / 2);
        offset += 5;
        bitReader.setBitsLittleEndian(output, offset, 5, time.getMinute());
        offset += 6;
        bitReader.setBitsLittleEndian(output, offset, 5, time.getHour());
        offset += 5;

        bitReader.setBitsLittleEndian(output, offset, 5, time.getDayOfMonth());
        offset += 5;
        bitReader.setBitsLittleEndian(output, offset, 5, time.getMonthValue());
        offset += 4;
        bitReader.setBitsLittleEndian(output, offset, 5, time.getYear() - 1980);
        offset += 7;

        return offset;
    }

    int setCRC32Checksum(byte[] output, int offset, int checksum) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 32, checksum);
        offset += 32;
        return offset;
    }

    int setCompressedSize(byte[] output, int offset, int compressedSize) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 32, compressedSize);
        offset += 32;
        return offset;
    }

    int setUncompressedSize(byte[] output, int offset, int uncompressedSize) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 32, uncompressedSize);
        offset += 32;
        return offset;
    }

    int setFilenameLen(byte[] output, int offset, int filenameLen) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 16, filenameLen);
        offset += 16;
        return offset;
    }

    int setExtraFieldsLen(byte[] output, int offset, int extraFieldsLen) {
        BitReader bitReader = new BitReader();
        bitReader.setBitsLittleEndian(output, offset, 16, extraFieldsLen);
        offset += 16;
        return offset;
    }


    int setFilename(byte[] output, int offset, String filename, int filenameLen) {
        ByteBuffer buffer = ByteBuffer.allocate(filenameLen);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(filename.getBytes());
        buffer.rewind();

        byte[] bytes = buffer.array();
        for (byte element: bytes) {
            output[offset / 8] = element;
            offset += 8;
        }

        offset += filenameLen * 8;
        return offset;
    }

    private Flag getFlag(byte[] flag) {
        Optional<Flag> flags = Flag.valueOf(flag);
        return flags.orElse(null);
    }

    private CompressionMethod getCompressionMethod(byte[] compressionMethodBytes) {
        Optional<CompressionMethod> compressionMethod = CompressionMethod.valueOf(compressionMethodBytes);
        return compressionMethod.orElse(null);
    }

    private LocalDateTime getModificationDateTime(byte[] datetime) {
        /*
        File modification time	stored in standard MS-DOS format:
        Bits 00-04: seconds divided by 2
        Bits 05-10: minute
        Bits 11-15: hour
        File modification date	stored in standard MS-DOS format:
        Bits 00-04: day
        Bits 05-08: month
        Bits 09-15: years from 1980
         */

        ByteBuffer buffer = ByteBuffer.allocate(datetime.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(datetime);
        buffer.rewind();

        int timeBits = buffer.getShort() & 0xffff;
        int seconds = (timeBits & 0x1f) * 2;
        int minutes = (timeBits & 0x7e0) >> 5;
        int hours = (timeBits & 0xf800) >> 11;

        int dateBits = buffer.getShort() & 0xffff;
        int day = (dateBits & 0x1f);
        int month = (dateBits & 0x1e0) >> 5;
        int year = ((dateBits & 0xfe00) >> 9) + 1980;

        return LocalDateTime.of(year, month, day, hours, minutes, seconds);
    }

    private int getCrc32Checksum(byte[] checksumBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(checksumBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(checksumBytes);
        buffer.rewind();

       return (int) (buffer.getInt() & 0xFFFFFFFFL);
    }

    private int getFileSize(byte[] fileSizeBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(fileSizeBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(fileSizeBytes);
        buffer.rewind();

       return (int) (buffer.getInt() & 0xFFFFFFFFL);
    }

    private short getFilenameLength(byte[] filenameLengthBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(filenameLengthBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(filenameLengthBytes);
        buffer.rewind();

       return buffer.getShort();
    }

    private short getExtraFieldLength(byte[] fileSizeBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(fileSizeBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(fileSizeBytes);
        buffer.rewind();

       return buffer.getShort();
    }

    private String getFilename(byte[] fileSizeBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(fileSizeBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(fileSizeBytes);
        buffer.rewind();

       return StandardCharsets.UTF_8.decode(buffer).toString();
    }
}
