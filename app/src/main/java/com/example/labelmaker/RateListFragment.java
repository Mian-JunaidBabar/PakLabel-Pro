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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.EditText;
import android.widget.ScrollView;
import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private SeekBar seekbarFontSize, seekbarRowPadding, seekbarColumnWidth;
    private TextView fontSizeLabel, rowPaddingLabel, columnWidthLabel, tvA4Warning;
    private EditText etRateListTitle;
    private SwitchMaterial switchAutoSrNo;
    private boolean autoSrNo = false;
    private int selectedColumnIndex = -1;

    // Theme selector circles
    private ImageView themeTeal, themeBlue, themeWarm, themeGray;
    private int selectedThemeIndex = 0;

    // Theme palettes: [headerBg, rowBg, subheaderBg, fontColor]
    private static final int[][] THEME_PALETTES = {
        { Color.parseColor("#E0F2F1"), Color.parseColor("#F5F8F8"), Color.parseColor("#B2DFDB"), Color.BLACK },         // Teal
        { Color.parseColor("#BBDEFB"), Color.parseColor("#E3F2FD"), Color.parseColor("#90CAF9"), Color.parseColor("#0D47A1") }, // Blue
        { Color.parseColor("#FFCCBC"), Color.parseColor("#FFF3E0"), Color.parseColor("#FFAB91"), Color.parseColor("#BF360C") }, // Warm
        { Color.parseColor("#E0E0E0"), Color.parseColor("#F5F5F5"), Color.parseColor("#BDBDBD"), Color.BLACK }          // Gray
    };

    // Data
    private RateListAdapter adapter;
    private List<ColumnConfig> columns = new ArrayList<>();

    // Typography & Styling
    private float globalFontSize = 14f;
    private int globalRowPadding = 12;
    private int headerBgColor = Color.parseColor("#E0E0E0");
    private int rowBgColor = Color.parseColor("#F5F8F8");
    private int subheaderBgColor = Color.parseColor("#E0F2F1");
    private int fontColor = Color.BLACK;

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
        setupThemeSelector();
        setupToolbarMenu();
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
        seekbarFontSize = view.findViewById(R.id.seekbar_font_size);
        seekbarRowPadding = view.findViewById(R.id.seekbar_row_padding);
        seekbarColumnWidth = view.findViewById(R.id.seekbar_column_width);
        fontSizeLabel = view.findViewById(R.id.font_size_label);
        rowPaddingLabel = view.findViewById(R.id.row_padding_label);
        columnWidthLabel = view.findViewById(R.id.column_width_label);
        tvA4Warning = view.findViewById(R.id.tv_a4_warning);
        themeTeal = view.findViewById(R.id.theme_teal);
        themeBlue = view.findViewById(R.id.theme_blue);
        themeWarm = view.findViewById(R.id.theme_warm);
        themeGray = view.findViewById(R.id.theme_gray);
        etRateListTitle = view.findViewById(R.id.et_rate_list_title);
        switchAutoSrNo = view.findViewById(R.id.switch_auto_sr_no);
    }

    private void loadPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        globalFontSize = prefs.getFloat("fontSize", 14f);
        globalRowPadding = prefs.getInt("rowPadding", 12);
        headerBgColor = prefs.getInt("headerBgColor", Color.parseColor("#E0E0E0"));
        rowBgColor = prefs.getInt("rowBgColor", Color.parseColor("#F5F8F8"));
        subheaderBgColor = prefs.getInt("subheaderBgColor", Color.parseColor("#E0F2F1"));
        fontColor = prefs.getInt("fontColor", Color.BLACK);
        autoSrNo = prefs.getBoolean("autoSrNo", false);

        String savedTitle = prefs.getString("rateListTitle", "Meher Cables Rate List");
        etRateListTitle.setText(savedTitle);
        switchAutoSrNo.setChecked(autoSrNo);

        seekbarFontSize.setProgress((int) globalFontSize);
        seekbarRowPadding.setProgress(globalRowPadding);
        fontSizeLabel.setText((int) globalFontSize + "sp");
        rowPaddingLabel.setText(globalRowPadding + "dp");
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putFloat("fontSize", globalFontSize);
        editor.putInt("rowPadding", globalRowPadding);
        editor.putInt("headerBgColor", headerBgColor);
        editor.putInt("rowBgColor", rowBgColor);
        editor.putInt("subheaderBgColor", subheaderBgColor);
        editor.putInt("fontColor", fontColor);
        editor.putInt("themeIndex", selectedThemeIndex);
        editor.putBoolean("autoSrNo", autoSrNo);
        editor.putString("rateListTitle", etRateListTitle.getText().toString().trim());
        editor.apply();
    }

    private void setupDefaultColumns() {
        if (columns.isEmpty()) {
            columns.add(new ColumnConfig("Cable Name", 200));
            columns.add(new ColumnConfig("Old Rates", 150));
            columns.add(new ColumnConfig("New Rates", 150));
        }
    }

    private void setupRecyclerView() {
        adapter = new RateListAdapter();
        adapter.setColumns(columns);
        adapter.setFontSize(globalFontSize);
        adapter.setRowPadding(globalRowPadding);
        adapter.setRowBgColor(rowBgColor);
        adapter.setSubheaderBgColor(subheaderBgColor);
        adapter.setFontColor(fontColor);
        adapter.setAutoSrNo(autoSrNo);
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

        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnExportPng.setOnClickListener(v -> exportPng());
        btnSaveCurrentPreset.setOnClickListener(v -> showSavePresetDialog());

        switchAutoSrNo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoSrNo = isChecked;
            if (autoSrNo) {
                // Ensure Sr. No is the first column
                boolean hasSrNo = false;
                if (!columns.isEmpty() && columns.get(0).getName().equals("Sr. No")) {
                    hasSrNo = true;
                }
                if (!hasSrNo) {
                    columns.add(0, new ColumnConfig("Sr. No", 50));
                    // Shift existing row data to right
                    for (RowModel row : adapter.getRows()) {
                        if (row.getViewType() == RowModel.TYPE_PRODUCT) {
                            row.getCellValues().add(0, "");
                        }
                    }
                }
            } else {
                // Remove Sr. No if it's the first column
                if (!columns.isEmpty() && columns.get(0).getName().equals("Sr. No")) {
                    columns.remove(0);
                    // Shift existing row data to left
                    for (RowModel row : adapter.getRows()) {
                        if (row.getViewType() == RowModel.TYPE_PRODUCT && !row.getCellValues().isEmpty()) {
                            row.getCellValues().remove(0);
                        }
                    }
                }
            }
            adapter.setAutoSrNo(autoSrNo);
            adapter.setColumns(columns);
            rebuildHeaderRow();
            adapter.notifyDataSetChanged();
            savePreferences();
        });

        adapter.setOnRowClickListener((position, row) -> {
            showEditRowDialog(position, row);
        });

        // Typography seekbars
        seekbarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (selectedColumnIndex != -1) {
                    float newSize = Math.max(8f, progress);
                    columns.get(selectedColumnIndex).setCustomFontSize(newSize);
                    fontSizeLabel.setText((int) newSize + "sp");
                } else {
                    globalFontSize = Math.max(8f, progress);
                    fontSizeLabel.setText((int) globalFontSize + "sp");
                }
                adapter.setFontSize(globalFontSize);
                adapter.setColumns(columns);
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
                if (!fromUser || selectedColumnIndex == -1 || selectedColumnIndex >= columns.size()) return;

                int currentTotal = 0;
                for (int i=0; i<columns.size(); i++) {
                    if (i != selectedColumnIndex) currentTotal += columns.get(i).getWidth();
                }

                if (currentTotal + progress > 595) {
                    progress = 595 - currentTotal;
                    seekBar.setProgress(progress);
                    Toast.makeText(requireContext(), "Maximum A4 width (595pts) reached", Toast.LENGTH_SHORT).show();
                }

                columnWidthLabel.setText(String.valueOf(progress));
                columns.get(selectedColumnIndex).setWidth(progress);
                rebuildHeaderRow(); 
                adapter.notifyDataSetChanged(); 
                checkA4Constraints();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupThemeSelector() {
        // Load saved theme index
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        selectedThemeIndex = prefs.getInt("themeIndex", 0);
        applyTheme(selectedThemeIndex);
        highlightThemeCircle(selectedThemeIndex);

        View.OnClickListener themeClickListener = v -> {
            int index = 0;
            int id = v.getId();
            if (id == R.id.theme_teal) index = 0;
            else if (id == R.id.theme_blue) index = 1;
            else if (id == R.id.theme_warm) index = 2;
            else if (id == R.id.theme_gray) index = 3;

            selectedThemeIndex = index;
            applyTheme(index);
            highlightThemeCircle(index);
            savePreferences();
        };

        themeTeal.setOnClickListener(themeClickListener);
        themeBlue.setOnClickListener(themeClickListener);
        themeWarm.setOnClickListener(themeClickListener);
        themeGray.setOnClickListener(themeClickListener);
    }

    private void applyTheme(int index) {
        int[] palette = THEME_PALETTES[index];
        headerBgColor = palette[0];
        rowBgColor = palette[1];
        subheaderBgColor = palette[2];
        fontColor = palette[3];

        adapter.setRowBgColor(rowBgColor);
        adapter.setSubheaderBgColor(subheaderBgColor);
        adapter.setFontColor(fontColor);
        adapter.notifyDataSetChanged();
        rebuildHeaderRow();
    }

    private void highlightThemeCircle(int index) {
        ImageView[] circles = { themeTeal, themeBlue, themeWarm, themeGray };
        int[] backgrounds = { R.drawable.circle_teal, R.drawable.circle_blue, R.drawable.circle_warm, R.drawable.circle_gray };
        for (int i = 0; i < circles.length; i++) {
            if (i == index) {
                circles[i].setForeground(getResources().getDrawable(R.drawable.circle_selected_ring, null));
            } else {
                circles[i].setForeground(null);
            }
        }
    }

    private void setupToolbarMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_rate_list, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_add_item) {
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
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showEditRowDialog(int position, RowModel row) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        boolean isSubheader = (row.getViewType() == RowModel.TYPE_SUBHEADER);
        builder.setTitle(isSubheader ? "Edit Category" : "Edit Row");

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(16), dp(24), dp(8));

        List<TextInputEditText> inputFields = new ArrayList<>();

        if (isSubheader) {
            TextInputLayout til = new TextInputLayout(requireContext(), null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint("Category Name");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            til.setLayoutParams(lp);

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            et.setText(row.getSubheaderText());
            til.addView(et);
            dialogLayout.addView(til);
            inputFields.add(et);
        } else {
            List<String> values = row.getCellValues();
            for (int i = 0; i < columns.size(); i++) {
                ColumnConfig col = columns.get(i);
                // Skip entry for Auto Sr. No
                if (autoSrNo && i == 0 && "Sr. No".equals(col.getName())) {
                    continue;
                }

                TextInputLayout til = new TextInputLayout(requireContext(), null,
                        com.google.android.material.R.attr.textInputOutlinedStyle);
                til.setHint(col.getName());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                til.setLayoutParams(lp);

                TextInputEditText et = new TextInputEditText(til.getContext());
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                
                String existingVal = "";
                if (i < values.size()) {
                    existingVal = values.get(i);
                }
                et.setText(existingVal);
                
                til.addView(et);
                dialogLayout.addView(til);
                inputFields.add(et);
            }
        }

        scrollView.addView(dialogLayout);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            if (isSubheader) {
                String text = (inputFields.get(0).getText() != null) ? inputFields.get(0).getText().toString().trim() : "";
                row.setSubheaderText(text);
            } else {
                List<String> newValues = new ArrayList<>();
                int inputIdx = 0;
                for (int i = 0; i < columns.size(); i++) {
                    if (autoSrNo && i == 0 && "Sr. No".equals(columns.get(i).getName())) {
                        newValues.add(""); // Placeholder
                    } else if (inputIdx < inputFields.size()) {
                        TextInputEditText et = inputFields.get(inputIdx++);
                        String val = (et.getText() != null) ? et.getText().toString().trim() : "";
                        newValues.add(val);
                    }
                }
                row.getCellValues().clear();
                row.getCellValues().addAll(newValues);
            }
            adapter.notifyItemChanged(position);
            
            // Rebuild rows if row impacts something globally, though usually not.
            if(autoSrNo && !isSubheader) adapter.notifyDataSetChanged(); // Sr numbers might shift if positions change but editing doesn't change position.
        });

        builder.setNeutralButton("Remove", (dialog, which) -> {
            adapter.getRows().remove(position);
            adapter.notifyItemRemoved(position);
            
            if (adapter.getRows().isEmpty()) {
                emptyMessage.setVisibility(View.VISIBLE);
            }
            
            if (autoSrNo) adapter.notifyDataSetChanged(); // update Sr No counts
            
            Snackbar snackbar = Snackbar.make(requireView(), "Row Removed", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", v -> {
                adapter.getRows().add(position, row);
                adapter.notifyItemInserted(position);
                emptyMessage.setVisibility(View.GONE);
                if (autoSrNo) adapter.notifyDataSetChanged();
                rateListRecycler.scrollToPosition(position);
            });
            snackbar.show();
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private void showColorPickerDialog(String title, int defaultColor, OnColorSelectedListener listener) {
        final int[] colors = {
            -1, // Special value for Clear/Default
            Color.WHITE, Color.LTGRAY, Color.DKGRAY, Color.BLACK,
            Color.parseColor("#E0E0E0"), Color.parseColor("#E0F2F1"), Color.parseColor("#B2DFDB"), Color.parseColor("#006666"),
            Color.parseColor("#F5F8F8"), Color.parseColor("#FFCCBC"), Color.parseColor("#FFCDD2"),
            Color.parseColor("#D1C4E9"), Color.parseColor("#C5CAE9")
        };
        final String[] names = {
            "Clear/Default Color",
            "White", "Light Gray", "Dark Gray", "Black",
            "Classic Gray", "Light Teal", "Teal Highlight", "Dark Teal",
            "Off White (Teal)", "Light Orange", "Light Red",
            "Light Purple", "Light Blue"
        };
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(names, (dialog, which) -> {
                listener.onColorSelected(colors[which]);
            })
            .show();
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

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(dialogLayout);

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
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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

        builder.setView(scrollView);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            int count = countSeek.getProgress();
            List<ColumnConfig> newColumns = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TextInputEditText et = fieldsContainer.findViewWithTag("col_name_" + i);
                String name = (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
                
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
        headerRow.setBackgroundColor(headerBgColor);
        
        // Add click listener to the container to deselect
        headerRow.setOnClickListener(v -> {
            selectedColumnIndex = -1;
            seekbarColumnWidth.setEnabled(false);
            columnWidthLabel.setText("-");
            rebuildHeaderRow();
        });
        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig col = columns.get(i);
            int index = i;

            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(col.getWidth()), ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            tv.setText(col.getName());
            float headerFontSize = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : globalFontSize;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerFontSize);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(fontColor);
            tv.setPadding(dp(4), dp(8), dp(4), dp(8));
            tv.setGravity(Gravity.CENTER);

            // Highlight selected column
            if (index == selectedColumnIndex) {
                tv.setBackgroundColor(Color.parseColor("#B2DFDB")); // Darker teal highlight
            } else {
                tv.setBackgroundColor(Color.TRANSPARENT);
            }

            // Click to select/deselect
            tv.setOnClickListener(v -> {
                if (selectedColumnIndex == index) {
                    selectedColumnIndex = -1;
                    seekbarColumnWidth.setEnabled(false);
                    columnWidthLabel.setText("-");
                    seekbarFontSize.setProgress((int) globalFontSize);
                    fontSizeLabel.setText((int) globalFontSize + "sp");
                } else {
                    selectedColumnIndex = index;
                    seekbarColumnWidth.setEnabled(true);
                    seekbarColumnWidth.setProgress(col.getWidth());
                    columnWidthLabel.setText(String.valueOf(col.getWidth()));
                    float currentFs = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : globalFontSize;
                    seekbarFontSize.setProgress((int) currentFs);
                    fontSizeLabel.setText((int) currentFs + "sp");
                }
                rebuildHeaderRow(); // Re-render highlights
            });

            headerRow.addView(tv);
        }
        checkA4Constraints();
    }

    // ==================== ADD DIALOGS ====================

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Product");

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(16), dp(24), dp(8));

        List<TextInputEditText> inputFields = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig col = columns.get(i);
            // Skip entry for Auto Sr. No
            if (autoSrNo && i == 0 && "Sr. No".equals(col.getName())) {
                continue;
            }

            TextInputLayout til = new TextInputLayout(requireContext(), null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint(col.getName());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            til.setLayoutParams(lp);

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            til.addView(et);
            dialogLayout.addView(til);
            inputFields.add(et);
        }

        scrollView.addView(dialogLayout);
        builder.setView(scrollView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            List<String> values = new ArrayList<>();
            int inputIdx = 0;
            for (int i = 0; i < columns.size(); i++) {
                if (autoSrNo && i == 0 && "Sr. No".equals(columns.get(i).getName())) {
                    values.add(""); // Placeholder for auto-numbered cell
                } else if (inputIdx < inputFields.size()) {
                    TextInputEditText et = inputFields.get(inputIdx++);
                    String val = (et.getText() != null) ? et.getText().toString().trim() : "";
                    values.add(val);
                }
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
        int headerBgColor;
        int rowBgColor;
        int subheaderBgColor;
        int fontColor;
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
            state.headerBgColor = headerBgColor;
            state.rowBgColor = rowBgColor;
            state.subheaderBgColor = subheaderBgColor;
            state.fontColor = fontColor;

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

        if (state.headerBgColor != 0) {
            headerBgColor = state.headerBgColor;
            headerRow.setBackgroundColor(headerBgColor);
        }

        if (state.rowBgColor != 0) {
            rowBgColor = state.rowBgColor;
            adapter.setRowBgColor(rowBgColor);
        }

        if (state.subheaderBgColor != 0) {
            subheaderBgColor = state.subheaderBgColor;
            adapter.setSubheaderBgColor(subheaderBgColor);
        }

        if (state.fontColor != 0) {
            fontColor = state.fontColor;
            adapter.setFontColor(fontColor);
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
                int neededWidth = 2 * 40;
                for (ColumnConfig c : colSnapshot) {
                    neededWidth += c.getWidth();
                }
                int pageWidth = Math.max(595, neededWidth);
                int pageHeight = 842;

                PdfDocument pdfDocument = new PdfDocument();
                
                generateMultiPagePdf(pdfDocument, pageWidth, pageHeight, rowSnapshot, colSnapshot, fontSizeSnapshot);

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

        final List<RowModel> rowSnapshot = new ArrayList<>(adapter.getRows());
        final List<ColumnConfig> colSnapshot = new ArrayList<>(columns);
        final float fontSizeSnapshot = globalFontSize;

        executorService.execute(() -> {
            try {
                int neededWidth = 2 * 40;
                for (ColumnConfig c : colSnapshot) {
                    neededWidth += c.getWidth();
                }
                int pageWidth = Math.max(595, neededWidth);

                int margin = 40;
                int currentY = margin;
                int headerHeight = 30;
                int rowHeight = (int) (fontSizeSnapshot * 2.2f);

                int totalHeight = margin * 2 + 50 + headerHeight + 5; // title + header
                for (RowModel r : rowSnapshot) {
                    totalHeight += rowHeight + 3; // row height + separator
                }

                Bitmap bitmap = Bitmap.createBitmap(pageWidth, totalHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                currentY = drawPdfHeader(canvas, paint, currentY, margin, pageWidth, colSnapshot, fontSizeSnapshot);

                paint.setFakeBoldText(false);
                paint.setTextSize(fontSizeSnapshot);

                int srNoCount = 0;
                for (int r = 0; r < rowSnapshot.size(); r++) {
                    RowModel row = rowSnapshot.get(r);

                    if (row.getViewType() == RowModel.TYPE_SUBHEADER) {
                        int subBgColor = row.getCustomBgColor() != 0 ? row.getCustomBgColor() : subheaderBgColor;
                        paint.setColor(subBgColor);
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint);
                        paint.setColor(Color.parseColor("#006666"));
                        paint.setFakeBoldText(true);
                        paint.setTextSize(fontSizeSnapshot + 2);
                        paint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText(row.getSubheaderText(), margin + 8, currentY + rowHeight - 8, paint);
                        paint.setFakeBoldText(false);
                        paint.setTextSize(fontSizeSnapshot);
                    } else {
                        srNoCount++;
                        int rBgColor = rowBgColor;
                        if (row.getCustomBgColor() != 0) {
                            rBgColor = row.getCustomBgColor();
                        } else if (r % 2 == 0) {
                            rBgColor = Color.WHITE;
                        }
                        paint.setColor(rBgColor);
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint);

                        paint.setColor(fontColor);
                        float currentX = margin;
                        List<String> vals = row.getCellValues();
                        float y = currentY + rowHeight - 8;

                        paint.setTextAlign(Paint.Align.CENTER);
                        for (int i = 0; i < colSnapshot.size(); i++) {
                            ColumnConfig col = colSnapshot.get(i);
                            String cellText = "";
                            if (autoSrNo && i == 0 && "Sr. No".equals(col.getName())) {
                                cellText = String.valueOf(srNoCount);
                            } else if (i < vals.size()) {
                                cellText = vals.get(i);
                            }
                            float cellFs = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : fontSizeSnapshot;
                            paint.setTextSize(cellFs);
                            canvas.drawText(cellText, currentX + col.getWidth() / 2f, y, paint);
                            currentX += col.getWidth();
                        }
                    }
                    currentY += rowHeight;
                    paint.setColor(Color.LTGRAY);
                    paint.setStrokeWidth(1);
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
                    currentY += 3;
                }

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
    }

    // ==================== CANVAS DRAWING ====================

    private void generateMultiPagePdf(PdfDocument pdfDocument, int pageWidth, int pageHeight,
                                      List<RowModel> rows, List<ColumnConfig> cols, float textSize) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int margin = 40;
        int currentY = margin;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        currentY = drawPdfHeader(canvas, paint, currentY, margin, pageWidth, cols, textSize);

        // Data rows
        paint.setFakeBoldText(false);
        paint.setTextSize(textSize);
        int rowHeight = (int) (textSize * 2.2f);

        for (int r = 0; r < rows.size(); r++) {
            RowModel row = rows.get(r);
            
            // Check for page break
            if (currentY + rowHeight > 780) {
                pdfDocument.finishPage(page);
                
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);
                
                currentY = margin;
                currentY = drawPdfHeader(canvas, paint, currentY, margin, pageWidth, cols, textSize);
                
                paint.setFakeBoldText(false);
                paint.setTextSize(textSize);
            }

            if (row.getViewType() == RowModel.TYPE_SUBHEADER) {
                // Subheader: tinted bg, bold, full width
                int subBgColor = row.getCustomBgColor() != 0 ? row.getCustomBgColor() : subheaderBgColor;
                paint.setColor(subBgColor);
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint);
                paint.setColor(Color.parseColor("#006666"));
                paint.setFakeBoldText(true);
                paint.setTextSize(textSize + 2);
                canvas.drawText(row.getSubheaderText(), margin + 8, currentY + rowHeight - 8, paint);
                paint.setFakeBoldText(false);
                paint.setTextSize(textSize);
            } else {
                // Product row bg
                int rBgColor = rowBgColor;
                if (row.getCustomBgColor() != 0) {
                    rBgColor = row.getCustomBgColor();
                } else if (r % 2 == 0) {
                    rBgColor = Color.WHITE;
                }
                paint.setColor(rBgColor);
                
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint);

                paint.setColor(fontColor);
                float currentX = margin + 8;
                List<String> vals = row.getCellValues();
                float y = currentY + rowHeight - 8;
                
                // Track serial number for PDF
                int srNoCount = 0;
                for (int j = 0; j <= r; j++) {
                    if (rows.get(j).getViewType() == RowModel.TYPE_PRODUCT) {
                        srNoCount++;
                    }
                }

                for (int i = 0; i < cols.size(); i++) {
                    ColumnConfig col = cols.get(i);
                    String cellText = "";
                    
                    if (autoSrNo && i == 0 && "Sr. No".equals(col.getName())) {
                        cellText = String.valueOf(srNoCount);
                    } else if (i < vals.size()) {
                        cellText = vals.get(i);
                    }
                    float cellFs = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : textSize;
                    paint.setTextSize(cellFs);
                    
                    canvas.drawText(cellText, currentX, y, paint);
                    currentX += col.getWidth();
                }
            }

            currentY += rowHeight;

            // Row separator
            paint.setColor(Color.LTGRAY);
            paint.setStrokeWidth(1);
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
            currentY += 3;
        }

        // Finish the last page
        pdfDocument.finishPage(page);
    }

    private int drawPdfHeader(Canvas canvas, Paint paint, int currentY, int margin, int pageWidth, List<ColumnConfig> cols, float textSize) {
        // Title
        String title = etRateListTitle.getText().toString().trim();
        if (title.isEmpty()) title = "Meher Cables Rate List";
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        // Center the title
        float titleWidth = paint.measureText(title);
        float titleX = (pageWidth - titleWidth) / 2f;
        canvas.drawText(title, titleX, currentY, paint);
        currentY += 40;

        // Header row bg
        int headerHeight = 30;
        paint.setColor(headerBgColor);
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + headerHeight, paint);

        // Header text
        paint.setColor(fontColor);
        paint.setFakeBoldText(true);
        float currentX = margin + 8;
        for (int i = 0; i < cols.size(); i++) {
            ColumnConfig col = cols.get(i);
            float headerFs = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : textSize;
            paint.setTextSize(headerFs);
            canvas.drawText(col.getName(), currentX, currentY + headerHeight - 8, paint);
            currentX += col.getWidth();
        }
        currentY += headerHeight;

        // Line separator
        paint.setColor(Color.parseColor("#006666"));
        paint.setStrokeWidth(2);
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
        currentY += 5;

        return currentY;
    }

    // ==================== UTILITY ====================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }
}
