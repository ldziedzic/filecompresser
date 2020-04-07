package com.dziedzic.filecompresser.zip;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ZipCompresser {

    public void getFilesFromZip(String path) {
        byte[] content = readFile(path);
        ZipHeaderUtils zipHeaderUtils = new ZipHeaderUtils();
        zipHeaderUtils.getLocalFileHeader(content);


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
