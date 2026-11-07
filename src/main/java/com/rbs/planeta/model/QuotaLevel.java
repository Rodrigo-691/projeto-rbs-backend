package com.rbs.planeta.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuotaLevel {
    MASTER("master", "Master"),
    SUBCOTA_G("subcota_g", "Subcota G"),
    SUBCOTA("subcota", "Subcota");

    private final String value;
    private final String label;

    QuotaLevel(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static QuotaLevel fromValue(String value) {
        for (QuotaLevel q : values()) {
            if (q.value.equalsIgnoreCase(value) || q.name().equalsIgnoreCase(value)) {
                return q;
            }
        }
        return SUBCOTA;
    }
}
