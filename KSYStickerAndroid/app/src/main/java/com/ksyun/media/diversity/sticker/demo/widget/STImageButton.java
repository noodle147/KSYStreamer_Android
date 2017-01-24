package com.ksyun.media.diversity.sticker.demo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by sensetime on 16-7-8.
 */
public class STImageButton extends ImageButton {
    private String text = null;  //要显示的文字
    private int color;               //文字的颜色
    public STImageButton(Context context, AttributeSet attrs) {
        super(context,attrs);
    }

    public void setText(String text){
        this.text = text;       //设置文字
    }

    public void setColor(int color){
        this.color = color;    //设置文字颜色
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint=new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(45);
        Paint.FontMetrics fm = paint.getFontMetrics();

        float width = paint.measureText(text);

        float viewWidth = canvas.getWidth();
        float viewHeight = canvas.getHeight();
        float textCenterVerticalBaselineY = viewHeight / 2 - fm.descent + (fm.descent - fm.ascent) / 2;
        float textCenterX = (float)viewWidth / 2;
        float textBaselineY = textCenterVerticalBaselineY;

        paint.setColor(color);
        canvas.drawText(text, textCenterX, textBaselineY, paint);  //绘制文字
    }
}
