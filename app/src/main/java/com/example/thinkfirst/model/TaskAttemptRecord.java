package com.example.thinkfirst.model;

public class TaskAttemptRecord {

    private final String taskId;
    private final String question;
    private final String chapter;      // ← new
    private final int    effortScore;
    private final int    hintTierUsed;
    private final long   timestamp;

    public TaskAttemptRecord(String taskId, String question, String chapter,
                             int effortScore, int hintTierUsed, long timestamp) {
        this.taskId       = taskId;
        this.question     = question;
        this.chapter      = chapter;
        this.effortScore  = effortScore;
        this.hintTierUsed = hintTierUsed;
        this.timestamp    = timestamp;
    }

    public String  getTaskId()       { return taskId; }
    public String  getQuestion()     { return question; }
    public String  getChapter()      { return chapter; }
    public int     getEffortScore()  { return effortScore; }
    public int     getHintTierUsed() { return hintTierUsed; }
    public long    getTimestamp()    { return timestamp; }
    public boolean wasIndependent()  { return hintTierUsed == 0; }
}