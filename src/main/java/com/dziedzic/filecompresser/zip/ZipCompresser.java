package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.CompressionOutput;
import com.dziedzic.filecompresser.zip.Entity.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;


public class ZipCompresser  implements Runnable {
    private Thread thread;
    private FileData fileData;
    String huffmanCodesMode;
    int maxBlockSize;
    Path temp;
    byte[] content;

    public ZipCompresser(FileData fileData, String huffmanCodesMode, int maxBlockSize, Path temp, byte[] content) {
        this.fileData = fileData;
        this.huffmanCodesMode = huffmanCodesMode;
        this.maxBlockSize = maxBlockSize;
        this.temp = temp;
        this.content = content;
    }

    public void run() {
        System.out.println("Started compressing " + fileData.getFilename());
        compressFile(huffmanCodesMode, maxBlockSize, temp, content);

    }

    public Thread start () {
        if (thread == null) {
            thread = new Thread (this, fileData.getFilename());
            thread.start ();
        }
        return thread;
    }

    private void compressFile(String huffmanCodesMode, int maxBlockSize, Path temp, byte[] content) {
        int maxBytesToProcess = 10 * 1024 * 1024; // 10 MB
        int blocksNumber = content.length / maxBytesToProcess;
        if (content.length % maxBytesToProcess != 0)
            blocksNumber++;
        int compressedSize = 0;
        byte additionalByte = 0;
        int additionalBits = 0;

        int processedBytes = 0;
        for (int i = 0; i < blocksNumber; i++) {
            int blockSize = Math.min(maxBytesToProcess, content.length - processedBytes);
            boolean isLastDataSet = false;
            if (content.length - processedBytes - blockSize == 0)
                isLastDataSet = true;
            Deflater deflater = new Deflater();
            CompressionOutput compressionOutput = deflater.compress(Arrays.copyOfRange(content, i * maxBytesToProcess, i * maxBytesToProcess + blockSize),
                    additionalByte, additionalBits, isLastDataSet, huffmanCodesMode, maxBlockSize);

            additionalBits = compressionOutput.getAdditionalBits();
            additionalByte = compressionOutput.getContent()[compressionOutput.getContent().length-1];
            int endPosition = compressionOutput.getContent().length;
            if (additionalBits > 0 && !isLastDataSet)
                endPosition--;

            writeFile(Paths.get(String.valueOf(temp)).toString(), Arrays.copyOfRange(compressionOutput.getContent(), 0, endPosition));

            processedBytes += blockSize;
        }
    }


    private void writeFile(String path, byte[] output) {
        try {
            Files.write(Paths.get(path), output, StandardOpenOption.APPEND);
        } catch (IOException e) {
            try {
                Files.write(Paths.get(path), output);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
