package com.example.thinkfirst.model;

import java.util.List;

public class Task {

    private String       id;
    private String       subject;
    private String       chapter;
    private int          difficulty;
    private String       question;
    private String       hintTier1;
    private String       hintTier2;
    private String       hintTier3;
    private List<String> keyConcepts;
    private List<String> keywords;

    public Task() {}

    public String       getId()          { return id; }
    public String       getSubject()     { return subject; }
    public String       getChapter()     { return chapter; }
    public int          getDifficulty()  { return difficulty; }
    public String       getQuestion()    { return question; }
    public String       getHintTier1()   { return hintTier1; }
    public String       getHintTier2()   { return hintTier2; }
    public String       getHintTier3()   { return hintTier3; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public List<String> getKeywords()    { return keywords; }
}