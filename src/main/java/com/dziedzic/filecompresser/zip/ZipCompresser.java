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
            if (zipHeaderUtils.checkLocalFileHeaderSignature(Arrays.copyOfRange(content, offset, offset + 4))) {
                FileData fileData = zipHeaderUtils.getLocalFileHeader(content, offset);
                decompressFile(fileData,
                        Arrays.copyOfRange(content,
                                offset + fileData.getFileHeaderSize(),
                                (int) (offset + fileData.getFileHeaderSize() + fileData.getCompressedSize())), path);
                offset += fileData.getFileDataSize();
            }
            else if (zipHeaderUtils.checkCentralDirectoryHeaderSignature(Arrays.copyOfRange(content, offset, offset + 4))) {
                System.out.println("All files decompressed");
                // TODO Get directory information
                break;
            }
        }
    }

    private void decompressFile(FileData fileData, byte[] content, String path) {
        switch (fileData.getCompresionMethod()) {
            case DEFLATED:
                decompressDataUsingDeflate(fileData, content, path);
                break;
            case NO_COMPRESSION:
                if (!fileData.getFilename().endsWith("/"))
                    writeFile(fileData, path, content);
            default:
                break;
        }
    }

    private void decompressDataUsingDeflate(FileData fileData, byte[] content, String path) {
        System.out.println("Started decompressing " + fileData.getFilename());
        Deflater deflater = new Deflater();
        byte [] output = deflater.decompress(content, fileData.getUncompressedSize());



//        output = deflater.compress(output);
//        int compressionLength = output.length;
//        output = deflater.decompress(output, fileData.getUncompressedSize());
//
//        System.out.println("------------------------------------------------------------");
//        System.out.println("------------------------------------------------------------");
//        System.out.println(output.length + " " + compressionLength);
//        System.out.println("------------------------------------------------------------");
        writeFile(fileData, path, output);
    }

    private void writeFile(FileData fileData, String path, byte[] output) {
        try {
            String directoryPath = FilenameUtils.removeExtension(path);
            File f = new File(String.valueOf(Paths.get(directoryPath, fileData.getFilename())));

            boolean isDirectoryCreated = f.getParentFile().mkdirs();

            if (isDirectoryCreated)
                System.out.println("Created directory " + directoryPath);
            Files.write(Paths.get(directoryPath, fileData.getFilename()), output);
            System.out.println("Successfully decompressed " + fileData.getFilename());
            System.out.println("------------------------------------------------------------");
            System.out.println("------------------------------------------------------------");
            System.out.println("------------------------------------------------------------");
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
