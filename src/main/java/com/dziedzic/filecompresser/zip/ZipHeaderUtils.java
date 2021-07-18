package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 07.04.2020
 */

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
        Long crc32Checksum = getCrc32Checksum(Arrays.copyOfRange(content, offset + 14, offset + 18));
        Long compressedSize = getFileSize(Arrays.copyOfRange(content, offset + 18, offset + 22));
        Long uncompressedSize = getFileSize(Arrays.copyOfRange(content, offset + 22, offset + 26));
        short fileNameLength = getFilenameLength(Arrays.copyOfRange(content, offset + 26, offset + 28));
        short extraFieldLength = getExtraFieldLength(Arrays.copyOfRange(content, offset + 28, offset + 30));
        String filename = getFilename(Arrays.copyOfRange(content, offset + 30, offset + 30 + fileNameLength));
        String extraFields = "";
        // String extraFields = getFilename(Arrays.copyOfRange(content, offset + 30 + fileNameLength,
        // offset + 30 + fileNameLength + extraFieldLength));
        int fileHeaderSize = FILE_HEADER_SIZE + fileNameLength + extraFieldLength;

        int fileDataSize = (int) (FILE_HEADER_SIZE + fileNameLength + extraFieldLength + compressedSize);

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

    private Long getCrc32Checksum(byte[] checksumBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(checksumBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(checksumBytes);
        buffer.rewind();

       return buffer.getInt() & 0xFFFFFFFFL;
    }

    private Long getFileSize(byte[] fileSizeBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(fileSizeBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(fileSizeBytes);
        buffer.rewind();

       return buffer.getInt() & 0xFFFFFFFFL;
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
