package com.app.stubble.utils;


import android.graphics.Bitmap;

import com.app.stubble.data.InterSectionData;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 *
 **/
public class ImageDiffUtil2 {

    public static ArrayList<InterSectionData> findSmallestRate(int[][] args) {
        ArrayList<InterSectionData> sectionDataArrayList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            InterSectionData is = new InterSectionData(i, args[0].length - 1,
                    args[i][args[0].length - 1], args[0].length);
            sectionDataArrayList.add(is);
        }

        for (int j = 0; j < args[0].length; j++) {
            InterSectionData is = new InterSectionData(args[0].length - 1,
                    j,
                    args[args[0].length - 1][j], args[0].length);
            sectionDataArrayList.add(is);
        }

        Collections.sort(sectionDataArrayList);

        return sectionDataArrayList;
    }

    /*
     * 参数长度都一致
     */
    public static int[][] initTwoDimen(int[] r1, int[] g1, int[] b1,
                                       int[] r2, int[] g2, int[] b2) {
        if (r1.length != r2.length || g1.length != g2.length || b1.length != b2.length) {
            throw new RuntimeException();
        }

        int ll = r1.length;

        int twoDimen[][] = new int[ll][ll];
        for (int i = 0; i < ll; i++) {
            for (int j = 0; j < ll; j++) {
                int absDiff = (Math.abs(r1[i] - r2[j]) + Math.abs(g1[i] - g2[j]) + + Math.abs(b1[i] - b2[j])) / 3;
                if (i == 0 || j == 0) {
                    twoDimen[i][j] = absDiff;

                } else {
                    twoDimen[i][j] = twoDimen[i-1][j-1] + absDiff;
                }

                if (absDiff > 0) {
                    continue;
                }
            }
        }

        return twoDimen;
    }

    /*
     * 为每一行像素分别计算其 R， G，B 的平均值
     */
    public static void calculateRGBAverPerRow(Bitmap bitmap, int[] rr, int[] gg, int[] bb) {
        if (bitmap == null) {
            return ;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (rr.length != height) {
            throw new RuntimeException();
        }

        if (gg.length != height) {
            throw new RuntimeException();
        }

        if (bb.length != height) {
            throw new RuntimeException();
        }


        for(int i = 0; i < height; i++) {
            int rr_total = 0;
            int gg_total = 0;
            int bb_total = 0;
            int[] pixels = new int[width];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, 1);
            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                rr_total += r;
                gg_total += g;
                bb_total += b;
            }
            rr[i] = rr_total / pixels.length;
            gg[i] = gg_total / pixels.length;
            bb[i] = bb_total / pixels.length;
        }
    }

}