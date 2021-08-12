package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 07.08.2021
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Zip {
    public void start(String compressionType, String path) {
        switch (compressionType) {
            case "compress":
                compressDirectory(path);
                break;
            case "decompress":
                readZipFile(path);
                break;
            default:
                System.out.println("First argument should be set to compress or decompress.");
                break;
        }
    }

    private void compressDirectory(String path) {
        Path dirPath = Paths.get(path);
        List<Path> paths = readFileListToCompress(path);
        byte[][] output = new byte[paths.size()][];
        int fileIndex = 0;
        int outputSize = 0;
        for (Path filePath: paths) {

            FileData fileData = getFileAttributes(dirPath, filePath);

            byte[] content = readFile(filePath.toString());

            Deflater deflater = new Deflater();
            byte [] compressedContent = deflater.compress(content);
            fileData.setCompressedSize(compressedContent.length);

            byte[] header = generateFileHeader(fileData, compressedContent);

            output[fileIndex] = new byte[header.length + compressedContent.length];
            System.arraycopy(header, 0, output[fileIndex], 0, header.length);
            System.arraycopy(compressedContent, 0, output[fileIndex], header.length, compressedContent.length);
            System.out.println("Successfully compressed " + fileData.getFilename());
            outputSize += header.length + compressedContent.length;
            fileIndex++;
        }
        byte[] outputFile = new byte[outputSize];
        int outputPosition = 0;
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < output[i].length; j++) {
                outputFile[outputPosition] = output[i][j];
                outputPosition++;
            }
        }
        writeFile(path + "2.zip", outputFile);
    }

    private byte[] generateFileHeader(FileData fileData, byte[] compressedContent) {
        byte[] header = new byte[30 + fileData.getFilename().length() + fileData.getExtraFieldLength()];
        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        int offset = 0;
        offset = zipHeaderUtils.setZipSignature(header, offset);
        offset = zipHeaderUtils.setVersion(header, offset);
        offset = zipHeaderUtils.setFlags(header, offset);
        offset = zipHeaderUtils.setDeflateCompressionMethod(header, offset);
        offset = zipHeaderUtils.setModificationDateTime(header, offset, fileData.getModificationDateTime());
        offset = zipHeaderUtils.setCRC32Checksum(header, offset, fileData.getCrc32Checksum());
        offset = zipHeaderUtils.setCompressedSize(header, offset, compressedContent.length);
        offset = zipHeaderUtils.setUncompressedSize(header, offset, fileData.getUncompressedSize());
        offset = zipHeaderUtils.setFilenameLen(header, offset, fileData.getFileNameLength());
        offset = zipHeaderUtils.setExtraFieldsLen(header, offset, fileData.getExtraFieldLength());
        zipHeaderUtils.setFilename(header, offset, fileData.getFilename(), fileData.getFileNameLength());
        return header;
    }

    private FileData getFileAttributes(Path dirPath, Path filePath) {
        FileData fileData = new FileData();
        try {
            BasicFileAttributes attr =
                    Files.readAttributes(filePath, BasicFileAttributes.class);
            fileData.setModificationDateTime(attr.lastModifiedTime());
            fileData.setUncompressedSize((int) attr.size());
            fileData.setFilename(dirPath.relativize(filePath).toString());
            fileData.setFileNameLength((short) dirPath.relativize(filePath).toString().length());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileData;
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
                                offset + fileData.getFileHeaderSize() + fileData.getCompressedSize()), path));
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


    private List<Path> readFileListToCompress(String path) {
        List<Path> fileList = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(fileList::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
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


    private void writeFile(String path, byte[] output) {
        try {
            Files.write(Paths.get(path), output);
            System.out.println("Successfully compressed " + path);
        } catch (IOException e) {
            System.out.println("Failed to compress " + path);
            e.printStackTrace();
        }
    }
}