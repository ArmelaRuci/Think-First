package com.example.thinkfirst.model;

public class VerdictItem {

    private final String sentenceText;
    private String       verdict;

    public VerdictItem(String sentenceText) {
        this.sentenceText = sentenceText;
        this.verdict      = "";
    }

    public String getSentenceText() { return sentenceText; }
    public String getVerdict()      { return verdict; }

    public void setVerdict(String v) { this.verdict = v; }

    public boolean isVerdictSelected() {
        return verdict != null && !verdict.isEmpty();
    }
}