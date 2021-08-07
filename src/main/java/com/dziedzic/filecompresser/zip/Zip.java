package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 07.08.2021
 */

import com.dziedzic.filecompresser.zip.Entity.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Zip {
    public void start(String compressionType, String path) {
        switch (compressionType) {
            case "compress":
                readZipFile(path);
                break;
            case "decompress":
                readZipFile(path);
            default:
                System.out.println("First argument should be set to compress or decompress.");
                break;
        }
    }

    private void readZipFile(String path) {
        byte[] content = readFile(path);
        int offset = 0;

        ArrayList<Thread> threads = new ArrayList<>();

        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        while (isNextFile(content, offset)) {
            if (zipHeaderUtils.checkLocalFileHeaderSignature(Arrays.copyOfRange(content, offset, offset + 4))) {
                FileData fileData = zipHeaderUtils.getLocalFileHeader(content, offset);
                threads.add(decompressFile(fileData,
                        Arrays.copyOfRange(content,
                                offset + fileData.getFileHeaderSize(),
                                (int) (offset + fileData.getFileHeaderSize() + fileData.getCompressedSize())), path));
                offset += fileData.getFileDataSize();
            }
            else if (zipHeaderUtils.checkCentralDirectoryHeaderSignature(Arrays.copyOfRange(content, offset, offset + 4))) {
                for (Thread thread : threads) {
                    try {
                        if (thread != null)
                            thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("All files decompressed");
                // TODO Get directory information
                break;
            }
        }
    }

    private Thread decompressFile(FileData fileData, byte[] content, String path) {
        ZipDecompresser zipDecompresser = new ZipDecompresser(fileData, content, path);
        return zipDecompresser.start();
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
