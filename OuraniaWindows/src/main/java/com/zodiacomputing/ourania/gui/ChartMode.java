package com.zodiacomputing.ourania.gui;

public enum ChartMode {
    SINGLE("Single Chart"),
    TRANSIT("Natal & Transit"),
    SYNASTRY("Synastry"),
    COMPOSITE_MIDPOINT("Composite (Midpoint)"),
    COMPOSITE_DAVISON("Composite (Davison)");

    public final String label;
    
    ChartMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
