package com.example.thinkfirst.model;

public class Subject {

    public static final String DSA  = "dsa";
    public static final String DBMS = "dbms";
    public static final String WEB  = "web";
    public static final String OOP  = "oop";
    public static final String DM   = "dm";

    private final String key;
    private final String displayName;
    private final String description;
    private final int    colorResId;
    private final int    iconResId;        // ← new
    private int          tasksCompleted;
    private int          totalTasks;

    public Subject(String key, String displayName,
                   String description, int colorResId, int iconResId) {
        this.key           = key;
        this.displayName   = displayName;
        this.description   = description;
        this.colorResId    = colorResId;
        this.iconResId     = iconResId;
        this.tasksCompleted = 0;
        this.totalTasks    = 5;
    }

    public String getKey()            { return key; }
    public String getDisplayName()    { return displayName; }
    public String getDescription()    { return description; }
    public int    getColorResId()     { return colorResId; }
    public int    getIconResId()      { return iconResId; }
    public int    getTasksCompleted() { return tasksCompleted; }
    public int    getTotalTasks()     { return totalTasks; }

    public void setTasksCompleted(int n) { this.tasksCompleted = n; }
    public void setTotalTasks(int n)     { this.totalTasks = n; }
}