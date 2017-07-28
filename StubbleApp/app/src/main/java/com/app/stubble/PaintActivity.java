package com.app.stubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.app.stubble.data.InterSectionData;
import com.app.stubble.data.StartEndNum;
import com.app.stubble.utils.ImageDiffUtil3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static com.app.stubble.utils.ImageDiffUtil3.step_first_check_even_number;

public class PaintActivity extends BaseActivity {

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private TextView mTv;
    private MyView mMyView;

    private SimpleDateFormat dateFormat = null;
    private String strDate = null;
    private String pathImage = null;
    private String nameImage = null;

    private int windowWidth;
    private int windowHeight;
    private ImageReader mImageReader;

    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;

    private int mLeft, mTop, mRight, mBottom;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;

    final StringBuilder sb = new StringBuilder();

    private static int statusBarHeight;
    //Physical size: 1440x2560, screenSize: 1440 x 2392
    private float xFactor = 0.9333f; // (1440 - 47 - 49) / 1440.0
    private float yFactor = 0.9298f; // (2392 - 168) / 2392.0

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        initData();
        initView();
    }

    private void initView() {
        mTv = (TextView) findViewById(R.id.location_tv);
        mMyView = (MyView) findViewById(R.id.rectangle_view_id);

        mMyView.setOnScreenShotListener(new MyView.OnScreenShotListener() {
            @Override
            public void onScreenShot(float left, float top, float right, float bottom) {
                update_tv(left, top, right, bottom);
                startScreenCapture();
            }
        });

        mMyView.setOnMovedListener(new MyView.OnMovedListener() {
            @Override
            public void onMovedListener(float left, float top, float right, float bottom) {
                update_tv(left, top, right, bottom);
            }
        });
    }

    private void update_tv(float left, float top, float right, float bottom) {
        mLeft = step_first_check_even_number(left, true);
        mTop = step_first_check_even_number(top, true);
        mRight = step_first_check_even_number(right, false);
        mBottom = step_first_check_even_number(bottom, false);

//        //todo fake
//        mLeft = 17;
//        mTop = 6;
//        mRight = 336;
//        mBottom = 821;

        sb.delete(0, sb.length());
        sb.append("left=").append(mLeft).append("\n")
                .append("top=").append(mTop).append("\n")
                .append("right=").append(mRight).append("\n")
                .append("bottom=").append(mBottom);
        mTv.setText(sb.toString());
    }

    private void initData() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        WindowManager mWindowManager = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager.getDefaultDisplay().getWidth();
        windowHeight = mWindowManager.getDefaultDisplay().getHeight();
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "取消", Toast.LENGTH_SHORT).show();
                return;
            }

            mResultCode = resultCode;
            mResultData = data;

            setUpMediaProjection();
            setUpVirtualDisplay();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tearDownMediaProjection();
    }

    private Runnable rr = new Runnable() {
        @Override
        public void run() {

            dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/";
            strDate = dateFormat.format(new java.util.Date());

            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth(); //1440
            int height = image.getHeight(); //2392
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer(); //capacity = limit = 15921152

            int pixelStride = planes[0].getPixelStride(); //4
            int rowStride = planes[0].getRowStride(); //6656
            int rowPadding = rowStride - pixelStride * width; //896
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);// 1664
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();

            int[] pixel = new int[width];
            bitmap.getPixels(pixel, 0, width, 0, 0, width, 1);
            int leftPadding = 0;
            int rightPadding = width;
            for (int i=0; i<pixel.length;i++) {
                if (pixel[i] !=0 ) {
                    leftPadding = i; //47
                    break;
                }
            }
            for (int i=pixel.length-1;i>=0;i--) {
                if (pixel[i] != 0) {
                    rightPadding = i; //1391
                    break;
                }
            }
            bitmap = Bitmap.createBitmap(bitmap, leftPadding, 0, rightPadding-leftPadding, height); //delete empty pixel

            save_bitmap(bitmap, "b1_b2_src");

            int newLeft = (int)(mLeft * xFactor);
            int newTop = (int)(mTop * yFactor);
            int newWidth = (int)((mRight - mLeft) * xFactor);
            int newHeight = (int)((mBottom - mTop) * yFactor);

            Bitmap b1_src = Bitmap.createBitmap(bitmap,
                    newLeft,
                    newTop,
                    newWidth,
                    newHeight / 2);
            save_bitmap(b1_src, "b1_src");
            Bitmap b2_src = Bitmap.createBitmap(bitmap,
                    newLeft,
                    newTop + newHeight / 2,
                    newWidth,
                    newHeight / 2);
            save_bitmap(b2_src, "b2_src");

            Bitmap b1_copy = Bitmap.createBitmap(b1_src, 0, 0, b1_src.getWidth(), b1_src.getHeight());
            Bitmap b2_copy = Bitmap.createBitmap(b2_src, 0, 0, b2_src.getWidth(), b2_src.getHeight());

            StartEndNum startEndNum1 = new StartEndNum(0, b1_src.getHeight(), 1);
            StartEndNum startEndNum2 = new StartEndNum(0, b2_src.getHeight(), 1);

            int[] xy = new int[2];
            Bitmap scale_b1 = ImageDiffUtil3.step_second_scale_bitmap(b1_copy, xy);
            Bitmap scale_b2 = ImageDiffUtil3.step_second_scale_bitmap(b2_copy, xy);

            double[][] grayPixels1 = ImageDiffUtil3.step_third_1_getGrayPixels(scale_b1);
            double avg = ImageDiffUtil3.step_third_2_getGrayAvg(grayPixels1);
            byte[] bytes1 = ImageDiffUtil3.step_third_3_getFinger(grayPixels1, avg); //finger1

            double[][] grayPixels2 = ImageDiffUtil3.step_third_1_getGrayPixels(scale_b2);
            byte[] bytes2 = ImageDiffUtil3.step_third_3_getFinger(grayPixels2, avg); //finger2

            int[][] twoDimen = ImageDiffUtil3.step_fouth_get_diff_twodimen(bytes1, bytes2);

            ArrayList<InterSectionData> arrayList = ImageDiffUtil3.step_fivth_getSimilarList(twoDimen);

            if (arrayList != null && arrayList.size() > 0) {
                InterSectionData last = arrayList.get(arrayList.size() - 1);

                int unit1 = b1_copy.getHeight() / scale_b1.getHeight();
                int scale1_start = (last.iBegin + scale_b1.getWidth() / 2) / scale_b1.getWidth();
                startEndNum1.setStart(startEndNum1.getStart() + unit1 * scale1_start);
                startEndNum1.setUnit(unit1);
                if (startEndNum1.getEnd() == b1_src.getHeight()) {
                    int scale1_end = last.iEnd / scale_b1.getWidth();
                    startEndNum1.setEnd(unit1 * scale1_end);
                }

                int unit2 = b2_copy.getHeight() / scale_b2.getHeight();
                int scale2_start = (last.jBegin + scale_b2.getWidth() / 2 ) / scale_b2.getWidth();
                startEndNum2.setStart(startEndNum2.getStart() + (unit2 * scale2_start));
                startEndNum2.setUnit(unit2);
                if (startEndNum2.getEnd() == b2_src.getHeight()) {
                    int scale2_end = last.jEnd / scale_b2.getWidth();
                    startEndNum2.setEnd(unit2 * scale2_end);
                }

                startEndNum1.setStart(0);

                int hh = startEndNum1.getStart() > startEndNum2.getStart() ?
                        startEndNum1.getStart() : startEndNum2.getStart();
                if (hh == 0) {
                    hh = startEndNum1.getUnit();
                }
                b1_copy = Bitmap.createBitmap(b1_src,
                        0, 0,
                        b1_src.getWidth(), hh);

                startEndNum2.setStart(0);
                b2_copy = Bitmap.createBitmap(b2_src,
                        0, 0,
                        b2_src.getWidth(), hh);

                save_bitmap(b1_copy, "b1_temp");
                save_bitmap(b2_copy, "b2_temp");

                //开始精细查找
                int[] shifts = new int[3];
                ImageDiffUtil3.precise_similarest_row(b1_copy, b2_copy, shifts);

                if (shifts[0] > shifts[1]) {
                    startEndNum1.setStart(startEndNum1.getStart() + shifts[0] - shifts[1]);
                } else if (shifts[0] < shifts[1]) {
                    startEndNum2.setStart(startEndNum2.getStart() + shifts[1] - shifts[0]);
                }
            }

            int splited_height1 = startEndNum1.getEnd() - startEndNum1.getStart();
            int splited_height2 = startEndNum2.getEnd() - startEndNum2.getStart();
            int splited_height = splited_height1 > splited_height2 ? splited_height2 : splited_height1;

            b1_copy = Bitmap.createBitmap(b1_src,
                    0, startEndNum1.getStart(),
                    b1_src.getWidth(), splited_height);
            b2_copy = Bitmap.createBitmap(b2_src,
                    0, startEndNum2.getStart(),
                    b2_src.getWidth(), splited_height);
            save_bitmap(b1_copy, "b1_final");
            save_bitmap(b2_copy, "b2_final");

            Bitmap resultBit = ImageDiffUtil3.last_compare_images(b1_copy, b2_copy);
            save_bitmap(resultBit, "result");

            mMyView.showBorder();
        }
    };

    private void save_bitmap(Bitmap bitmap, String name) {
        if(bitmap != null) {
            try {
                File pa = new File(pathImage);
                if (!pa.exists()) {
                    pa.mkdirs();
                }

                nameImage = pathImage + name + ".png";

                File fileImage = new File(nameImage);
                if(!fileImage.exists()) {
                    fileImage.createNewFile();
                }
                FileOutputStream out = new FileOutputStream(fileImage);
                if(out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();

                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[] { fileImage.getPath() },
                            new String[] { "image/jpeg" }, null);
                }
            }catch(FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void startScreenCapture() {
        if (mMediaProjection != null) {
            setUpVirtualDisplay();

        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();

        } else {
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void setUpVirtualDisplay() {

        mMyView.hideBorder();

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screenshot",
                windowWidth, windowHeight, mScreenDensity,
                //DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.getSurface(), null, null);

        Handler hl = new Handler();
        hl.postDelayed(rr, 1000);
        //hl.postDelayed(rr2, 1000);
    }

    private Runnable rr2 = new Runnable() {
        @Override
        public void run() {
            Bitmap b1 = BitmapFactory.decodeResource(getResources(), R.mipmap.moon_test_1);
            Bitmap b2 = BitmapFactory.decodeResource(getResources(), R.mipmap.moon_test_2);

//            int newShift = 2;
//            int hh = b1.getHeight() - newShift;
//            b1 = Bitmap.createBitmap(b1, 0, 2, b1.getWidth(), hh);
//            b2 = Bitmap.createBitmap(b2, 0, 0, b2.getWidth(), hh);

            Bitmap resultBit = ImageDiffUtil3.last_compare_images(b1, b2);

            pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/";
            save_bitmap(resultBit, "test_result");

            mMyView.showBorder();
        }
    };

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }

    private int getSoftButtonsBarHeight() {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;

            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

}
