package com.example.labelmaker;

/**
 * Represents a single column in the dynamic rate list spreadsheet.
 * Stores the column's display name and its proportional layout weight.
 */
public class ColumnConfig {

    private String name;
    private int width;
    private float customFontSize = -1f;
    private float customLetterSpacing = -1f;
    private Boolean customBold = null;
    private Boolean customItalic = null;

    public ColumnConfig(String name, int width) {
        this.name = name;
        this.width = width;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public float getCustomFontSize() {
        return customFontSize;
    }

    public void setCustomFontSize(float customFontSize) {
        this.customFontSize = customFontSize;
    }

    public float getCustomLetterSpacing() {
        return customLetterSpacing;
    }

    public void setCustomLetterSpacing(float customLetterSpacing) {
        this.customLetterSpacing = customLetterSpacing;
    }

    public Boolean getCustomBold() {
        return customBold;
    }

    public void setCustomBold(Boolean customBold) {
        this.customBold = customBold;
    }

    public Boolean getCustomItalic() {
        return customItalic;
    }

    public void setCustomItalic(Boolean customItalic) {
        this.customItalic = customItalic;
    }
}
