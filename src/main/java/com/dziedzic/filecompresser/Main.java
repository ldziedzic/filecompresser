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
        if (args.length < 2) {
            System.out.println("Error. Not enough arguments.");
            return;
        }
        if (!args[0].equals("compress") && !args[0].equals("decompress")) {
            System.out.println("Error. First argument should be equal to compress or decompress.");
            return;
        }
        String huffmanCodesMode = "";
        int maxBlockSize = 32768;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--huffman-codes-mode") || args[i].equals("-huffman-codes-mode")) {
                huffmanCodesMode = args[i+1];
            }
            if (args[i].equals("--max-block-size") || args[i].equals("-max-block-size")) {
                maxBlockSize = Integer.parseInt(args[i+1]);
            }
        }

        Zip zip = new Zip();
        zip.start(args[0], args[1], huffmanCodesMode, maxBlockSize);
    }
}
