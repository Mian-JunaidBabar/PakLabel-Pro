package com.example.labelmaker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class LabelMakerFragment extends Fragment {

    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "LabelMakerPrefs";

    // A4 dimensions in points (1/72 inch)
    private static final float A4_WIDTH_POINTS = 595f;
    private static final float A4_HEIGHT_POINTS = 842f;

    private A4PreviewView a4PreviewView;
    private EditText textInput, rowsInput, columnsInput;
    private MaterialButton exportButton, textColorButton;
    private MaterialButton presetWhiteBtn, presetGrayBtn, presetBlackBtn;
    private MaterialButton btnSaveCurrentPreset;
    private ChipGroup chipGroupPresets;
    private Slider fontSizeSlider;
    private TextView fontSizeValue;

    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private float fontSize = 12f;

    private PresetManager presetManager;

    private ActivityResultLauncher<Intent> createPngLauncher;
    private ActivityResultLauncher<Intent> createPdfLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_label_maker, container, false);

        presetManager = new PresetManager(requireContext(), "LabelMakerPresets");

        initializeViews(view);
        loadConfiguration();
        setupListeners();
        updatePreview();
        loadPresetChips();

        registerLaunchers();

        return view;
    }

    private void registerLaunchers() {
        createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        createPdfDocument(uri);
                    }
                }
            }
        );

        createPngLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        savePng(uri);
                    }
                }
            }
        );
    }

    private void initializeViews(View view) {
        a4PreviewView = view.findViewById(R.id.a4_preview);
        textInput = view.findViewById(R.id.text_input);
        rowsInput = view.findViewById(R.id.rows_input);
        columnsInput = view.findViewById(R.id.columns_input);
        
        exportButton = view.findViewById(R.id.export_button);
        MaterialButton exportPngBtn = view.findViewById(R.id.btn_export_png);
        fontSizeSlider = view.findViewById(R.id.font_size_slider);
        fontSizeValue = view.findViewById(R.id.font_size_value);
        
        textColorButton = view.findViewById(R.id.text_color_button);
        presetWhiteBtn = view.findViewById(R.id.preset_white);
        presetGrayBtn = view.findViewById(R.id.preset_gray);
        presetBlackBtn = view.findViewById(R.id.preset_black);
        
        btnSaveCurrentPreset = view.findViewById(R.id.btn_save_current_preset);
        chipGroupPresets = view.findViewById(R.id.chip_group_presets);

        // Wire export PNG button
        if (exportPngBtn != null) {
            exportPngBtn.setOnClickListener(v -> exportToPng());
        }
    }

    private void setupListeners() {
        // Text change listeners for live preview
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        textInput.addTextChangedListener(textWatcher);
        rowsInput.addTextChangedListener(textWatcher);
        columnsInput.addTextChangedListener(textWatcher);

        // Font size control
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            fontSize = value;
            updateFontSizeDisplay();
            updatePreview();
        });
        
        // Text color toggle (keeps it simple: black / white for contrast)
        textColorButton.setOnClickListener(v -> {
            textColor = (textColor == Color.BLACK) ? Color.WHITE : Color.BLACK;
            updateButtonColor(textColorButton, textColor);
            updatePreview();
        });

        // Background preset buttons (white, gray, black)
        presetWhiteBtn.setOnClickListener(v -> {
            backgroundColor = Color.WHITE;
            updatePreview();
        });

        presetGrayBtn.setOnClickListener(v -> {
            backgroundColor = Color.parseColor("#BFBFBF");
            updatePreview();
        });

        presetBlackBtn.setOnClickListener(v -> {
            backgroundColor = Color.BLACK;
            updatePreview();
        });

        // Preset buttons
        btnSaveCurrentPreset.setOnClickListener(v -> showSavePresetDialog());

        // Export button
        exportButton.setOnClickListener(v -> exportToPdf());
    }
    
    private void updateButtonColor(MaterialButton button, int color) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }
    
    private void updateFontSizeDisplay() {
        // Use Locale.ROOT to avoid DefaultLocale lint warning
        fontSizeValue.setText(String.format(Locale.ROOT, "%.0fpx", fontSize));
    }

    private void updatePreview() {
        String text = textInput.getText().toString();
        boolean useTempText = text.isEmpty();
        if (useTempText) {
            text = "temp text";
        }
        
        int rows = parseIntOrDefault(rowsInput.getText().toString(), 10);
        int cols = parseIntOrDefault(columnsInput.getText().toString(), 3);

        // Validate and constrain values
        rows = Math.max(1, Math.min(50, rows));
        cols = Math.max(1, Math.min(20, cols));

        // Ensure preview and exports use a darker text color if grey-ish
        int effectiveTextColor = (textColor == Color.TRANSPARENT) ? Color.BLACK : textColor;
        if (useTempText) {
            effectiveTextColor = Color.TRANSPARENT;
        }

        // Only use solid background colors now
        a4PreviewView.setLabelData(text, rows, cols, fontSize, effectiveTextColor, backgroundColor);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void saveConfiguration() {
        SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("text", textInput.getText().toString());
        editor.putInt("rows", parseIntOrDefault(rowsInput.getText().toString(), 10));
        editor.putInt("cols", parseIntOrDefault(columnsInput.getText().toString(), 3));
        editor.putFloat("fontSize", fontSize);
        editor.putInt("textColor", textColor);
        editor.putInt("backgroundColor", backgroundColor);
        editor.apply();
        
        Toast.makeText(requireContext(), "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void loadConfiguration() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        String text = prefs.getString("text", "15mm Cable");
        int rows = prefs.getInt("rows", 10);
        int cols = prefs.getInt("cols", 3);
        fontSize = prefs.getFloat("fontSize", 12f);
        
        textInput.setText(text);
        rowsInput.setText(String.valueOf(rows));
        columnsInput.setText(String.valueOf(cols));
        
        textColor = prefs.getInt("textColor", Color.BLACK);
        backgroundColor = prefs.getInt("backgroundColor", Color.WHITE);

        fontSizeSlider.setValue(fontSize);
        updateFontSizeDisplay();
        
        updateButtonColor(textColorButton, textColor);
        updateButtonColor(presetWhiteBtn, Color.WHITE);
        updateButtonColor(presetGrayBtn, Color.parseColor("#BFBFBF"));
        updateButtonColor(presetBlackBtn, Color.BLACK);

        // No more gradient UI states to restore
    }

    // ==================== PRESET MANAGEMENT ====================

    private static class LabelPresetState {
        String text;
        int rows;
        int cols;
        float fontSize;
        int textColor;
        int backgroundColor;
    }

    private void showSavePresetDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Save Preset");

        TextInputLayout til = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint("Preset Name");
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        til.setPadding(padding, padding / 2, padding, padding / 2);

        TextInputEditText et = new TextInputEditText(til.getContext());
        til.addView(et);
        builder.setView(til);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = (et.getText() != null) ? et.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Preset name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            LabelPresetState state = new LabelPresetState();
            state.text = textInput.getText().toString();
            state.rows = parseIntOrDefault(rowsInput.getText().toString(), 10);
            state.cols = parseIntOrDefault(columnsInput.getText().toString(), 3);
            state.fontSize = fontSize;
            state.textColor = textColor;
            state.backgroundColor = backgroundColor;

            presetManager.savePreset(name, state);
            Toast.makeText(requireContext(), "Preset '" + name + "' saved!", Toast.LENGTH_SHORT).show();
            loadPresetChips();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadPresetChips() {
        chipGroupPresets.removeAllViews();
        List<String> names = presetManager.getAllPresetNames();
        for (String name : names) {
            Chip chip = new Chip(requireContext());
            chip.setText(name);
            chip.setCheckable(false);
            chip.setCloseIconVisible(true);

            chip.setOnClickListener(v -> {
                LabelPresetState state = presetManager.loadPreset(name, LabelPresetState.class);
                if (state != null) {
                    applyPresetState(state);
                    Toast.makeText(requireContext(), "Loaded preset: " + name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error loading preset", Toast.LENGTH_SHORT).show();
                }
            });

            chip.setOnCloseIconClickListener(v -> {
                presetManager.deletePreset(name);
                chipGroupPresets.removeView(chip);
                Toast.makeText(requireContext(), "Preset Deleted", Toast.LENGTH_SHORT).show();
            });

            chipGroupPresets.addView(chip);
        }
    }

    private void applyPresetState(LabelPresetState state) {
        textInput.setText(state.text);
        rowsInput.setText(String.valueOf(state.rows));
        columnsInput.setText(String.valueOf(state.cols));
        
        // Handle font size bounds correctly based on your slider
        if (state.fontSize >= 8 && state.fontSize <= 36) {
            fontSize = state.fontSize;
            fontSizeSlider.setValue(fontSize);
            updateFontSizeDisplay();
        }
        
        textColor = state.textColor;
        backgroundColor = state.backgroundColor;
        
        updateButtonColor(textColorButton, textColor);
        updatePreview();
    }


    private void exportToPdf() {
        // Validate inputs before export
        if (textInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter label text before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "cable_labels.pdf");

        createPdfLauncher.launch(intent);
    }

    private void exportToPng() {
        if (textInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter label text before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, "cable_labels_" + System.currentTimeMillis() + ".png");

        createPngLauncher.launch(intent);
    }

    private void createPdfDocument(Uri uri) {
        try {
            // Create PDF document
            PdfDocument document = new PdfDocument();
            
            // Create A4 page with exact dimensions (595 x 842 points)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                (int) A4_WIDTH_POINTS, 
                (int) A4_HEIGHT_POINTS, 
                1
            ).create();
            
            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw the label sheet at actual size
            a4PreviewView.drawToCanvas(page.getCanvas(), A4_WIDTH_POINTS, A4_HEIGHT_POINTS);

            document.finishPage(page);

            // Write to file
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                document.writeTo(os);
                document.close();
                
                Toast.makeText(requireContext(), "PDF exported successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void savePng(Uri uri) {
        try {
            // Render full A4-sized bitmap using the A4Preview drawing code scaled to pixels
            int pxWidth = 1240; // ~ 8.5in at 150dpi
            int pxHeight = 1754; // maintain aspect ratio

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(pxWidth, pxHeight, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            // Reuse preview drawing but scaled to pxWidth/pxHeight
            a4PreviewView.drawToCanvas(canvas, pxWidth, pxHeight);

            OutputStream os = null;
            try {
                os = requireContext().getContentResolver().openOutputStream(uri);
                if (os != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                } else {
                    throw new IOException("Unable to open output stream");
                }
            } finally {
                if (os != null) try { os.close(); } catch (IOException ignored) {}
            }

            Toast.makeText(requireContext(), "PNG exported successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("LabelMakerFragment", "Error exporting PNG", e);
            Toast.makeText(requireContext(), "Error exporting PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
