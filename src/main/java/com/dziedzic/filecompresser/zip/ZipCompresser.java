package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
import com.dziedzic.filecompresser.zip.Entity.CompressionMethod;
import com.dziedzic.filecompresser.zip.Entity.Flag;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;

public class ZipCompresser {
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};

    public void getFilesFromZip(String path) {
        byte[] content = readFile(path);
        getLocalFileHeader(content);


    }


    private void getLocalFileHeader(byte[] content) {
        boolean isZip = checkLocalFileHeaderSignature(Arrays.copyOfRange(content, 0, 4));
        Flag flag = getFlag(Arrays.copyOfRange(content, 6, 8));
        CompressionMethod compresionMethod = getCompressionMethod(Arrays.copyOfRange(content, 8, 10));
        LocalDateTime modificationDateTime = getModificationDateTime(Arrays.copyOfRange(content, 10, 14));
        int crc32Checksum = getCrc32Checksum(Arrays.copyOfRange(content, 14, 18));

        return;
    }


    private boolean checkLocalFileHeaderSignature(byte[] localFileHeaderSignature) {
        return Arrays.equals(ZIP_SIGNATURE, localFileHeaderSignature);
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

       return buffer.getInt();
    }



    private byte[] readFile(String path)  {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
