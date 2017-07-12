package com.app.stubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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

    private static int statusBarHeight;

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

        final StringBuilder sb = new StringBuilder();
        mMyView.setOnScreenShotListener(new MyView.OnScreenShotListener() {
            @Override
            public void onScreenShot(float left, float top, float right, float bottom) {
                mLeft = (int) left;
                mTop = (int) top;
                mRight = (int) right;
                mBottom = (int) bottom;

                sb.delete(0, sb.length());
                sb.append("left=").append(left).append("\n")
                        .append("top=").append(top).append("\n")
                        .append("right=").append(right).append("\n")
                        .append("bottom=").append(bottom);
                mTv.setText(sb.toString());

                startScreenCapture();
            }
        });

        mMyView.setOnMovedListener(new MyView.OnMovedListener() {
            @Override
            public void onMovedListener(float left, float top, float right, float bottom) {
                sb.delete(0, sb.length());
                sb.append("left=").append(left).append("\n")
                        .append("top=").append(top).append("\n")
                        .append("right=").append(right).append("\n")
                        .append("bottom=").append(bottom);
                mTv.setText(sb.toString());
            }
        });
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
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();

            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);

            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();

            bitmap = splitBitmap(bitmap);
            save_bitmap(bitmap);
        }
    };

    private Bitmap splitBitmap(Bitmap bitmap) {
        if(bitmap != null) {
            return Bitmap.createBitmap(bitmap,
                    mLeft, mTop - getStatusBarHeight() > 0 ? mTop - getStatusBarHeight() : getStatusBarHeight(),
                    mRight - mLeft, mBottom - mTop);
        }
        return bitmap;
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
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-shot",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);

        Handler hl = new Handler();
        hl.postDelayed(rr, 1000);
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

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
}
