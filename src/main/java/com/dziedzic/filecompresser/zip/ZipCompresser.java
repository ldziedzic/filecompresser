package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
import com.dziedzic.filecompresser.zip.Entity.CompressionMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class ZipCompresser {
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};

    public void getFilesFromZip(String path) {
        byte[] content = readFile(path);
        getLocalFileHeader(content);


    }


    private void getLocalFileHeader(byte[] content) {
        boolean isZip = checkLocalFileHeaderSignature(Arrays.copyOfRange(content, 0, 4));
        CompressionMethod compresionMethod = getCompressionMethod(Arrays.copyOfRange(content, 7, 9));

        return;
    }


    private boolean checkLocalFileHeaderSignature(byte[] localFileHeaderSignature) {
        return Arrays.equals(ZIP_SIGNATURE, localFileHeaderSignature);
    }

    private CompressionMethod getCompressionMethod(byte[] compressionMethodBytes) {
        Optional<CompressionMethod> compressionMethod = CompressionMethod.valueOf(compressionMethodBytes);
        return compressionMethod.orElse(null);
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
