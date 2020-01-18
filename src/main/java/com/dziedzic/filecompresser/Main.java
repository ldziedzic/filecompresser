package com.dziedzic.filecompresser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
public class Main {
    public static void main(String [] args) {
        Compresser compresser = new Compresser();
        compresser.readFile("/home/lukasz/projects/file-compression-python/file-compression-python.zip");
    }
}
