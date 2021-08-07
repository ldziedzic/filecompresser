package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 01.08.2021
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.FileData;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ZipDecompresser implements Runnable {
    private Thread thread;
    private FileData fileData;
    private byte[] content;
    private String path;

    public ZipDecompresser(FileData fileData, byte[] content, String path) {
        this.fileData = fileData;
        this.content = content;
        this.path = path;
    }

    public void run() {
        System.out.println("Started decompressing " + fileData.getFilename());

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

    public Thread start () {
        if (thread == null) {
            thread = new Thread (this, "Decompressing " + fileData.getFilename());
            thread.start ();
        }
        return thread;
    }

    private void decompressDataUsingDeflate(FileData fileData, byte[] content, String path) {
        Deflater deflater = new Deflater();
        byte [] output = deflater.decompress(content, fileData.getUncompressedSize());
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
        } catch (IOException e) {
            System.out.println("Failed to decompress " + fileData.getFilename());
            e.printStackTrace();
        }
    }
}
