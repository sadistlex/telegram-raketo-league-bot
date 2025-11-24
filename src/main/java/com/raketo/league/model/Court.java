package com.raketo.league.model;

public enum Court {
    BATUMI_TENNIS_CLUB("Batumi Tennis Club"),
    COURT_AIRPORT("Court Airport"),
    BATUMI_VIEW_COURT("Batumi View Court"),
    BENZE_TENNIS_CLUB("Benze Tennis Club"),
    OTHER("Other");

    private final String displayName;

    Court(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

