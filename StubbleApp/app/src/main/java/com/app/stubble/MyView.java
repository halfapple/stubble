package com.app.stubble;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.app.stubble.utils.ScreenUtil;


public class MyView extends View {

    //start_point
    private float sPWidth;
    private float sPHeight;
    private Bitmap sPBitmap;
    private float sPRawX;
    private float sPRawY;

    //end_point
    private float ePWidth;
    private float ePHeight;
    private Bitmap ePBitmap;
    private float ePRawX;
    private float ePRawY;

    //边框
    private int borderColor = Color.parseColor("#FF4081");
    private float borderWidth = 3;
    private float borderFactor = 0.25f;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Paint mBorderPaint;
    private Paint mBitmapPaint;

    private float xDownInScreen;
    private float yDownInScreen;
    private float xInScreen;
    private float yInScreen;
    private float xStartPointOffset;
    private float yStartPointOffset;
    private float xEndPointOffset;
    private float yEndPointOffset;

    private int mScreenW;
    private int mScreenH;

    private Context mContext;
    private OnScreenShotListener mOnScreenShotListener;
    private OnMovedListener mOnMovedListener;

    private boolean isInSlide;
    private boolean isInRectangle;

    public interface OnScreenShotListener {
        void onScreenShot(float left, float top, float right, float bottom);
    }

    public interface OnMovedListener {
        void onMovedListener(float left, float top, float right, float bottom);
    }

    public void setOnScreenShotListener(OnScreenShotListener arg) {
        this.mOnScreenShotListener = arg;
    }

    public void setOnMovedListener(OnMovedListener arg) {
        this.mOnMovedListener = arg;
    }

    public MyView(Context context) {
        this(context, null);
    }

