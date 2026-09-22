package com.stremio.morphe;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Runtime settings for the audio downmix patch. Values are stored in
 * SharedPreferences and read live by {@link AudioDownmixBridge}.
 */
public final class DownmixSettingsActivity extends Activity {
    private static final int MIN_BOOST_DB = 0;
    private static final int MAX_BOOST_DB = 30;

    private TextView boostValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudioDownmixBridge.init(this);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(Color.rgb(12, 11, 18));
        window.setNavigationBarColor(Color.rgb(12, 11, 18));

        setContentView(buildContent());
    }

    private LinearLayout buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(12, 11, 18));
        root.setPadding(dp(48), dp(32), dp(48), dp(32));

        TextView title = label("Audio downmix", 24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        root.addView(spacer(dp(24)));

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView toggleLabel = label("Enable stereo downmix", 18);
        Switch toggle = new Switch(this);
        toggle.setChecked(AudioDownmixBridge.isEnabled());
        toggle.setOnCheckedChangeListener(
                (view, checked) -> AudioDownmixBridge.setEnabled(checked));
        toggleRow.addView(toggleLabel, new LinearLayout.LayoutParams(0, dp(56), 1f));
        toggleRow.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(toggleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(spacer(dp(24)));

        root.addView(label("Center boost (dB)", 18), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        boostValue = label(AudioDownmixBridge.getCenterBoostDb() + " dB", 20);
        boostValue.setGravity(Gravity.CENTER);
        root.addView(boostValue, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        SeekBar seek = new SeekBar(this);
        seek.setMax(MAX_BOOST_DB - MIN_BOOST_DB);
        seek.setProgress(AudioDownmixBridge.getCenterBoostDb() - MIN_BOOST_DB);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int db = MIN_BOOST_DB + progress;
                boostValue.setText(db + " dB");
                if (fromUser) AudioDownmixBridge.setCenterBoostDb(db);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        root.addView(seek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(spacer(dp(32)));

        Button done = new Button(this);
        done.setText("Done");
        done.setOnClickListener(view -> finish());
        root.addView(done, new LinearLayout.LayoutParams(dp(220), dp(52)));

        return root;
    }

    private TextView label(String text, int sizeSp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private View spacer(int heightDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
