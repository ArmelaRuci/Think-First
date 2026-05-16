package com.example.thinkfirst.model;

public class DetectiveError {

    private String errorText;    // exact text as it appears in ai_response
    private String correct;      // what the correct information is
    private String explanation;  // shown to student after verdict
    private String difficulty;   // "easy" | "medium" | "hard"

    // Required by Gson
    public DetectiveError() {}

    public String getErrorText()   { return errorText; }
    public String getCorrect()     { return correct; }
    public String getExplanation() { return explanation; }
    public String getDifficulty()  { return difficulty; }
}