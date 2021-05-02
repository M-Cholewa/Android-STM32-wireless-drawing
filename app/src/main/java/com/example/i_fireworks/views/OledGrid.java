package com.example.i_fireworks.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.i_fireworks.R;

import java.util.Arrays;

public class OledGrid extends View {

    //consts
    private static final String TAG = "OledGrid";
    private final int STROKE_SIZE = 1;

    private int oledHeight = 32;
    private int oledWidth = 128;

    Context mContext;
    int pixelSize, viewHeight = 0, viewWidth = 0, currentBrush = 0;
    int bgColor, frameColor, brushColor;
    boolean shouldDrawGrid = true, zoomed = false;


    //draw objects
    RectF pixelRect = new RectF();
    Paint framePaint = new Paint();
    Paint bgPaint = new Paint();
    Paint picturePaint = new Paint();

    int[][] pictureMatrix = new int[oledHeight + 1][oledWidth + 1];
    int x_leftTopZoomBound = 0, x_rightBottomZoomBound=128;
    int y_leftTopZoomBound = 0, y_rightBottomZoomBound=32;
    int zoomScale = 2; //power of two

    private onPixelChangeListener pixelChangeListener;


    public interface BrushConstants {
        int BRUSH_DRAW = 1;
        int BRUSH_ERASER = 2;
        int BRUSH_STICKER = 3;
        int BRUSH_ZOOM = 4;
    }

    public interface onPixelChangeListener{
        void pixelDrawn(int x, int y, int color);
        void brushChanged(int brush);
        void gridCleared();
    }

    public OledGrid(Context context) {
        super(context);
        init();
    }

    public OledGrid(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OledGrid(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    private void init() {
        mContext = getContext();
        frameColor = ContextCompat.getColor(mContext, R.color.black);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(STROKE_SIZE);
        framePaint.setColor(frameColor);
        framePaint.setAntiAlias(true);

        bgColor = ContextCompat.getColor(mContext, R.color.bg_dark);
        brushColor = ContextCompat.getColor(mContext, R.color.design_default_color_error);
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);

        picturePaint.setAntiAlias(true);
        picturePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        picturePaint.setColor(brushColor);
    }

    private void blackWhite(Canvas c) {
        Bitmap bmp = ((BitmapDrawable) getContext().getResources().getDrawable(R.drawable.pikachu)).getBitmap();
        int originalHeight = bmp.getHeight();
        float scale = (float) oledHeight / (float) originalHeight;
        int originalWidth = bmp.getWidth();
        Bitmap pixelatedBmp = Bitmap.createScaledBitmap(bmp, (int) (originalWidth * scale), oledHeight, true);
        Bitmap scaledBmp = Bitmap.createScaledBitmap(pixelatedBmp, viewWidth, viewHeight, true);
        Bitmap scaledMonochromeBmp = createBlackAndWhite(scaledBmp);
        Paint paint = new Paint();
        c.drawBitmap(scaledMonochromeBmp, 0, 0, paint);
    }

    private Bitmap createBlackAndWhite(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

                // use 128 as threshold, above -> white, below -> black
                if (gray > 160)
                    gray = 0;
                else
                    gray = 255;
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
//                pixelChangeListener.pixelDrawn(getXPixelCoordinate(x),getYPixelCoordinate(y),brushColor);

            }
        }
        return bmOut;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, viewWidth, viewHeight, bgPaint);
