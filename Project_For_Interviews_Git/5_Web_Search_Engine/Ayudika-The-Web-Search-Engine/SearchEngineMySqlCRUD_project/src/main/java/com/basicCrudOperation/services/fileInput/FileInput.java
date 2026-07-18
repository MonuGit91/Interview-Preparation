package com.basicCrudOperation.services.fileInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class FileInput {
    private static File input = new File("/Users/narenderkumarchoudhary/Public/Education/Java/intellijJava/SearchEngine/AyudikaTheWebSearchEngine/SearchEngineMySqlCRUD_project/src/main/java/com/basicCrudOperation/services/fileInput/input.txt");
    public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    public static Scanner scan = new Scanner(System.in);

    public static void fileInput() {
        System.out.println("inided fileInput(): ");
        if (input.exists()) {
            System.out.println("input.exists(): ");
            try {
                scan = new Scanner(input);
                br = new BufferedReader(new FileReader(input));
                System.out.println("input file founded");
            } catch (Exception e) {
                System.out.println("input file not found!!");
            }
        }
        System.out.println("exit fileInput(): ");
    }

}
