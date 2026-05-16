package com.example.thinkfirst.model;

import java.util.List;

public class DetectiveCase {

    private String              id;
    private String              subject;
    private String              topic;
    private String              aiResponse;
    private List<DetectiveError> errors;

    // Required by Gson
    public DetectiveCase() {}

    public String               getId()         { return id; }
    public String               getSubject()    { return subject; }
    public String               getTopic()      { return topic; }
    public String               getAiResponse() { return aiResponse; }
    public List<DetectiveError> getErrors()     { return errors; }
}