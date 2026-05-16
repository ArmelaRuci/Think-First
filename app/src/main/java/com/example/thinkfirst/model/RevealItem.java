package com.example.thinkfirst.model;

public class RevealItem {

    private final String type;           // "correct" | "missed" | "false_flag"
    private final String displayText;
    private final String correctVersion;
    private final String explanation;
    private final int    points;

    public RevealItem(String type, String displayText,
                      String correctVersion, String explanation, int points) {
        this.type           = type;
        this.displayText    = displayText;
        this.correctVersion = correctVersion;
        this.explanation    = explanation;
        this.points         = points;
    }

    public String getType()           { return type; }
    public String getDisplayText()    { return displayText; }
    public String getCorrectVersion() { return correctVersion; }
    public String getExplanation()    { return explanation; }
    public int    getPoints()         { return points; }
}