    public MyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyView, defStyleAttr, 0);

        try {
            mContext = context;

            mScreenW = ScreenUtil.getScreenWidth(context);
            mScreenH = ScreenUtil.getScreenHeight(context);

            sPWidth = ScreenUtil.dip2px(context, 10);
            sPHeight = ScreenUtil.dip2px(context, 10);
            ePWidth = ScreenUtil.dip2px(context, 10);
            ePHeight = ScreenUtil.dip2px(context, 10);

            sPBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.start_point_default);
            ePBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.end_point_default);

            int count = ta.getIndexCount();
            for (int i = 0; i < count; i++) {

                int index = ta.getIndex(i);

                switch (index) {
                    case R.styleable.MyView_start_point_bg:
                        sPBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.start_point_default);
                        break;
                    case R.styleable.MyView_start_point_width:
                        sPWidth = ta.getDimension(index, 20);
                        break;
                    case R.styleable.MyView_start_point_height:
                        sPHeight = ta.getDimension(index, 20);
                        break;

                    case R.styleable.MyView_end_point_bg:
                        ePBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.end_point_default);
                        break;
                    case R.styleable.MyView_end_point_width:
                        ePWidth = ta.getDimension(index, 20);
                        break;
                    case R.styleable.MyView_end_point_height:
                        ePHeight = ta.getDimension(index, 20);
                        break;

                    case R.styleable.MyView_boder_color:
                        borderColor = ta.getColor(index, Color.parseColor("#FF4081"));
                        break;

                    case R.styleable.MyView_boder_width:
                        borderWidth = ta.getDimension(index, 3);
                        break;
                }
            }

            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStrokeWidth(borderWidth);
            mBorderPaint.setColor(borderColor);

            mBitmapPaint = new Paint(Paint.DITHER_FLAG);

            init();

        } finally {
            ta.recycle();
        }
    }

    private void init() {
        post(new Runnable() {
            @Override
            public void run() {
                mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
                mCanvas.drawColor(Color.TRANSPARENT);

                sPRawX = (1 - borderFactor) / 2 * getWidth();
                sPRawY = (1 - borderFactor) / 2 * getHeight();
                ePRawX = (1 + borderFactor) / 2 * getWidth();
                ePRawY = (1 + borderFactor) / 2 * getHeight();

                update_view_position(true);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY();
                xInScreen = event.getRawX();
                yInScreen = event.getRawY();
                xStartPointOffset = event.getRawX() - sPRawX;
                yStartPointOffset = event.getRawY() - sPRawY;
                xEndPointOffset = event.getRawX() - ePRawX;
                yEndPointOffset = event.getRawY() - ePRawY;
                isInSlide = isInSlide(event.getRawX(), event.getRawY());
                isInRectangle = isInRectangle(event.getRawX(), event.getRawY());
                break;

            case MotionEvent.ACTION_MOVE:
                xInScreen = event.getRawX();
                yInScreen = event.getRawY();
                if (isInSlide) {
                    ePRawX = (xInScreen - xEndPointOffset);
                    ePRawY = (yInScreen - yEndPointOffset);
                    if (ePRawX < sPRawX) {
                        ePRawX = sPRawX;
                    }
                    if (ePRawY < sPRawY) {
                        ePRawY = sPRawY;
                    }
                    if (ePRawX > mScreenW) {
                        ePRawX = mScreenW;
                    }
                    if (ePRawY > mScreenH) {
                        ePRawY = mScreenH;
                    }
                    update_view_position(true);

                } else if (isInRectangle) {
                    //边界处理
                    float w = ePRawX - sPRawX;
                    float h = ePRawY - sPRawY;

                    if (xInScreen - xStartPointOffset >= 0 &&
                            xInScreen - xStartPointOffset + w <= mScreenW) {
                        sPRawX = (xInScreen - xStartPointOffset);
                        ePRawX = sPRawX + w;
                    }

                    if (yInScreen - yStartPointOffset >= 0 &&
                            yInScreen - yStartPointOffset + h <= mScreenH) {
                        sPRawY = (yInScreen - yStartPointOffset);
                        ePRawY = sPRawY + h;
                    }

                    update_view_position(true);
                }

                if (mOnMovedListener != null) {
                    mOnMovedListener.onMovedListener(sPRawX, sPRawY, ePRawX, ePRawY);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (xDownInScreen == xInScreen && yDownInScreen == yInScreen &&
                        ((sPRawX - sPWidth / 2) < xDownInScreen) &&
                        ((sPRawX + sPWidth / 2) > xDownInScreen) &&
                        ((sPRawY - sPHeight / 2) < yDownInScreen) &&
                        ((sPRawY + sPHeight / 2) > yDownInScreen)) {
                    if (mOnScreenShotListener != null) {
                        mOnScreenShotListener.onScreenShot(sPRawX, sPRawY, ePRawX, ePRawY);
                    }
                }
                break;

        }
        return true;
    }

    public void showBorder() {
        update_view_position(true);
    }

    public void hideBorder() {
        update_view_position(false);
    }

    private void update_view_position(boolean drawBorder) {

        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (drawBorder) {
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mCanvas.drawRect(sPRawX, sPRawY, ePRawX, ePRawY, mBorderPaint);
            mCanvas.drawLine(sPRawX, (sPRawY + ePRawY) / 2,
                    ePRawX, (sPRawY + ePRawY) / 2, mBorderPaint);

            Rect rectStart = new Rect((int)(sPRawX - sPWidth / 2),
                    (int)(sPRawY - sPHeight / 2),
                    (int)(sPRawX + sPWidth / 2),
                    (int)(sPRawY + sPHeight / 2));
            mCanvas.drawBitmap(sPBitmap, null, rectStart, null);

            Rect rectEnd = new Rect((int)(ePRawX - ePWidth / 2),
                    (int)(ePRawY - ePHeight / 2),
                    (int)(ePRawX + ePWidth / 2),
                    (int)(ePRawY + ePHeight / 2));
            mCanvas.drawBitmap(ePBitmap, null, rectEnd, null);
        }

        postInvalidate();
    }

    private boolean isInRectangle(float x, float y) {
        if ((x >= sPRawX && x < (ePRawX - ePWidth / 2)
                && y >= sPRawY && y < ePRawY)
                ||
                (x >= (ePRawX - ePWidth / 2) && x < ePRawX
                && y >= sPRawY && y < (ePRawY - ePWidth / 2))) {
            return true;
        }
        return false;
    }

    private boolean isInSlide(float x, float y) {
        if (x >= (ePRawX - ePWidth / 2) &&
                x < (ePRawX + ePWidth / 2) &&
                y >= (ePRawY - ePHeight / 2) &&
                y < (ePRawY + ePHeight / 2)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        }

    }
}



