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
    private float fontSize = 14f; // sp
    private int rowPadding = 12; // dp
    private int rowBgColor = Color.parseColor("#F5F8F8"); // default alternating color
    private int subheaderBgColor = Color.parseColor("#E0F2F1");
    private int fontColor = Color.BLACK; // default text color
    private boolean autoSrNo = false;

    // Global Typography State
    private float globalLetterSpacing = 0f;
    private boolean globalBold = false;
    private boolean globalItalic = false;

    public interface OnRowClickListener {
        void onRowClicked(int position, RowModel row);
    }

    private OnRowClickListener rowClickListener;

    // --- Data setters ---

    public RateListAdapter() {
        // default constructor
    }

    public void setGlobalLetterSpacing(float globalLetterSpacing) {
        this.globalLetterSpacing = globalLetterSpacing;
    }

    public void setGlobalBold(boolean globalBold) {
        this.globalBold = globalBold;
    }

    public void setGlobalItalic(boolean globalItalic) {
        this.globalItalic = globalItalic;
    }

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

    public void setSubheaderBgColor(int color) {
        this.subheaderBgColor = color;
    }

    public void setOnRowClickListener(OnRowClickListener listener) {
        this.rowClickListener = listener;
    }

    public void setAutoSrNo(boolean autoSrNo) {
        this.autoSrNo = autoSrNo;
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

        if (rowClickListener != null) {
            holder.itemView.setOnClickListener(v -> rowClickListener.onRowClicked(position, row));
        }
    }

    private void bindSubheader(SubheaderViewHolder holder, RowModel row) {
        TextView tv = holder.textView;
        tv.setText(row.getSubheaderText());
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize + 2);
        int pad = dpToPx(tv, rowPadding);
        tv.setPadding(dpToPx(tv, 16), pad, dpToPx(tv, 16), pad);

        int bgColor = row.getCustomBgColor() != 0 ? row.getCustomBgColor() : subheaderBgColor;
        tv.setBackgroundColor(bgColor);
        tv.setTextColor(fontColor);
    }

    private void bindProduct(ProductViewHolder holder, RowModel row, int position) {
        LinearLayout container = holder.container;
        container.removeAllViews();

        int pad = dpToPx(container, rowPadding);
        container.setPadding(dpToPx(container, 16), pad, dpToPx(container, 16), pad);

        // Row Background Logic
        int bgColor = rowBgColor;
        if (row.getCustomBgColor() != 0) {
            bgColor = row.getCustomBgColor();
        } else if (position % 2 == 0) {
            bgColor = Color.WHITE;
        }
        container.setBackgroundColor(bgColor);

        List<String> values = row.getCellValues();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig col = columns.get(i);

            TextView cell = new TextView(container.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dpToPx(cell, col.getWidth()), ViewGroup.LayoutParams.WRAP_CONTENT);
            cell.setLayoutParams(lp);

            float cellFontSize = (col.getCustomFontSize() != -1f) ? col.getCustomFontSize() : fontSize;
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, cellFontSize);
            cell.setTextColor(fontColor);

            // Letter Spacing
            float letterSpacing = (col.getCustomLetterSpacing() != -1f) ? col.getCustomLetterSpacing()
                    : globalLetterSpacing;
            cell.setLetterSpacing(letterSpacing);

            // Font Style
            boolean isBold = (col.getCustomBold() != null) ? col.getCustomBold() : globalBold;
            boolean isItalic = (col.getCustomItalic() != null) ? col.getCustomItalic() : globalItalic;

            int textStyle = Typeface.NORMAL;
            if (isBold && isItalic)
                textStyle = Typeface.BOLD_ITALIC;
            else if (isBold)
                textStyle = Typeface.BOLD;
            else if (isItalic)
                textStyle = Typeface.ITALIC;

            cell.setTypeface(null, textStyle);

            // Center everything uniformly to match PDF
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dpToPx(cell, 4), 0, dpToPx(cell, 4), 0);

            // Populate with data or empty string
            if (autoSrNo && i == 0 && col.getName().equals("Sr. No")) {
                // Auto serial number: count product rows before this position
                int srNo = 0;
                for (int r = 0; r <= position; r++) {
                    if (rows.get(r).getViewType() == RowModel.TYPE_PRODUCT) {
                        srNo++;
                    }
                }
                cell.setText(String.valueOf(srNo));
            } else if (i < values.size()) {
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
