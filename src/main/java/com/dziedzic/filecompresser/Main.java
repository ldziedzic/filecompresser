package com.dziedzic.filecompresser;

import com.dziedzic.filecompresser.zip.ZipCompresser;

/*
 * @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
public class Main {
    public static void main(String [] args) {
        ZipCompresser zipCompresser = new ZipCompresser();
        zipCompresser.getFilesFromZip("/home/lukasz/Magisterka/test.zip");
//        zipCompresser.getFilesFromZip("/home/lukasz/Magisterka_kopia_11_12_2020.zip");
//        zipCompresser.getFilesFromZip("/home/lukasz/projects/file-compression-python/a9.zip");
    }
}
