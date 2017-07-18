package com.app.stubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

public class PaintActivity extends BaseActivity {

    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";
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
        mLeft = (int) (left);
        mTop = (int) (top);
        mRight = (int) (right);
        mBottom = (int) (bottom);

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
            nameImage = pathImage + strDate + ".png";

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

            marker_self_border(bitmap);
            marker_target_border(bitmap);

            //bitmap = cropBitmap(bitmap);

            save_bitmap(bitmap);
        }
    };

    private void marker_self_border(Bitmap bitmap) {
        Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStrokeWidth(20);
        mBorderPaint.setColor(Color.parseColor("#303F9F"));//blue
        mBorderPaint.setStyle(Paint.Style.STROKE);
        Canvas mCanvas = new Canvas(bitmap);
        mCanvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), mBorderPaint);
    }

    private void marker_target_border(Bitmap bitmap) {
        Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStrokeWidth(2);
        mBorderPaint.setColor(Color.parseColor("#303F9F"));//blue
        mBorderPaint.setStyle(Paint.Style.STROKE);
        Canvas mCanvas = new Canvas(bitmap);
        mCanvas.drawRect(mLeft * xFactor, mTop * yFactor, mRight * xFactor, mBottom * yFactor, mBorderPaint);
    }

    private void save_bitmap(Bitmap bitmap) {
        if(bitmap != null) {
            try {
                File pa = new File(pathImage);
                if (!pa.exists()) {
                    pa.mkdirs();
                }
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

    private Bitmap cropBitmap(Bitmap bitmap) {
        if(bitmap != null) {
            int cut_width = Math.abs(mRight - mLeft);
            int cut_height = Math.abs(mBottom - mTop);
            if (cut_width > 0 && cut_height > 0) {
                return Bitmap.createBitmap(bitmap, mLeft, mTop, cut_width, cut_height);
            }
        }
        return bitmap;
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
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screenshot",
                windowWidth, windowHeight, mScreenDensity,
                //DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.getSurface(), null, null);

        Toast.makeText(getApplicationContext(), "screenshot...", Toast.LENGTH_SHORT).show();

        Handler hl = new Handler();
        hl.postDelayed(rr, 1000);
    }

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
