package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class ZipCompresser {

    public void getFilesFromZip(String path) {
        byte[] content = readFile(path);
        int offset = 0;
        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        while (isNextFile(content, offset)) {
            FileData fileData = zipHeaderUtils.getLocalFileHeader(content, offset);

            decompressFile(fileData,
                    Arrays.copyOfRange(content,
                            offset + fileData.getFileHeaderSize(),
                            (int) (offset + fileData.getFileHeaderSize() + fileData.getCompressedSize())));

            offset += fileData.getFileDataSize();
        }

    }

    private void decompressFile(FileData fileData, byte[] content) {
        switch (fileData.getCompresionMethod()) {
            case DEFLATED:
                Deflater deflater = new Deflater();
                deflater.deflate(content, fileData.getUncompressedSize());
                break;
            default:
                break;
        }
    }


    private boolean isNextFile(byte[] content, int offset) {
        return content.length > offset;
    }


    private byte[] readFile(String path)  {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
