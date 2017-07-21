//package com.app.stubble.utils;
//
//
//import android.graphics.Bitmap;
//
//import java.awt.image.BufferedImage;
//
///**
// * Created by halfapple on 17/7/12.
// *
// **/
//public class ImageDiffUtil {
//
//    public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2) {
//        int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
//        int width2 = img2.getWidth(); // take no arguments
//        int height1 = img1.getHeight();
//        int height2 = img2.getHeight();
//        if ((width1 != width2) || (height1 != height2)) {
//            System.exit(1);
//        }
//
//        // NEW - Create output Buffered image of type RGB
//        BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);
//
//        // Modified - Changed to int as pixels are ints
//        int diff;
//        int result; // Stores output pixel
//        for (int i = 0; i < height1; i++) {
//            for (int j = 0; j < width1; j++) {
//                int rgb1 = img1.getRGB(j, i);
//                int rgb2 = img2.getRGB(j, i);
//                int r1 = (rgb1 >> 16) & 0xff;
//                int g1 = (rgb1 >> 8) & 0xff;
//                int b1 = (rgb1) & 0xff;
//                int r2 = (rgb2 >> 16) & 0xff;
//                int g2 = (rgb2 >> 8) & 0xff;
//                int b2 = (rgb2) & 0xff;
//                diff = Math.abs(r1 - r2); // Change
//                diff += Math.abs(g1 - g2);
//                diff += Math.abs(b1 - b2);
//                diff /= 3; // Change - Ensure result is between 0 - 255
//                // Make the difference image gray scale
//                // The RGB components are all the same
//                result = (diff << 16) | (diff << 8) | diff;
//                outImg.setRGB(j, i, result); // Set result
//            }
//        }
//
//        // Now return
//        return outImg;
//    }
//
//    public static Bitmap getDifferenceImage(Bitmap img1, Bitmap img2) {
//        int width1 = img1.getWidth();
//        int width2 = img2.getWidth();
//        int height1 = img1.getHeight();
//        int height2 = img2.getHeight();
//        if ((width1 != width2) || (height1 != height2)) {
//            return img1;
//        }
//
//        Bitmap outImg = Bitmap.createBitmap(width1, height1, Bitmap.Config.ARGB_8888);
//
//        for (int i = 0; i < height1; i++) {
//            for (int j = 0; j < width1; j++) {
//                int rgb1 = img1.getPixel(j, i);
//
//                int rgb2 = img2.getPixel(j, i);
//
//                if (rgb1 > rgb2) {
//                    outImg.setPixel(j, i, rgb1);
//
//                } else if (rgb1 == rgb2) {
//                    //outImg.setPixel(j, i, Color.parseColor("#00000000"));
//
//                } else {
//                    outImg.setPixel(j, i, rgb2);
//                }
//            }
//        }
//
//        return outImg;
//    }
//
//    public static int[] calculatePixelsSum(Bitmap bitmap) {
//        if (bitmap == null) {
//            return null;
//        }
//
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//
//        int[] array = new int[height];
//
//        for(int i = 0; i < height; i++) {
//            int sum = 0;
//            int[] pixels = new int[width];
//            bitmap.getPixels(pixels, 0, width, 0, 0, width, 1);
//
//            for (int pix: pixels) {
//                sum += pix;
//            }
//            array[i] = sum;
//        }
//
//        return array;
//    }
//
//}