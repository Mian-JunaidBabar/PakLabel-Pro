package com.example.labelmaker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private Context context;
    private int initialColor;
    private OnColorSelectedListener listener;

    public ColorPickerDialog(Context context, int initialColor, OnColorSelectedListener listener) {
        this.context = context;
        this.initialColor = initialColor;
        this.listener = listener;
    }

    public void show() {
        // Create custom view for color picker
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);

        View colorPreview = dialogView.findViewById(R.id.color_preview);
        SeekBar redSeekBar = dialogView.findViewById(R.id.red_seekbar);
        SeekBar greenSeekBar = dialogView.findViewById(R.id.green_seekbar);
        SeekBar blueSeekBar = dialogView.findViewById(R.id.blue_seekbar);
        TextView redValue = dialogView.findViewById(R.id.red_value);
        TextView greenValue = dialogView.findViewById(R.id.green_value);
        TextView blueValue = dialogView.findViewById(R.id.blue_value);

        // Initialize with current color
        int red = Color.red(initialColor);
        int green = Color.green(initialColor);
        int blue = Color.blue(initialColor);

        redSeekBar.setProgress(red);
        greenSeekBar.setProgress(green);
        blueSeekBar.setProgress(blue);

        redValue.setText(String.valueOf(red));
        greenValue.setText(String.valueOf(green));
        blueValue.setText(String.valueOf(blue));

        colorPreview.setBackgroundColor(initialColor);

        // Setup seekbar listeners
        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = redSeekBar.getProgress();
                int g = greenSeekBar.getProgress();
                int b = blueSeekBar.getProgress();

                redValue.setText(String.valueOf(r));
                greenValue.setText(String.valueOf(g));
                blueValue.setText(String.valueOf(b));

                int color = Color.rgb(r, g, b);
                colorPreview.setBackgroundColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        redSeekBar.setOnSeekBarChangeListener(seekBarListener);
        greenSeekBar.setOnSeekBarChangeListener(seekBarListener);
        blueSeekBar.setOnSeekBarChangeListener(seekBarListener);

        // Preset color buttons
        View[] presetButtons = {
            dialogView.findViewById(R.id.preset_black),
            dialogView.findViewById(R.id.preset_white),
            dialogView.findViewById(R.id.preset_red),
            dialogView.findViewById(R.id.preset_green),
            dialogView.findViewById(R.id.preset_blue),
            dialogView.findViewById(R.id.preset_yellow),
            dialogView.findViewById(R.id.preset_cyan),
            dialogView.findViewById(R.id.preset_magenta)
        };

        int[] presetColors = {
            Color.BLACK,
            Color.WHITE,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA
        };

        for (int i = 0; i < presetButtons.length; i++) {
            int color = presetColors[i];
            presetButtons[i].setBackgroundColor(color);
            presetButtons[i].setOnClickListener(v -> {
                redSeekBar.setProgress(Color.red(color));
                greenSeekBar.setProgress(Color.green(color));
                blueSeekBar.setProgress(Color.blue(color));
            });
        }

        // Show dialog
        new MaterialAlertDialogBuilder(context)
            .setTitle("Pick a Color")
            .setView(dialogView)
            .setPositiveButton("Select", (dialog, which) -> {
                int selectedColor = Color.rgb(
                    redSeekBar.getProgress(),
                    greenSeekBar.getProgress(),
                    blueSeekBar.getProgress()
                );
                if (listener != null) {
                    listener.onColorSelected(selectedColor);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
