package com.dziedzic.filecompresser.zip;/*
 * @project filecompresser
 * @author Łukasz Dziedzic
 * @date 07.08.2021
 */

import com.dziedzic.filecompresser.algorithms.deflate.Deflater;
import com.dziedzic.filecompresser.zip.Entity.CompressionOutput;
import com.dziedzic.filecompresser.zip.Entity.FileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public class Zip {
    public void start(String compressionType, String path, String huffmanCodesMode, int maxBlockSize) {
        switch (compressionType) {
            case "compress":
                compressDirectory(path, huffmanCodesMode, maxBlockSize);
                break;
            case "decompress":
                readZipFile(path);
                break;
            default:
                System.out.println("First argument should be set to compress or decompress.");
                break;
        }
    }

    private void compressDirectory(String path, String huffmanCodesMode, int maxBlockSize) {
        Path dirPath = Paths.get(path);
        List<Path> paths = readFileListToCompress(path);
        byte[][] output = new byte[paths.size()][];
        int fileIndex = 0;
        int outputSize = 0;
        FileData[] fileData = new FileData[paths.size()];

        File fileToRemove = new File(path + ".zip.tmp");
        fileToRemove.delete();
        fileToRemove = new File(path + "2.zip");
        fileToRemove.delete();


        for (Path filePath: paths) {

            fileData[fileIndex] = getFileAttributes(dirPath, filePath);

            byte[] content = readFile(filePath.toString());
            CRC32 crc32 = generateCRC32Checksum(content);
            fileData[fileIndex].setCrc32Checksum((int) crc32.getValue());

            int maxBytesToProcess = 1 * 1024 * 1024; // 10 MB
            int blocksNumber = content.length / maxBytesToProcess;
            if (content.length % maxBytesToProcess != 0)
                blocksNumber++;
            int compressedSize = 0;

            Path temp = null;
            try {
                temp = Files.createTempFile(filePath.getFileName().toString(), ".tmp");
            }catch (IOException e) {

            }

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

            byte[] compressedContent = readFile(temp.toString());
            compressedSize = compressedContent.length;
            fileData[fileIndex].setCompressedSize(compressedSize);

            byte[] header = generateFileHeader(fileData[fileIndex]);
            writeFile(path + ".zip.tmp", header);

            writeFile(path + ".zip.tmp", compressedContent);
            fileToRemove = new File(temp.toString());
            fileToRemove.delete();

            output[fileIndex] = new byte[header.length + compressedSize];
            System.arraycopy(header, 0, output[fileIndex], 0, header.length);
            System.arraycopy(compressedContent, 0, output[fileIndex], header.length, compressedSize);
            System.out.println("Successfully compressed " + fileData[fileIndex].getFilename());
            fileData[fileIndex].setOffset(outputSize);
            outputSize += header.length + compressedSize;
            fileIndex++;
        }

        byte[] outputFile = addCentralDirectoryHeaders(paths, output, outputSize, fileData);

        writeFile(path + "2.zip", outputFile);
    }

    private byte[] addCentralDirectoryHeaders(List<Path> paths, byte[][] output, int outputSize, FileData[] fileData) {
        int fileIndex;
        fileIndex = 0;
        byte[][] centralDirectoryHeaders = new byte[paths.size()][];
        int centralDirectoryHeaderSize = 0;
        for (int i = 0; i < paths.size(); i++) {
            centralDirectoryHeaders[fileIndex] = generateCentralDirectoryFileHeader(fileData[fileIndex]);
            centralDirectoryHeaderSize += centralDirectoryHeaders[fileIndex].length;
            fileIndex++;
        }

        int endOfCentralDirectoryHeaderSize = 22;
        byte[] outputFile = new byte[outputSize + centralDirectoryHeaderSize + endOfCentralDirectoryHeaderSize];
        long outputPosition = 0;
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < output[i].length; j++) {
                outputFile[(int) outputPosition] = output[i][j];
                outputPosition++;
            }
        }
        int centralDirectoryStartPosition = (int) outputPosition;
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < centralDirectoryHeaders[i].length; j++) {
                outputFile[(int) outputPosition] = centralDirectoryHeaders[i][j];
                outputPosition++;
            }
        }

        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();

        zipHeaderUtils.setEndOfCentralDirectory(outputFile, outputPosition * 8, paths.size(),
                centralDirectoryHeaderSize, centralDirectoryStartPosition);
        return outputFile;
    }

    private CRC32 generateCRC32Checksum(byte[] content) {
        // CRC32 from uncompressed data
        CRC32 crc = new CRC32();
        crc.update(content, 0, content.length);
        return crc;

    }

    private byte[] generateFileHeader(FileData fileData) {
        byte[] header = new byte[30 + fileData.getFilename().length() + fileData.getExtraFieldLength()];
        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        int offset = 0;
        offset = zipHeaderUtils.setZipSignature(header, offset);
        offset = zipHeaderUtils.setPKZIPVersion(header, offset);
        offset = zipHeaderUtils.setFlags(header, offset);
        offset = zipHeaderUtils.setDeflateCompressionMethod(header, offset);
        offset = zipHeaderUtils.setModificationDateTime(header, offset, fileData.getModificationDateTime());
        offset = zipHeaderUtils.setCRC32Checksum(header, offset, fileData.getCrc32Checksum());
        offset = zipHeaderUtils.setCompressedSize(header, offset, fileData.getCompressedSize());
        offset = zipHeaderUtils.setUncompressedSize(header, offset, fileData.getUncompressedSize());
        offset = zipHeaderUtils.setFilenameLen(header, offset, fileData.getFileNameLength());
        offset = zipHeaderUtils.setExtraFieldsLen(header, offset, fileData.getExtraFieldLength());
        zipHeaderUtils.setFilename(header, offset, fileData.getFilename(), fileData.getFileNameLength());
        return header;
    }

    private byte[] generateCentralDirectoryFileHeader(FileData fileData) {
        byte[] header = new byte[46 + fileData.getFilename().length() + fileData.getExtraFieldLength()];
        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        int offset = 0;
        offset = zipHeaderUtils.setFileCentralDirectorySignature(header, offset);
        offset = zipHeaderUtils.setVersion(header, offset);
        offset = zipHeaderUtils.setPKZIPVersion(header, offset);
        offset = zipHeaderUtils.setFlags(header, offset);
        offset = zipHeaderUtils.setDeflateCompressionMethod(header, offset);
        offset = zipHeaderUtils.setModificationDateTime(header, offset, fileData.getModificationDateTime());
        offset = zipHeaderUtils.setCRC32Checksum(header, offset, fileData.getCrc32Checksum());
        offset = zipHeaderUtils.setCompressedSize(header, offset, fileData.getCompressedSize());
        offset = zipHeaderUtils.setUncompressedSize(header, offset, fileData.getUncompressedSize());
        offset = zipHeaderUtils.setFilenameLen(header, offset, fileData.getFileNameLength());
        offset = zipHeaderUtils.setExtraFieldsLen(header, offset, fileData.getExtraFieldLength());
        offset = zipHeaderUtils.setExtraCommentsLen(header, offset, fileData.getExtraFieldLength());
        offset = zipHeaderUtils.setDiskStart(header, offset, 0);
        offset = zipHeaderUtils.setAdditionalAttributes(header, offset);
        offset = zipHeaderUtils.setFileOffset(header, offset, fileData.getOffset());
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
