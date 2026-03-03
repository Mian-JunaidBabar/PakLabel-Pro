package com.example.labelmaker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class A4PreviewView extends View {

    // A4 dimensions in points (1/72 inch)
    private static final float A4_WIDTH_POINTS = 595f;
    private static final float A4_HEIGHT_POINTS = 842f;
    private static final float A4_ASPECT_RATIO = A4_HEIGHT_POINTS / A4_WIDTH_POINTS;

    private String labelText = "";
    private int rows = 10;
    private int cols = 3;
    private float fontSize = 12f;
    private int textColor = Color.BLACK;
    private int startColor = Color.WHITE;
    private int endColor = Color.LTGRAY;

    private Paint gridPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Rect textBounds;
    private boolean useGradient = false;

    public A4PreviewView(Context context) {
        super(context);
        init();
    }

    public A4PreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public A4PreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.rgb(200, 200, 200));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);

        textBounds = new Rect();
    }

    public void setLabelData(String text, int rows, int cols, float fontSize, 
                            int textColor, int startColor, int endColor) {
        this.labelText = text != null ? text : "";
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.fontSize = Math.max(4f, fontSize);
        this.textColor = textColor;
        this.startColor = startColor;
        this.endColor = endColor;
        
        // Check if gradient is enabled (colors are different)
        this.useGradient = (startColor != endColor);
        
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * A4_ASPECT_RATIO);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw white background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        if (labelText.isEmpty() || rows <= 0 || cols <= 0) {
            return;
        }

        drawGrid(canvas, width, height);
        drawLabels(canvas, width, height);
    }

    private void drawGrid(Canvas canvas, int width, int height) {
        float cellWidth = width / (float) cols;
        float cellHeight = height / (float) rows;

        // Draw vertical lines
        for (int i = 0; i <= cols; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, gridPaint);
        }

        // Draw horizontal lines
        for (int i = 0; i <= rows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private void drawLabels(Canvas canvas, int width, int height) {
        float cellWidth = width / (float) cols;
        float cellHeight = height / (float) rows;

        // Scale font size based on preview size vs actual A4 size
        float scaledFontSize = fontSize * (width / A4_WIDTH_POINTS);
        textPaint.setTextSize(scaledFontSize);

        // Create gradient shader if enabled
        if (useGradient) {
            Shader shader = new LinearGradient(
                0, 0, 0, scaledFontSize,
                startColor, endColor, Shader.TileMode.CLAMP
            );
            textPaint.setShader(shader);
        } else {
            textPaint.setShader(null);
            textPaint.setColor(textColor);
        }

        // Measure text bounds for centering
        textPaint.getTextBounds(labelText, 0, labelText.length(), textBounds);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float centerX = col * cellWidth + cellWidth / 2f;
                float centerY = row * cellHeight + cellHeight / 2f;

                // Center text vertically
                float textY = centerY - textBounds.exactCenterY();

                canvas.drawText(labelText, centerX, textY, textPaint);
            }
        }
    }

    /**
     * Draw the label sheet at actual A4 size onto the provided canvas
     * Used for PDF export
     */
    public void drawToCanvas(Canvas canvas, float width, float height) {
        // Save canvas state
        canvas.save();

        // Draw white background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, height, bgPaint);

        if (labelText.isEmpty() || rows <= 0 || cols <= 0) {
            canvas.restore();
            return;
        }

        float cellWidth = width / (float) cols;
        float cellHeight = height / (float) rows;

        // Draw grid
        Paint pdfGridPaint = new Paint();
        pdfGridPaint.setColor(Color.rgb(100, 100, 100));
        pdfGridPaint.setStrokeWidth(1f);
        pdfGridPaint.setStyle(Paint.Style.STROKE);
        pdfGridPaint.setAntiAlias(true);

        for (int i = 0; i <= cols; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, pdfGridPaint);
        }

        for (int i = 0; i <= rows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, pdfGridPaint);
        }

        // Draw text
        Paint pdfTextPaint = new Paint();
        pdfTextPaint.setAntiAlias(true);
        pdfTextPaint.setTextAlign(Paint.Align.CENTER);
        pdfTextPaint.setTextSize(fontSize);

        if (useGradient) {
            Shader shader = new LinearGradient(
                0, 0, 0, fontSize,
                startColor, endColor, Shader.TileMode.CLAMP
            );
            pdfTextPaint.setShader(shader);
        } else {
            pdfTextPaint.setShader(null);
            pdfTextPaint.setColor(textColor);
        }

        Rect bounds = new Rect();
        pdfTextPaint.getTextBounds(labelText, 0, labelText.length(), bounds);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float centerX = col * cellWidth + cellWidth / 2f;
                float centerY = row * cellHeight + cellHeight / 2f;
                float textY = centerY - bounds.exactCenterY();

                canvas.drawText(labelText, centerX, textY, pdfTextPaint);
            }
        }

        // Restore canvas state
        canvas.restore();
    }
}
