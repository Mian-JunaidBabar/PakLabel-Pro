package com.example.labelmaker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "LabelMakerPrefs";

    // A4 dimensions in points (1/72 inch)
    private static final float A4_WIDTH_POINTS = 595f;
    private static final float A4_HEIGHT_POINTS = 842f;

    private DrawerLayout drawerLayout;
    private A4PreviewView a4PreviewView;
    private EditText textInput, rowsInput, columnsInput, fontSizeInput;
    private MaterialButton textColorButton, startColorButton, endColorButton, exportButton;
    private Toolbar toolbar;

    private int textColor = Color.BLACK;
    private int startColor = Color.WHITE;
    private int endColor = Color.LTGRAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupToolbar();
        loadConfiguration();
        setupListeners();
        setupBackPressHandler();
        updatePreview();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        a4PreviewView = findViewById(R.id.a4_preview);
        textInput = findViewById(R.id.text_input);
        rowsInput = findViewById(R.id.rows_input);
        columnsInput = findViewById(R.id.columns_input);
        fontSizeInput = findViewById(R.id.font_size_input);
        textColorButton = findViewById(R.id.text_color_button);
        startColorButton = findViewById(R.id.start_color_button);
        endColorButton = findViewById(R.id.end_color_button);
        exportButton = findViewById(R.id.export_button);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
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
        fontSizeInput.addTextChangedListener(textWatcher);

        // Color selection buttons
        textColorButton.setOnClickListener(v -> selectTextColor());
        startColorButton.setOnClickListener(v -> selectStartColor());
        endColorButton.setOnClickListener(v -> selectEndColor());
        
        // Export button
        exportButton.setOnClickListener(v -> exportToPdf());

        // Navigation drawer
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_save_labels) {
                saveConfiguration();
                Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (isEnabled()) {
                        setEnabled(false);
                        MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    private void selectTextColor() {
        new ColorPickerDialog(this, textColor, color -> {
            textColor = color;
            updateButtonColor(textColorButton, color);
            updatePreview();
        }).show();
    }

    private void selectStartColor() {
        new ColorPickerDialog(this, startColor, color -> {
            startColor = color;
            updateButtonColor(startColorButton, color);
            updatePreview();
        }).show();
    }

    private void selectEndColor() {
        new ColorPickerDialog(this, endColor, color -> {
            endColor = color;
            updateButtonColor(endColorButton, color);
            updatePreview();
        }).show();
    }

    private void updateButtonColor(MaterialButton button, int color) {
        // Update button to show selected color as icon tint
        button.setIconTint(android.content.res.ColorStateList.valueOf(color));
    }

    private void updatePreview() {
        String text = textInput.getText().toString();
        int rows = parseIntOrDefault(rowsInput.getText().toString(), 10);
        int cols = parseIntOrDefault(columnsInput.getText().toString(), 3);
        float fontSize = parseFloatOrDefault(fontSizeInput.getText().toString(), 12f);

        // Validate and constrain values
        rows = Math.max(1, Math.min(50, rows));
        cols = Math.max(1, Math.min(20, cols));
        fontSize = Math.max(4f, Math.min(72f, fontSize));

        a4PreviewView.setLabelData(text, rows, cols, fontSize, textColor, startColor, endColor);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private float parseFloatOrDefault(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void saveConfiguration() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("text", textInput.getText().toString());
        editor.putInt("rows", parseIntOrDefault(rowsInput.getText().toString(), 10));
        editor.putInt("cols", parseIntOrDefault(columnsInput.getText().toString(), 3));
        editor.putFloat("fontSize", parseFloatOrDefault(fontSizeInput.getText().toString(), 12f));
        editor.putInt("textColor", textColor);
        editor.putInt("startColor", startColor);
        editor.putInt("endColor", endColor);
        editor.apply();
    }

    private void loadConfiguration() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        String text = prefs.getString("text", "");
        int rows = prefs.getInt("rows", 10);
        int cols = prefs.getInt("cols", 3);
        float fontSize = prefs.getFloat("fontSize", 12f);
        
        textInput.setText(text);
        rowsInput.setText(String.valueOf(rows));
        columnsInput.setText(String.valueOf(cols));
        fontSizeInput.setText(String.valueOf((int) fontSize));
        
        textColor = prefs.getInt("textColor", Color.BLACK);
        startColor = prefs.getInt("startColor", Color.WHITE);
        endColor = prefs.getInt("endColor", Color.LTGRAY);

        // Update button colors
        updateButtonColor(textColorButton, textColor);
        updateButtonColor(startColorButton, startColor);
        updateButtonColor(endColorButton, endColor);
    }

    private void exportToPdf() {
        // Validate inputs before export
        if (textInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter label text before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "cable_labels.pdf");

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    createPdfDocument(uri);
                }
            }
        }
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
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                document.writeTo(os);
                document.close();
                
                Toast.makeText(this, "PDF exported successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
