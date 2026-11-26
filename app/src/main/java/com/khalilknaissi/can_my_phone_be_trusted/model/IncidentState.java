package com.khalilknaissi.can_my_phone_be_trusted.model;

/** The four incident states from the scoping document. */
public enum IncidentState {
    OPEN("Open"), INVESTIGATING("Investigating"), RESOLVED("Resolved"), FALSE_POSITIVE("False positive");

    public final String label;

    IncidentState(String label) {
        this.label = label;
    }

    /** Open and Investigating incidents still need attention. */
    public boolean isActive() {
        return this == OPEN || this == INVESTIGATING;
    }
}
