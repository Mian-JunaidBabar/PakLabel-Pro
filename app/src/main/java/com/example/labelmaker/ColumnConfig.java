package com.example.labelmaker;

/**
 * Represents a single column in the dynamic rate list spreadsheet.
 * Stores the column's display name and its proportional layout weight.
 */
public class ColumnConfig {

    private String name;
    private float weight;

    public ColumnConfig(String name, float weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
