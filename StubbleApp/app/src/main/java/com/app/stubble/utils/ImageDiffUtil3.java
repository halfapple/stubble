package com.app.stubble.utils;


import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.app.stubble.data.InterSectionData;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 *
 **/
public class ImageDiffUtil3 {

    public static final int mFingerLength = 200;

    /*
     * 向中心缩小
     */
    public static int step_first_check_even_number(float value, boolean isLeftOrTop) {
        int intV = (int) value;
        int aboutV = (int)(value + 0.5);

        int result = intV;

        if (aboutV > intV) {
            if (isLeftOrTop) {
                result = aboutV;
            }
        }

        return result;
    }

    public static Bitmap step_second_scale_bitmap(Bitmap bitmap, int[] scaleXY) {
        if (scaleXY == null || scaleXY.length < 2) {
            throw new IllegalArgumentException("scaleXY must be an array of two integers");
        }

        int sx = 1;
        int sy = 1;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        while ((width / sx) * (height / sy) > mFingerLength) {
            sx = sx * 2;
            sy = sy * 2;

            if (width / sx < 1 || height / sy < 1) {
                sx = sx / 2;
                sy = sy / 2;
                break;
            }
        }

        scaleXY[0] = sx;
        scaleXY[1] = sy;

        Matrix matrix = new Matrix();
        matrix.postScale(1f / sx, 1f / sy); //sx=sy=32, 297 * 378 => 9 * 12
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);//四舍五入
    }

    public static double[][] step_third_1_getGrayPixels(Bitmap bitmap) {
        return getGrayPixels(bitmap);
    }

    public static double step_third_2_getGrayAvg(double[][] arg) {
        return getGrayAvg(arg);
    }

    public static byte[] step_third_3_getFinger(double[][] pixels, double avg) {
        int width = pixels[0].length;
        int height = pixels.length;
        byte[] bytes = new byte[height * width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (pixels[i][j] >= avg) {
                    bytes[i * width + j] = 1;
                } else {
                    bytes[i * width + j] = 0;
                }
            }
        }

        return bytes;
    }

    private static double[][] getGrayPixels(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double[][] pixels = new double[height][width];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[j][i] = computeGrayValue(bitmap.getPixel(i, j));
            }
        }
        return pixels;
    }

    private static double getGrayAvg(double[][] pixels) {
        int width = pixels[0].length;
        int height = pixels.length;
        int count = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                count += pixels[i][j];
            }
        }
        return count / (width * height);
    }

    private static double computeGrayValue(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = (pixel) & 255;
        return 0.3 * red + 0.59 * green + 0.11 * blue;
    }

    private static int computeRGBDiff(int pixel1, int pixel2) {
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = (pixel1) & 255;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = (pixel2) & 255;

        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    //(alpha << 24) | (red << 16) | (green << 8) | blue;
    private static int computePixelDiff(int pixel1, int pixel2, int[] rgbDiff) {
        //int a1 = (pixel1 >> 24) & 0xFF;
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = (pixel1) & 255;

        //int a2 = (pixel2 >> 24) & 0xFF;
        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = (pixel2) & 255;

        if (rgbDiff == null || rgbDiff.length != 3) {
            throw new IllegalArgumentException();
        }

        rgbDiff[0] = Math.abs(r1 - r2);
        rgbDiff[1] = Math.abs(g1 - g2);
        rgbDiff[2] = Math.abs(b1 - b2);

        return (0xFF << 24) | (rgbDiff[0] << 16) | (rgbDiff[1] << 8) | rgbDiff[2];
    }

    /*
     * 最长连续相似, a.length = b.length
     */
    public static int[][] step_fouth_get_diff_twodimen(byte[] a, byte[] b) {
        int ll = a.length;

        int twoDimen[][] = new int[ll][ll];
        for (int i = 0; i < ll; i++) {
            for (int j = 0; j < ll; j++) {
                int vv = 0;
                if (a[i] == b[j]) {
                    vv = 1;
                }

                if (i == 0 || j == 0) {
                    twoDimen[i][j] = vv;

                } else {
                    if (twoDimen[i-1][j-1] == 0) {
                        twoDimen[i][j] = vv;
                    } else {
                        twoDimen[i][j] = twoDimen[i-1][j-1] + vv;
                    }
                }
            }
        }

        return twoDimen;
    }

    public static ArrayList<InterSectionData> step_fivth_getSimilarList(int[][] args) {
        ArrayList<InterSectionData> sectionDataArrayList = new ArrayList<>();

        int width = args[0].length;
        int height = args.length;
        //只考虑 20% 的误差
        for (int i = height * 4 / 5; i < height; i++) {
            for (int j = width * 4 / 5; j < width; j++) {
                InterSectionData is = new InterSectionData(i, j, args[i][j]);
                sectionDataArrayList.add(is);
            }
        }

        Collections.sort(sectionDataArrayList);

        return sectionDataArrayList;
    }

    /*
     * 第一张图片的第 m 行， 匹配第二张图的第 n 行, 最小差异值
     */
    public static void precise_similarest_row(Bitmap b1, Bitmap b2, int[] result) {
        if (b1.getWidth() != b2.getWidth()) {
            throw new IllegalArgumentException();
        }

        if (result == null || result.length != 3) {
            throw new IllegalArgumentException();
        }

        int width = b1.getWidth();

        int index1 = 0;
        int index2 = 0;
        int minRowDiff = -1;

        for (int k=0; k < b1.getHeight(); k++) {
            int[] pixles1 = new int[width];
            b1.getPixels(pixles1, 0, width, 0, k, width, 1);

            for(int i=0; i < b2.getHeight(); i++) {
                int[] pixles2 = new int[width];
                b2.getPixels(pixles2, 0, width, 0, i, width, 1);

                int rowDiff = 0;
                for(int j=0; j < width; j++) {
                    rowDiff += computeRGBDiff(pixles1[j], pixles2[j]);
                }

                if (minRowDiff == -1 || rowDiff < minRowDiff) {
                    index1 = k;
                    index2 = i;
                    minRowDiff = rowDiff;
                } else if (rowDiff == minRowDiff) {
                    index1 = k;
                    index2 = i;
                    minRowDiff = rowDiff;
                }
            }
        }

        result[0] = index1;
        result[1] = index2;
        result[2] = minRowDiff;
    }

    public static Bitmap last_compare_images(Bitmap srcBitmap, Bitmap targetBitmap) {
        if (srcBitmap.getWidth() != targetBitmap.getWidth() ||
                srcBitmap.getHeight() != targetBitmap.getHeight()) {
            throw new IllegalArgumentException("not the same size!");
        }

        Bitmap outImg = Bitmap.createBitmap(srcBitmap.getWidth(), 2 * srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        int[] rgbDiff = new int[3];
        for (int i = 0; i < srcBitmap.getHeight(); i++) {
            for (int j = 0; j < srcBitmap.getWidth(); j++) {
                int pixelA = srcBitmap.getPixel(j, i);
                int pixelB = targetBitmap.getPixel(j, i);

//                outImg.setPixel(j, i, pixelA);
//                outImg.setPixel(j, srcBitmap.getHeight() + i, pixelB);

//                if (pixelA == pixelB) {
//                    outImg.setPixel(j, i, pixelA);
//                } else {
//                    outImg.setPixel(j, srcBitmap.getHeight() + i, pixelB);
//                }

                int diff = computePixelDiff(pixelA, pixelB, rgbDiff);
                outImg.setPixel(j, i, pixelA);
                outImg.setPixel(j, srcBitmap.getHeight() + i, diff);


//                int cmp = computeRGBDiff(pixelA, pixelB);
//                if (cmp == 0) {
//                    outImg.setPixel(j, i, pixelA);
//                } else {
//                    outImg.setPixel(j, srcBitmap.getHeight() + i, pixelB);
//                }

            }
        }

        return outImg;
    }

}