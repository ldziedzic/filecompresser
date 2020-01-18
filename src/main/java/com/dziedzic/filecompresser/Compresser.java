package com.dziedzic.filecompresser;

/*
  @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compresser {
    public byte[] readFile(String path)  {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
