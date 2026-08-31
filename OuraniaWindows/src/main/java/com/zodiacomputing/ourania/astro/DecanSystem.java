package com.zodiacomputing.ourania.astro;

public enum DecanSystem {
    MODERN("Modern / Triplicity"),
    CHALDEAN("Chaldean"),
    SABIAN("Sabian Decan"),
    GOLDEN_DAWN("Golden Dawn");

    private final String label;

    DecanSystem(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
