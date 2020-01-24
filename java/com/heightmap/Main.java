package com.heightmap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.Raster;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Scanner;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import com.heightmap.stl.ModelObject;
import com.heightmap.stl.StlObject;

public class Main {

    public static void main(String[] args) {

        File file;
        //float base_height = 5f;
        float model_height = 20.0f;

        if (args.length != 3) {
            System.out.println("Usage: filename model_height base_height");
        } else {

            try {
                file = new File(args[0]);
                //base_height = new Float(args[2]);
                model_height = Float.parseFloat(args[1]);

                System.out.println("File: " + file.getName());
                System.out.println("Elevation multiplier: " + model_height);
                //System.out.println("Base height: " + base_height);

                int width = 0;
                int height = 0;

                float min_height = Float.MAX_VALUE;
                float max_height = Float.MIN_VALUE;

                float[][] heightmap;

                System.out.println("-----------------------");
                String filename = file.getName();
                String fileEnding = filename.substring(filename.lastIndexOf("."),filename.length());

                System.out.println("v2 Processing - " + filename);


                Color co = null;
                float[] hsv = new float[3];
                BufferedImage img;

                int filetype = 0;

                if(fileEnding.equals(".asc")) {
                    System.out.println("Arcgis ASCII GRID!");
                    filetype = 1;

//                    java.util.List<String> lines = Files.readAllLines(Paths.get(args[2]));
//                    String[] hmm = lines.get(0).split(" ");
//                    System.out.println(hmm[0] + hmm[1]);

//                    BufferedReader br = new BufferedReader(new FileReader(file));
//                    try {
//                        String line = br.readLine();
//                        while(line != null){
//                            String[] bits = line.split(" ");
//                            System.out.println(bits[0] + bits[1]);
//                            line = br.readLine();
//                        }
//                    } finally {br.close();}

                    Path path = Paths.get(args[0]);
                    Scanner scanner = new Scanner(path);
                    while(scanner.hasNext()){
                        String next = scanner.next();

                    }

                    return;
                } else {

                    img = ImageIO.read(file);
                    width = img.getWidth();
                    height = img.getHeight();

                    System.out.println("rows: " + height);
                    System.out.println("cols: " + width);

                }


                heightmap = new float[width][height];

                for (int i = 0; i < width; i++) {

                    int rowMax = Integer.MIN_VALUE;

                    for (int j = 0; j < height; j++) {

                        float h, mh;
                        int hi;

                        if(filetype == 0) {
                            co = new Color(img.getRGB(i, j));
                            Color.RGBtoHSB(co.getRed(), co.getGreen(), co.getBlue(), hsv);
                            h = hsv[2];
                            hi = co.getRed();
                        } else {
                            h = 0.0F;
                            hi = 0;
                        }

                        mh = h * model_height;

                        heightmap[width - i - 1][j] = mh;

                        if (h < min_height) {
                            min_height = h;
                        }

                        if(h > max_height) {
                            max_height = h;
                        }

                        if(hi > rowMax){
                            rowMax = hi;
                        }

                    }
                }

                System.out.println("Min Height: " + min_height);
                System.out.println("Max Height: " + max_height);


                String name = file.getName();

                StlObject stl = StlObject.fromHeightmap(name, height, width, heightmap);

                System.out.println("-----------------------");
                System.out.println("Saving to " + new File(name + ".stl").getAbsolutePath());
                stl.path = name + ".stl";
                stl.save(StlObject.FILE_BINARY);
                System.out.println("-----------------------");
                System.out.println("Done saving");

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
