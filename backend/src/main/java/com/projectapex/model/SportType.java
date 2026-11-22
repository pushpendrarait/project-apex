package com.projectapex.model;

/**
 * Built-in and extensible sport categories for Project Apex
 */
public enum SportType {
    CRICKET_TURF("Box Cricket", "Cricket", "🏏", true),
    FOOTBALL_TURF("Football Turf", "Football", "⚽", true),
    POOL_TABLE("Pool & Snooker Table", "Cue Sports", "🎱", false),
    SQUASH("Squash Court", "Racquet Sports", "🏸", false),
    LAWN_TENNIS("Lawn Tennis Court", "Tennis", "🎾", false),
    BADMINTON("Badminton Court", "Badminton", "🏸", false),
    PICKLEBALL("Pickleball Court", "Pickleball", "🏓", false),
    MULTI_SPORT("Multi-Sport Turf (Cricket/Football)", "Multi-Sport", "⚡", true);

    private final String displayName;
    private final String category;
    private final String icon;
    private final boolean isTurf;

    SportType(String displayName, String category, String icon, boolean isTurf) {
        this.displayName = displayName;
        this.category = category;
        this.icon = icon;
        this.isTurf = isTurf;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isTurf() {
        return isTurf;
    }

    public static SportType fromString(String val) {
        if (val == null) return MULTI_SPORT;
        for (SportType st : values()) {
            if (st.name().equalsIgnoreCase(val) || st.displayName.equalsIgnoreCase(val)) {
                return st;
            }
        }
        return MULTI_SPORT;
    }
}
