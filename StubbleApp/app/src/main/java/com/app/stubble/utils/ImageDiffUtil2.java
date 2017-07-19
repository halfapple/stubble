package com.app.stubble.utils;


import android.graphics.Bitmap;

/**
 *
 *
 **/
public class ImageDiffUtil2 {

    public static Bitmap getDifferenceImage(Bitmap img1, Bitmap img2) {
        int width1 = img1.getWidth();
        int width2 = img2.getWidth();
        int height1 = img1.getHeight();
        int height2 = img2.getHeight();
        if ((width1 != width2) || (height1 != height2)) {
            return img1;
        }

        Bitmap outImg = Bitmap.createBitmap(width1, height1, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                int rgb1 = img1.getPixel(j, i);

                int rgb2 = img2.getPixel(j, i);

                if (rgb1 > rgb2) {
                    outImg.setPixel(j, i, rgb1);

                } else if (rgb1 == rgb2) {
                    //outImg.setPixel(j, i, Color.parseColor("#00000000"));

                } else {
                    outImg.setPixel(j, i, rgb2);
                }
            }
        }

        return outImg;
    }

}