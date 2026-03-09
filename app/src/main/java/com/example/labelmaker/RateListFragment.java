package com.example.labelmaker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RateListFragment extends Fragment {

    // UI Components
    private MaterialButton btnSetupColumns;
    private LinearLayout headerRow;
    private TextView emptyMessage;
    private RecyclerView rateListRecycler;
    private LinearLayout rateListContainer;
    private MaterialButton btnExportPdf, btnExportPng;
    private MaterialButton btnSaveCurrentPreset;
    private ChipGroup chipGroupPresets;
    private ExtendedFloatingActionButton fabAddItem;
    private SeekBar seekbarFontSize, seekbarRowPadding, seekbarColumnWidth;
    private TextView fontSizeLabel, rowPaddingLabel, columnWidthLabel, tvA4Warning;
    private int selectedColumnIndex = -1;

    // Data
    private RateListAdapter adapter;
    private List<ColumnConfig> columns = new ArrayList<>();

    // Typography
    private float globalFontSize = 14f;
    private int globalRowPadding = 12;

    private PresetManager presetManager;

    // Background execution
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> createPdfLauncher;
    private ActivityResultLauncher<Intent> createPngLauncher;

    private static final String PREFS_NAME = "RateListPrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rate_list, container, false);

        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        presetManager = new PresetManager(requireContext(), "RateListPresets");

        registerResultLaunchers();
        initializeViews(view);
        loadPreferences();
        setupDefaultColumns();
        setupRecyclerView();
        setupListeners();
        loadPresetChips();
        rebuildHeaderRow();

        return view;
    }

    private void registerResultLaunchers() {
        createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) savePdfBackground(uri);
                }
            }
        );

        createPngLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) savePngBackground(uri);
                }
            }
        );
    }

    private void initializeViews(View view) {
        btnSetupColumns = view.findViewById(R.id.btn_setup_columns);
        headerRow = view.findViewById(R.id.header_row);
        emptyMessage = view.findViewById(R.id.empty_message);
        rateListRecycler = view.findViewById(R.id.rate_list_recycler);
        rateListContainer = view.findViewById(R.id.rate_list_container);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        btnExportPng = view.findViewById(R.id.btn_export_png);
        btnSaveCurrentPreset = view.findViewById(R.id.btn_save_current_preset);
        chipGroupPresets = view.findViewById(R.id.chip_group_presets);
        fabAddItem = view.findViewById(R.id.fab_add_item);
        seekbarFontSize = view.findViewById(R.id.seekbar_font_size);
        seekbarRowPadding = view.findViewById(R.id.seekbar_row_padding);
        seekbarColumnWidth = view.findViewById(R.id.seekbar_column_width);
        fontSizeLabel = view.findViewById(R.id.font_size_label);
        rowPaddingLabel = view.findViewById(R.id.row_padding_label);
        columnWidthLabel = view.findViewById(R.id.column_width_label);
        tvA4Warning = view.findViewById(R.id.tv_a4_warning);
    }

    private void loadPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        globalFontSize = prefs.getFloat("fontSize", 14f);
        globalRowPadding = prefs.getInt("rowPadding", 12);

        seekbarFontSize.setProgress((int) globalFontSize);
        seekbarRowPadding.setProgress(globalRowPadding);
        fontSizeLabel.setText((int) globalFontSize + "sp");
        rowPaddingLabel.setText(globalRowPadding + "dp");
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putFloat("fontSize", globalFontSize);
        editor.putInt("rowPadding", globalRowPadding);
        editor.apply();
    }

    private void setupDefaultColumns() {
        if (columns.isEmpty()) {
            columns.add(new ColumnConfig("Sr.No", 50));
            columns.add(new ColumnConfig("Description", 250));
            columns.add(new ColumnConfig("Price", 100));
        }
    }

    private void setupRecyclerView() {
        adapter = new RateListAdapter();
        adapter.setColumns(columns);
        adapter.setFontSize(globalFontSize);
        adapter.setRowPadding(globalRowPadding);
        rateListRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        rateListRecycler.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                RowModel deletedRow = adapter.getRows().get(position);
                adapter.getRows().remove(position);
                adapter.notifyItemRemoved(position);

                Snackbar snackbar = Snackbar.make(requireView(), "Item Deleted", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", v -> {
                    adapter.getRows().add(position, deletedRow);
                    adapter.notifyItemInserted(position);
                    rateListRecycler.scrollToPosition(position);
                });
                snackbar.show();
            }
        };
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(rateListRecycler);
    }

    private void setupListeners() {
        btnSetupColumns.setOnClickListener(v -> showColumnSetupDialog());

        fabAddItem.setOnClickListener(v -> {
            // Show a choice dialog: Add Product or Add Subheader
            new AlertDialog.Builder(requireContext())
                    .setTitle("Add Row")
                    .setItems(new String[]{"Add Product", "Add Category Header"}, (dialog, which) -> {
                        if (which == 0) {
                            showAddProductDialog();
                        } else {
                            showAddSubheaderDialog();
                        }
                    })
                    .show();
        });

        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnExportPng.setOnClickListener(v -> exportPng());
        btnSaveCurrentPreset.setOnClickListener(v -> showSavePresetDialog());

        // Typography seekbars
        seekbarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                globalFontSize = Math.max(8, progress);
                fontSizeLabel.setText((int) globalFontSize + "sp");
                adapter.setFontSize(globalFontSize);
                adapter.notifyDataSetChanged();
                rebuildHeaderRow();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { savePreferences(); }
        });

        seekbarRowPadding.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                globalRowPadding = Math.max(4, progress);
                rowPaddingLabel.setText(globalRowPadding + "dp");
                adapter.setRowPadding(globalRowPadding);
                adapter.notifyDataSetChanged();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { savePreferences(); }
        });

        seekbarColumnWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (selectedColumnIndex != -1 && selectedColumnIndex < columns.size()) {
                    columnWidthLabel.setText(String.valueOf(progress));
                    columns.get(selectedColumnIndex).setWidth(progress);
                    rebuildHeaderRow(); // Updates header widths instantly
                    adapter.notifyDataSetChanged(); // Updates all rows instantly
                    checkA4Constraints();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void checkA4Constraints() {
        if (columns == null) return;
        int totalWidth = 0;
        for (ColumnConfig col : columns) {
            totalWidth += col.getWidth();
        }
        if (totalWidth > 595) {
            tvA4Warning.setVisibility(View.VISIBLE);
        } else {
            tvA4Warning.setVisibility(View.GONE);
        }
    }

    // ==================== COLUMN SETUP ====================

    private void showColumnSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Setup Columns");

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(16), dp(24), dp(8));

        // Column count selector
        TextView countLabel = new TextView(requireContext());
        countLabel.setText("Number of columns: " + columns.size());
        countLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dialogLayout.addView(countLabel);

        SeekBar countSeek = new SeekBar(requireContext());
        countSeek.setMin(2);
        countSeek.setMax(8);
        countSeek.setProgress(columns.size());
        dialogLayout.addView(countSeek);

        // Container for column name fields
        LinearLayout fieldsContainer = new LinearLayout(requireContext());
        fieldsContainer.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.addView(fieldsContainer);

        // Populate fields for current count
        Runnable populateFields = () -> {
            fieldsContainer.removeAllViews();
            int count = countSeek.getProgress();
            for (int i = 0; i < count; i++) {
                TextInputLayout til = new TextInputLayout(requireContext(), null,
                        com.google.android.material.R.attr.textInputOutlinedStyle);
                til.setHint("Column " + (i + 1) + " Name");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                til.setLayoutParams(lp);

                TextInputEditText et = new TextInputEditText(til.getContext());
                et.setInputType(InputType.TYPE_CLASS_TEXT);
                if (i < columns.size()) {
                    et.setText(columns.get(i).getName());
                }
                et.setTag("col_name_" + i);
                til.addView(et);
                fieldsContainer.addView(til);
            }
        };

        populateFields.run();

        countSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                countLabel.setText("Number of columns: " + progress);
                populateFields.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setView(dialogLayout);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            int count = countSeek.getProgress();
            List<ColumnConfig> newColumns = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TextInputEditText et = fieldsContainer.findViewWithTag("col_name_" + i);
                String name = (et != null && et.getText() != null) ? et.getText().toString().trim() : "Col " + (i + 1);
                if (name.isEmpty()) name = "Col " + (i + 1);
                
                // Retain existing width if available, else default to 100
                int width = (i < columns.size()) ? columns.get(i).getWidth() : 100;
                
                newColumns.add(new ColumnConfig(name, width));
            }
            columns = newColumns;
            
            // Reset selection since column count changed
            selectedColumnIndex = -1;
            seekbarColumnWidth.setEnabled(false);
            columnWidthLabel.setText("-");

            adapter.setColumns(columns);
            adapter.clearRows();
            adapter.notifyDataSetChanged();
            rebuildHeaderRow();
            emptyMessage.setVisibility(View.VISIBLE);
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ==================== HEADER ROW ====================

    private void rebuildHeaderRow() {
        headerRow.removeAllViews();
        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig col = columns.get(i);
            int index = i;

            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(col.getWidth()), ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            tv.setText(col.getName());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, globalFontSize);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.parseColor("#006666"));
            tv.setPadding(dp(4), dp(8), dp(4), dp(8));
            tv.setGravity(Gravity.CENTER);

            // Highlight selected column
            if (index == selectedColumnIndex) {
                tv.setBackgroundColor(Color.parseColor("#B2DFDB")); // Darker teal highlight
            } else {
                tv.setBackgroundColor(Color.TRANSPARENT);
            }

            // Click to select
            tv.setOnClickListener(v -> {
                selectedColumnIndex = index;
                rebuildHeaderRow(); // Re-render highlights
                seekbarColumnWidth.setEnabled(true);
                seekbarColumnWidth.setProgress(col.getWidth());
                columnWidthLabel.setText(String.valueOf(col.getWidth()));
            });

            headerRow.addView(tv);
        }
        checkA4Constraints();
    }

    // ==================== ADD DIALOGS ====================

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Product");

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(16), dp(24), dp(8));

        List<TextInputEditText> inputFields = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            TextInputLayout til = new TextInputLayout(requireContext(), null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint(columns.get(i).getName());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            til.setLayoutParams(lp);

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            til.addView(et);
            dialogLayout.addView(til);
            inputFields.add(et);
        }

        builder.setView(dialogLayout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            List<String> values = new ArrayList<>();
            for (TextInputEditText et : inputFields) {
                String val = (et.getText() != null) ? et.getText().toString().trim() : "";
                values.add(val);
            }
            adapter.addRow(RowModel.createProduct(values));
            emptyMessage.setVisibility(View.GONE);
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showAddSubheaderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Category Header");

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextInputLayout til = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint("Category Name");
        TextInputEditText et = new TextInputEditText(til.getContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        til.addView(et);
        dialogLayout.addView(til);

        builder.setView(dialogLayout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String text = (et.getText() != null) ? et.getText().toString().trim() : "";
            if (!text.isEmpty()) {
                adapter.addRow(RowModel.createSubheader(text));
                emptyMessage.setVisibility(View.GONE);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ==================== PRESET MANAGEMENT ====================

    private static class RateListPresetState {
        List<ColumnConfig> columns;
        List<RowModel> rows;
        float fontSize;
        int rowPadding;
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

            RateListPresetState state = new RateListPresetState();
            state.columns = new ArrayList<>(columns);
            state.rows = new ArrayList<>(adapter.getRows());
            state.fontSize = globalFontSize;
            state.rowPadding = globalRowPadding;

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
                RateListPresetState state = presetManager.loadPreset(name, RateListPresetState.class);
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

    private void applyPresetState(RateListPresetState state) {
        if (state.columns != null && !state.columns.isEmpty()) {
            this.columns = state.columns;
            adapter.setColumns(this.columns);
            rebuildHeaderRow();
        }

        if (state.rows != null) {
            adapter.clearRows();
            for (RowModel row : state.rows) {
                adapter.addRow(row);
            }
        }

        if (state.fontSize >= 8 && state.fontSize <= 28) {
            globalFontSize = state.fontSize;
            seekbarFontSize.setProgress((int) globalFontSize);
        }

        if (state.rowPadding >= 4 && state.rowPadding <= 32) {
            globalRowPadding = state.rowPadding;
            seekbarRowPadding.setProgress(globalRowPadding);
        }

        emptyMessage.setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
        savePreferences();
    }

    // ==================== EXPORT ====================

    private void exportPdf() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(requireContext(), "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".pdf");
        createPdfLauncher.launch(intent);
    }

    private void exportPng() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(requireContext(), "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".png");
        createPngLauncher.launch(intent);
    }

    private void savePdfBackground(Uri uri) {
        Toast.makeText(requireContext(), "Exporting PDF in background...", Toast.LENGTH_SHORT).show();

        // Snapshot data on the main thread
        final List<RowModel> rowSnapshot = new ArrayList<>(adapter.getRows());
        final List<ColumnConfig> colSnapshot = new ArrayList<>(columns);
        final float fontSizeSnapshot = globalFontSize;

        executorService.execute(() -> {
            try {
                int pageWidth = 595;
                int pageHeight = 842;

                PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                drawRateListToCanvas(canvas, pageWidth, pageHeight, rowSnapshot, colSnapshot, fontSizeSnapshot);

                pdfDocument.finishPage(page);
                OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                pdfDocument.writeTo(os);
                os.close();
                pdfDocument.close();

                mainThreadHandler.post(() ->
                    Toast.makeText(requireContext(), "PDF saved successfully", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                mainThreadHandler.post(() ->
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void savePngBackground(Uri uri) {
        Toast.makeText(requireContext(), "Exporting PNG in background...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                Bitmap bitmap = Bitmap.createBitmap(rateListContainer.getWidth(), rateListContainer.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);

                mainThreadHandler.post(() -> {
                    rateListContainer.draw(canvas);
                    executorService.execute(() -> {
                        try {
                            OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                            os.close();
                            mainThreadHandler.post(() ->
                                Toast.makeText(requireContext(), "PNG saved successfully", Toast.LENGTH_LONG).show());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mainThreadHandler.post(() ->
                                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainThreadHandler.post(() ->
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // ==================== CANVAS DRAWING ====================

    private void drawRateListToCanvas(Canvas canvas, int pageWidth, int pageHeight,
                                       List<RowModel> rows, List<ColumnConfig> cols, float textSize) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int margin = 40;
        int currentY = margin;
        int usableWidth = pageWidth - 2 * margin;

        // Title
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Rate List", margin, currentY, paint);
        currentY += 40;

        // Header row bg
        int headerHeight = 30;
        paint.setColor(Color.parseColor("#E0F2F1"));
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + headerHeight, paint);

        // Header text
        paint.setColor(Color.parseColor("#006666"));
        paint.setTextSize(textSize);
        paint.setFakeBoldText(true);
        float currentX = margin + 8;
        for (int i = 0; i < cols.size(); i++) {
            ColumnConfig col = cols.get(i);
            canvas.drawText(col.getName(), currentX, currentY + headerHeight - 8, paint);
            currentX += col.getWidth();
        }
        currentY += headerHeight;

        // Line separator
        paint.setColor(Color.parseColor("#006666"));
        paint.setStrokeWidth(2);
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
        currentY += 5;

        // Data rows
        paint.setFakeBoldText(false);
        paint.setTextSize(textSize);
        int rowHeight = (int) (textSize * 2.2f);

        for (RowModel row : rows) {
            if (currentY > pageHeight - 60) break;

            if (row.getViewType() == RowModel.TYPE_SUBHEADER) {
                // Subheader: tinted bg, bold, full width
                paint.setColor(Color.parseColor("#E0F2F1"));
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint);
                paint.setColor(Color.parseColor("#006666"));
                paint.setFakeBoldText(true);
                paint.setTextSize(textSize + 2);
                canvas.drawText(row.getSubheaderText(), margin + 8, currentY + rowHeight - 8, paint);
                paint.setFakeBoldText(false);
                paint.setTextSize(textSize);
            } else {
                // Product row
                paint.setColor(Color.BLACK);
                currentX = margin + 8;
                List<String> vals = row.getCellValues();
                float y = currentY + rowHeight - 8;
                for (int i = 0; i < cols.size(); i++) {
                    ColumnConfig col = cols.get(i);
                    String cellText = (i < vals.size()) ? vals.get(i) : "";
                    paint.setColor(Color.DKGRAY);
                    int cellWidth = col.getWidth();

                    // For the last cell to stretch if needed, though width is explicit now
                    canvas.drawText(cellText, currentX, y, paint);
                    currentX += cellWidth;
                }
            }

            currentY += rowHeight;

            // Row separator
            paint.setColor(Color.LTGRAY);
            paint.setStrokeWidth(1);
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
            currentY += 3;
        }
    }

    // ==================== UTILITY ====================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }
}
