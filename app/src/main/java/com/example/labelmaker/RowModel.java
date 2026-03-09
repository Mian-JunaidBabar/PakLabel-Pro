package com.example.labelmaker;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single row in the dynamic rate list spreadsheet.
 * Can be either a Category Subheader or a Product data row.
 */
public class RowModel {

    public static final int TYPE_SUBHEADER = 0;
    public static final int TYPE_PRODUCT = 1;

    private int viewType;
    private String subheaderText;
    private List<String> cellValues;
    private int customBgColor = 0; // 0 = not set

    private RowModel() {
    }

    /**
     * Factory: creates a category subheader row.
     */
    public static RowModel createSubheader(String text) {
        RowModel row = new RowModel();
        row.viewType = TYPE_SUBHEADER;
        row.subheaderText = text;
        row.cellValues = new ArrayList<>();
        return row;
    }

    /**
     * Factory: creates a standard product row with dynamic cell values.
     */
    public static RowModel createProduct(List<String> values) {
        RowModel row = new RowModel();
        row.viewType = TYPE_PRODUCT;
        row.subheaderText = "";
        row.cellValues = new ArrayList<>(values);
        return row;
    }

    public int getViewType() {
        return viewType;
    }

    public String getSubheaderText() {
        return subheaderText;
    }

    public List<String> getCellValues() {
        return cellValues;
    }

    public int getCustomBgColor() {
        return customBgColor;
    }

    public void setCustomBgColor(int customBgColor) {
        this.customBgColor = customBgColor;
    }
}