//        blackWhite(canvas);
        if (shouldDrawGrid)
            drawGrid(canvas);
        //todo change color thing
        drawPicture(canvas);
    }


    private void drawGrid(Canvas canvas) {
        //todo change color thing
        frameColor = ContextCompat.getColor(mContext, R.color.HTMLGray);
        framePaint.setColor(frameColor);

        for (int x = 0; x <= viewWidth; x += pixelSize) {
            for (int y = 0; y <= viewHeight; y += pixelSize) {
                framePaint.setStyle(Paint.Style.STROKE);
                framePaint.setColor(frameColor);
                pixelRect.set(x, y, pixelSize, pixelSize);
                canvas.drawRect(pixelRect, framePaint);
            }
        }

    }

    private void drawPicture(Canvas canvas) {

        int mx = 0, my;
        Log.d(TAG, "drawPicture: "+x_rightBottomZoomBound);
        Log.d(TAG, "drawPicture: "+y_rightBottomZoomBound);
        for (int x = x_leftTopZoomBound; x <= x_rightBottomZoomBound; x++) {
            my = 0;
            for (int y = y_leftTopZoomBound; y <= y_rightBottomZoomBound; y++) {
                if (pictureMatrix[y][x] == 0) {
                    my += pixelSize;
                    continue;
                }
//                    picturePaint.setColor(pictureMatrix[y][x]);
                pixelRect.set(mx, my, mx + pixelSize, my + pixelSize);

                canvas.drawRect(pixelRect, picturePaint);
                my += pixelSize;
            }
            mx += pixelSize;
        }
    }

    private void drawPixel(int screenX, int screenY) {
        int mBrushColor = currentBrush == BrushConstants.BRUSH_ERASER
                ? 0
                : brushColor;

        int pixelX = getXPixelCoordinate(screenX)+x_leftTopZoomBound;
        int pixelY = getYPixelCoordinate(screenY)+y_leftTopZoomBound;
        if (screenX > viewWidth - 1 || pixelX < 0)
            return;
        if (screenY > viewHeight - 1 || pixelY < 0)
            return;

        pictureMatrix[pixelY][pixelX] = mBrushColor;
        pixelChangeListener.pixelDrawn(pixelX,pixelY,mBrushColor);

        if ((pixelY - 1) >= 0){

            pictureMatrix[pixelY - 1][pixelX] = mBrushColor; //above
            pixelChangeListener.pixelDrawn(pixelX- 1,pixelY,mBrushColor);

        }
        if ((pixelY - 1) >= 0 && (pixelX - 1) >= 0){

            pictureMatrix[pixelY - 1][pixelX - 1] = mBrushColor;
            pixelChangeListener.pixelDrawn(pixelX- 1,pixelY- 1,mBrushColor);

        }
        if ((pixelX - 1) >= 0){

            pictureMatrix[pixelY][pixelX - 1] = mBrushColor; //next to
            pixelChangeListener.pixelDrawn(pixelX,pixelY- 1,mBrushColor);

        }
    }


    public void zoomIn(int rawX, int rawY) {

        if (zoomed){
            oledHeight = 32;
            oledWidth=128;
            x_leftTopZoomBound = 0; x_rightBottomZoomBound=128;
            y_leftTopZoomBound = 0; y_rightBottomZoomBound=32;
            zoomed = false;
            recalculatePixelSize();
            invalidate();
            return;
        }
        Log.d(TAG, "zoomIn: rawX "+rawX);
        Log.d(TAG, "zoomIn: rawY "+rawY);

        oledHeight = oledHeight / zoomScale;
        oledWidth = oledWidth / zoomScale;
        int scaledHalfScreenHor = pixelSize * oledWidth / 2;
        int scaledHalfScreenVer = pixelSize * oledHeight / 2;

        int scaledOledWidthPx = oledWidth * pixelSize;
        int scaledOledHeightPx = oledHeight * pixelSize;


        Log.d(TAG, "zoomIn: halfScreenHor: "+scaledHalfScreenHor);
        if (rawX - scaledHalfScreenHor >= 0) {
            if (rawX + scaledHalfScreenHor > viewWidth) {
                //left side fits, right doesn't
                Log.d(TAG, "zoomIn: 1");

                x_rightBottomZoomBound = viewWidth;
                x_leftTopZoomBound = rawX - scaledHalfScreenHor - (rawX + scaledHalfScreenHor - viewWidth);
            } else {
                //both sides fit
                Log.d(TAG, "zoomIn: 2");

                x_rightBottomZoomBound = rawX + scaledHalfScreenHor;
                x_leftTopZoomBound = rawX - scaledHalfScreenHor;
            }
        } else {
            //left side doesn't fit, right does
            Log.d(TAG, "zoomIn: 3");

            x_leftTopZoomBound = 0;
            x_rightBottomZoomBound = Math.abs(rawX - scaledHalfScreenHor) + rawX + scaledHalfScreenHor;
        }


        if (rawY - scaledHalfScreenVer >= 0) {
            if (rawY + scaledHalfScreenVer > viewHeight) {
                //top side fits, bottom doesn't
                y_rightBottomZoomBound = viewHeight;
                y_leftTopZoomBound = rawY - scaledHalfScreenVer - (rawY + scaledHalfScreenVer - viewHeight);
            } else {
                //both sides fit
                y_rightBottomZoomBound = rawY + scaledHalfScreenVer;
                y_leftTopZoomBound = rawY - scaledHalfScreenVer;
            }
        } else {
            //top side doesn't fit, bottom does
            y_leftTopZoomBound = 0;
            y_rightBottomZoomBound = Math.abs(rawY - scaledHalfScreenVer) + rawY + scaledHalfScreenVer;
        }


        y_rightBottomZoomBound = getYPixelCoordinate(y_rightBottomZoomBound);
        y_leftTopZoomBound = getYPixelCoordinate(y_leftTopZoomBound);
        x_rightBottomZoomBound = getXPixelCoordinate(x_rightBottomZoomBound);
        x_leftTopZoomBound = getXPixelCoordinate(x_leftTopZoomBound);
        Log.d(TAG, "zoomIn: x_leftTopZoomBound: "+x_leftTopZoomBound);
        Log.d(TAG, "zoomIn: y_leftTopZoomBound: "+y_leftTopZoomBound);
        Log.d(TAG, "zoomIn: x_rightBottomZoomBound: "+x_rightBottomZoomBound);
        Log.d(TAG, "zoomIn: y_rightBottomZoomBound: "+y_rightBottomZoomBound);
        recalculatePixelSize();

        zoomed = true;
        invalidate();
    }


    private int getXPixelCoordinate(int screenX) {
        Log.d(TAG, "getXPixelCoordinate: " + (screenX / pixelSize));

        return screenX / pixelSize;
    }

    private int getYPixelCoordinate(int screenY) {
        Log.d(TAG, "getYPixelCoordinate: " + (screenY / pixelSize));
        return screenY / pixelSize;
    }

    public void clearDisplay() {
        pixelChangeListener.gridCleared();
        for (int[] matrix : pictureMatrix) Arrays.fill(matrix, 0);
        invalidate();
    }

    public void setShouldDrawGrid(boolean shouldDrawGrid) {
        this.shouldDrawGrid = shouldDrawGrid;
        invalidate();
    }

    public void setBrush(int brush) {
        this.currentBrush = brush;
        pixelChangeListener.brushChanged(brush);
    }

    public void setOnPixelChangeListener(onPixelChangeListener pixelChangeListener) {
        this.pixelChangeListener = pixelChangeListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int rX = (int) event.getX();
        int rY = (int) event.getY();
        if (currentBrush == BrushConstants.BRUSH_ZOOM) {
            if (event.getAction() == MotionEvent.ACTION_DOWN){
                zoomIn(rX, rY);
                invalidate();
            }
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPixel(rX, rY);
                break;
            case MotionEvent.ACTION_MOVE:
                final int historySize = event.getHistorySize();
                final int pointerCount = event.getPointerCount();
                for (int h = 0; h < historySize; h++) {
                    for (int p = 0; p < pointerCount; p++) {
                        drawPixel((int) event.getHistoricalX(p, h), (int) event.getHistoricalY(p, h));
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: ACTION UP");
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        recalculatePixelSize();
        viewWidth = oledWidth * pixelSize;
        viewHeight = oledHeight * pixelSize;
        Log.d(TAG, "onMeasure: pixelSize" + pixelSize);
        Log.d(TAG, "onMeasure: viewWidth" + viewWidth);

        this.setMeasuredDimension(viewWidth, viewHeight);
    }

    private void recalculatePixelSize() {
        pixelSize = (int) Math.ceil(((double) viewWidth - 2 * (double)STROKE_SIZE) / (double)oledWidth);
        Log.d(TAG, "recalculatePixelSize: viewWidth: "+viewWidth);
        Log.d(TAG, "recalculatePixelSize: pixelSize: "+pixelSize);
    }
}
