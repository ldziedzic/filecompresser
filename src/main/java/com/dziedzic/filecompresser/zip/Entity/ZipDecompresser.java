package com.dziedzic.filecompresser.zip.Entity;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 01.08.2021
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ZipDecompresser implements Runnable {
    private Thread t;
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
        Deflater deflater = new Deflater();
        byte [] output = deflater.decompress(content, fileData.getUncompressedSize());
        writeFile(fileData, path, output);
    }

    public void start () {
        if (t == null) {
            t = new Thread (this, "Decompressing " + fileData.getFilename());
            t.start ();
        }
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
}
