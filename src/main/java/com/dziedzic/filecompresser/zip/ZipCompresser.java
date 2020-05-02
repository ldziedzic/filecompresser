package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.FileData;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
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
                            (int) (offset + fileData.getFileHeaderSize() + fileData.getCompressedSize())), path);

            offset += fileData.getFileDataSize();
        }

    }

    private void decompressFile(FileData fileData, byte[] content, String path) {
        switch (fileData.getCompresionMethod()) {
            case DEFLATED:
                decompressDataUsingDeflate(fileData, content, path);
                break;
            default:
                break;
        }
    }

    private void decompressDataUsingDeflate(FileData fileData, byte[] content, String path) {
        Deflater deflater = new Deflater();
        byte [] output = deflater.deflate(content, fileData.getUncompressedSize());
        try {
            String directoryPath = FilenameUtils.removeExtension(path);
            File f = new File(directoryPath);

            boolean isDirectoryCreated = f.mkdirs();
            if (isDirectoryCreated)
                System.out.println("Created directory " + directoryPath);
            Files.write(Paths.get(directoryPath, fileData.getFilename()), output);
            System.out.println("Successfully decompressed " + fileData.getFilename());
        } catch (IOException e) {
            System.out.println("Failed to decompress " + fileData.getFilename());
            e.printStackTrace();
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
