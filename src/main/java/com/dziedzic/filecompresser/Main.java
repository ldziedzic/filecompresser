package com.dziedzic.filecompresser;

import com.dziedzic.filecompresser.zip.Zip;
import com.dziedzic.filecompresser.zip.ZipCompresser;

/*
 * @author Łukasz Dziedzic
 * @project filecompresser
 * @date 18.01.2020
 */
public class Main {
    public static void main(String [] args) {
        Zip zip = new Zip();
        zip.start(args[0], args[1]);
    }
}
