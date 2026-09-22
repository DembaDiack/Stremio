package com.stremio.morphe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** A small D-pad focusable entry point that opens the audio downmix settings. */
public final class MorpheDownmixNavView extends TextView {
    public static final int VIEW_ID = 0x4d4f52b0;

    private static final int COLOR_FILL = 0x22242D37;
    private static final int COLOR_STROKE = 0xFF3B4A5A;
    private static final int COLOR_STROKE_FOCUSED = 0xFF7462F6;

    public MorpheDownmixNavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setId(VIEW_ID);
        setTag("morphe_downmix_nav");
        setGravity(Gravity.CENTER);
        setText("Audio downmix");
        setTextColor(Color.WHITE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        setTypeface(Typeface.DEFAULT_BOLD);
        setContentDescription("Audio downmix settings");
        setFocusable(true);
        setClickable(true);
        setBackground(background(false));

        setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClassName(getContext().getPackageName(),
                    "com.stremio.morphe.DownmixSettingsActivity");
            getContext().startActivity(intent);
        });

        setOnFocusChangeListener((view, hasFocus) -> {
            setBackground(background(hasFocus));
            setSelected(hasFocus);
        });
    }

    private GradientDrawable background(boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(8));
        drawable.setColor(COLOR_FILL);
        drawable.setStroke(dp(1), focused ? COLOR_STROKE_FOCUSED : COLOR_STROKE);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
