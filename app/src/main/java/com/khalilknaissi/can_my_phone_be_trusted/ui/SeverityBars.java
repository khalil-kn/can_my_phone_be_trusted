package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;

/**
 * The severity mark from the design: five bars, filled by level, always next to the word.
 * Red is only extra emphasis for High and Critical, so it still reads in greyscale.
 */
public class SeverityBars extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int filled = 1;
    private boolean hot;

    public SeverityBars(Context context) {
        super(context);
    }

    public SeverityBars(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void set(Severity s) {
        filled = s.segments();
        hot = s.isHot();
        setContentDescription("Severity " + s.label + ", " + filled + " of 5");
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float d = getResources().getDisplayMetrics().density;
        setMeasuredDimension((int) (33 * d), (int) (14 * d));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 5; i++) {
            int color = i >= filled ? R.color.sand : hot ? R.color.seal_red : R.color.ink;
            paint.setColor(ContextCompat.getColor(getContext(), color));
            canvas.drawRect(i * 7 * d, 0, i * 7 * d + 5 * d, 14 * d, paint);
        }
    }
}
