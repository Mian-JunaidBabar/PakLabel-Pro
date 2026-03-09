package com.example.labelmaker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RateListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<RowModel> rows = new ArrayList<>();
    private List<ColumnConfig> columns = new ArrayList<>();
    private float fontSize = 14f;   // sp
    private int rowPadding = 12;    // dp
    private int rowBgColor = Color.parseColor("#F5F8F8"); // default alternating color
    private int fontColor = Color.BLACK; // default alternating color

    // --- Data setters ---

    public void setColumns(List<ColumnConfig> columns) {
        this.columns = columns;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setRowPadding(int rowPadding) {
        this.rowPadding = rowPadding;
    }

    public void setRowBgColor(int color) {
        this.rowBgColor = color;
    }

    public void setFontColor(int color) {
        this.fontColor = color;
    }

    public void addRow(RowModel row) {
        rows.add(row);
        notifyItemInserted(rows.size() - 1);
    }

    public void clearRows() {
        int size = rows.size();
        rows.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<RowModel> getRows() {
        return rows;
    }

    public List<ColumnConfig> getColumns() {
        return columns;
    }

    // --- ViewType ---

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).getViewType();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    // --- ViewHolder creation ---

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == RowModel.TYPE_SUBHEADER) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new SubheaderViewHolder(tv);
        } else {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setGravity(Gravity.CENTER_VERTICAL);
            return new ProductViewHolder(row);
        }
    }

    // --- Binding ---

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowModel row = rows.get(position);

        if (holder instanceof SubheaderViewHolder) {
            bindSubheader((SubheaderViewHolder) holder, row);
        } else if (holder instanceof ProductViewHolder) {
            bindProduct((ProductViewHolder) holder, row, position);
        }
    }

    private void bindSubheader(SubheaderViewHolder holder, RowModel row) {
        TextView tv = holder.textView;
        tv.setText(row.getSubheaderText());
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize + 2);
        int pad = dpToPx(tv, rowPadding);
        tv.setPadding(dpToPx(tv, 16), pad, dpToPx(tv, 16), pad);
        tv.setBackgroundColor(Color.parseColor("#E0F2F1")); // light teal tint
        tv.setTextColor(Color.parseColor("#006666"));
    }

    private void bindProduct(ProductViewHolder holder, RowModel row, int position) {
        LinearLayout container = holder.container;
        container.removeAllViews();

        int pad = dpToPx(container, rowPadding);
        container.setPadding(dpToPx(container, 16), pad, dpToPx(container, 16), pad);

        // Alternating row colors
        if (position % 2 == 0) {
            container.setBackgroundColor(Color.WHITE);
        } else {
            container.setBackgroundColor(rowBgColor);
        }

        List<String> values = row.getCellValues();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig col = columns.get(i);

            TextView cell = new TextView(container.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dpToPx(cell, col.getWidth()), ViewGroup.LayoutParams.WRAP_CONTENT);
            cell.setLayoutParams(lp);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            cell.setTextColor(Color.parseColor("#1C1B1F"));

            // First column left-aligned, rest right-aligned
            if (i == 0) {
                cell.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                cell.setPadding(0, 0, dpToPx(cell, 8), 0);
            } else {
                cell.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                cell.setPadding(dpToPx(cell, 4), 0, dpToPx(cell, 4), 0);
            }

            // Populate with data or empty string
            if (i < values.size()) {
                cell.setText(values.get(i));
            } else {
                cell.setText("");
            }

            container.addView(cell);
        }
    }

    private int dpToPx(View view, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                view.getContext().getResources().getDisplayMetrics());
    }

    // --- ViewHolders ---

    static class SubheaderViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        SubheaderViewHolder(TextView tv) {
            super(tv);
            this.textView = tv;
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;

        ProductViewHolder(LinearLayout layout) {
            super(layout);
            this.container = layout;
        }
    }
}
