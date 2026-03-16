package com.example.labelmaker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
    private int labelBackgroundColor = Color.WHITE;
    private float borderWidth = 2f;
    private float letterSpacing = 0f;
    private boolean isBold = false;
    private boolean isItalic = false;

    private Paint gridPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint labelBgPaint;
    private Rect textBounds;
    
    // Zoom functionality
    private float scaleFactor = 1.0f;
    private final float minScale = 0.5f;
    private final float maxScale = 5.0f;

    private ScaleGestureDetector scaleDetector;

    public A4PreviewView(Context context) {
        super(context);
        init(context);
    }

    public A4PreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public A4PreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gridPaint = new Paint();
        gridPaint.setColor(Color.rgb(100, 100, 100));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        labelBgPaint = new Paint();
        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setAntiAlias(true);

        textBounds = new Rect();
        
        
        // Setup scale gesture detector for pinch-to-zoom
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));
                invalidate();
                return true;
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        // For accessibility, call performClick on ACTION_UP
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void setLabelData(String text, int rows, int cols, float fontSize, 
                            int textColor, int backgroundColor) {
        this.labelText = text != null ? text : "";
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.fontSize = Math.max(4f, fontSize);
        this.textColor = textColor;
        this.labelBackgroundColor = backgroundColor;
        
        invalidate();
    }
    
    public void setTypography(float letterSpacing, boolean isBold, boolean isItalic) {
        this.letterSpacing = letterSpacing;
        this.isBold = isBold;
        this.isItalic = isItalic;
        
        int textStyle = Typeface.NORMAL;
        if (isBold && isItalic) textStyle = Typeface.BOLD_ITALIC;
        else if (isBold) textStyle = Typeface.BOLD;
        else if (isItalic) textStyle = Typeface.ITALIC;
        
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, textStyle));
        textPaint.setLetterSpacing(letterSpacing);
        
        invalidate();
    }
    
    public void setBorderWidth(float width) {
        this.borderWidth = Math.max(0.5f, Math.min(10f, width));
        gridPaint.setStrokeWidth(borderWidth);
        invalidate();
    }
    
    public void setScale(float scale) {
        this.scaleFactor = Math.max(minScale, Math.min(maxScale, scale));
        invalidate();
    }
    
    public float getScale() {
        return scaleFactor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * A4_ASPECT_RATIO);
        // Ensure we at least have a reasonable height so preview is visible
        height = Math.max(height, 200);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw white background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // If there is no label text, still draw the grid so preview is visible
        if ((labelText == null || labelText.isEmpty()) || rows <= 0 || cols <= 0) {
            // draw only grid outline for empty preview
            drawLabelsAndGrid(canvas, width, height);
            return;
        }
        
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);

        drawLabelsAndGrid(canvas, (int)(width / scaleFactor), (int)(height / scaleFactor));
        
        canvas.restore();
    }

    private void drawLabelsAndGrid(Canvas canvas, int width, int height) {
        float cellWidth = width / (float) cols;
        float cellHeight = height / (float) rows;

        // Scale font size based on preview size vs actual A4 size
        float scaledFontSize = fontSize * (width / A4_WIDTH_POINTS);
        textPaint.setTextSize(scaledFontSize);

        textPaint.setColor(textColor);
        textPaint.setShader(null);
        
        if (labelText == null) labelText = "";

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cellLeft = col * cellWidth;
                float cellTop = row * cellHeight;
                float cellRight = cellLeft + cellWidth;
                float cellBottom = cellTop + cellHeight;
                
                labelBgPaint.setShader(null);
                labelBgPaint.setColor(labelBackgroundColor);
                
                // Draw label background
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, labelBgPaint);
                
                float centerX = cellLeft + cellWidth / 2f;
                float centerY = cellTop + cellHeight / 2f;

                if (!labelText.isEmpty()) {
                    String[] lines = labelText.split("\n", -1); // -1 ensures trailing newlines are kept as empty lines
                    Paint.FontMetrics fm = textPaint.getFontMetrics();
                    float lineHeight = fm.descent - fm.ascent;
                    // Add slight padding between lines
                    float lineSpacing = scaledFontSize * 0.15f; 
                    float totalHeight = (lineHeight * lines.length) + (lineSpacing * (lines.length - 1));
                    
                    float startY = centerY - totalHeight / 2f - fm.ascent;
                    
                    for (int i = 0; i < lines.length; i++) {
                        canvas.drawText(lines[i], centerX, startY + (i * (lineHeight + lineSpacing)), textPaint);
                    }
                }
            }
        }
        
        // Draw grid
        for (int i = 0; i <= cols; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, gridPaint);
        }

        for (int i = 0; i <= rows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, gridPaint);
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

        if (labelText == null) labelText = "";
        if (rows <= 0 || cols <= 0) {
            canvas.restore();
            return;
        }

        float cellWidth = width / (float) cols;
        float cellHeight = height / (float) rows;

        // Draw label backgrounds and text
        Paint pdfLabelBgPaint = new Paint();
        pdfLabelBgPaint.setStyle(Paint.Style.FILL);
        pdfLabelBgPaint.setAntiAlias(true);
        
        Paint pdfTextPaint = new Paint();
        pdfTextPaint.setAntiAlias(true);
        pdfTextPaint.setTextAlign(Paint.Align.CENTER);
        pdfTextPaint.setTextSize(fontSize);
        pdfTextPaint.setColor(textColor);
        pdfTextPaint.setShader(null);



        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cellLeft = col * cellWidth;
                float cellTop = row * cellHeight;
                float cellRight = cellLeft + cellWidth;
                float cellBottom = cellTop + cellHeight;
                
                pdfLabelBgPaint.setShader(null);
                pdfLabelBgPaint.setColor(labelBackgroundColor);
                
                // Draw label background
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, pdfLabelBgPaint);
                
                float centerX = cellLeft + cellWidth / 2f;
                float centerY = cellTop + cellHeight / 2f;

                if (!labelText.isEmpty()) {
                    String[] lines = labelText.split("\n", -1);
                    Paint.FontMetrics fm = pdfTextPaint.getFontMetrics();
                    float lineHeight = fm.descent - fm.ascent;
                    float lineSpacing = fontSize * 0.15f;
                    float totalHeight = (lineHeight * lines.length) + (lineSpacing * (lines.length - 1));
                    
                    float startY = centerY - totalHeight / 2f - fm.ascent;
                    
                    for (int i = 0; i < lines.length; i++) {
                        canvas.drawText(lines[i], centerX, startY + (i * (lineHeight + lineSpacing)), pdfTextPaint);
                    }
                }
            }
        }
        
        // Draw grid
        Paint pdfGridPaint = new Paint();
        pdfGridPaint.setColor(Color.rgb(100, 100, 100));
        pdfGridPaint.setStrokeWidth(borderWidth);
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

        // Restore canvas state
        canvas.restore();
    }
}
