package com.example.thinkfirst;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "thinkfirst.db";
    private static final int    DATABASE_VERSION = 1;

    // ── task_attempts columns ─────────────────────────────────────────────────
    public static final String TABLE_TASK_ATTEMPTS   = "task_attempts";
    public static final String COL_ATTEMPT_ID        = "id";
    public static final String COL_SUBJECT           = "subject";
    public static final String COL_TASK_ID           = "task_id";
    public static final String COL_EFFORT_SCORE      = "effort_score";
    public static final String COL_HINT_TIER         = "hint_tier";
    public static final String COL_TIMESTAMP         = "timestamp";
    public static final String COL_REFLECTION        = "reflection";
    public static final String COL_ANSWER_TEXT       = "answer_text";
    public static final String COL_VOICE_MEMO_PATH   = "voice_memo_path";
    public static final String COL_COMPLETED         = "completed";

    // ── recall_attempts table ─────────────────────────────────────────────────
    public static final String TABLE_RECALL_ATTEMPTS = "recall_attempts";
    public static final String COL_RECALL_SUBJECT    = "subject";
    public static final String COL_RECALL_TASK_ID    = "task_id";
    public static final String COL_RECALL_ANSWER     = "answer";
    public static final String COL_RECALL_TIMESTAMP  = "timestamp";

    private static final String CREATE_RECALL_ATTEMPTS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RECALL_ATTEMPTS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_RECALL_SUBJECT   + " TEXT NOT NULL, "
                    + COL_RECALL_TASK_ID   + " TEXT NOT NULL, "
                    + COL_RECALL_ANSWER    + " TEXT, "
                    + COL_RECALL_TIMESTAMP + " INTEGER NOT NULL"
                    + ");";

    // ── detective_sessions columns ────────────────────────────────────────────
    public static final String TABLE_DETECTIVE_SESSIONS = "detective_sessions";
    public static final String COL_SESSION_ID        = "id";
    public static final String COL_TOPIC             = "topic";
    public static final String COL_SCORE             = "score";
    public static final String COL_ERRORS_CAUGHT     = "errors_caught";
    public static final String COL_FALSE_FLAGS       = "false_flags";
    public static final String COL_RANK_AT_TIME      = "rank_at_time";
    public static final String COL_HIGHLIGHTS_JSON   = "highlights_json";

    // ── AI Mirror reflection columns (added to task_attempts) ────────────────
    public static final String COL_MIRROR_COVERED = "mirror_covered";
    public static final String COL_MIRROR_MISSED  = "mirror_missed";

    private static final String CREATE_TASK_ATTEMPTS =
            "CREATE TABLE " + TABLE_TASK_ATTEMPTS + " ("                      +
                    COL_ATTEMPT_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, "      +
                    COL_SUBJECT         + " TEXT    NOT NULL, "                       +
                    COL_TASK_ID         + " TEXT    NOT NULL, "                       +
                    COL_EFFORT_SCORE    + " INTEGER DEFAULT 0, "                      +
                    COL_HINT_TIER       + " INTEGER DEFAULT 0, "                      +
                    COL_TIMESTAMP       + " INTEGER NOT NULL, "                       +
                    COL_REFLECTION      + " TEXT, "                                   +
                    COL_ANSWER_TEXT     + " TEXT, "                                   +
                    COL_VOICE_MEMO_PATH + " TEXT, "                                   +
                    COL_COMPLETED       + " INTEGER DEFAULT 0, "                      +
                    COL_MIRROR_COVERED  + " TEXT DEFAULT '', "                        +
                    COL_MIRROR_MISSED   + " TEXT DEFAULT ''"                          +
                    ");";

    private static final String CREATE_DETECTIVE_SESSIONS =
            "CREATE TABLE " + TABLE_DETECTIVE_SESSIONS + " ("                 +
                    COL_SESSION_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, "      +
                    COL_SUBJECT         + " TEXT    NOT NULL, "                       +
                    COL_TOPIC           + " TEXT    NOT NULL, "                       +
                    COL_SCORE           + " INTEGER DEFAULT 0, "                      +
                    COL_ERRORS_CAUGHT   + " INTEGER DEFAULT 0, "                      +
                    COL_FALSE_FLAGS     + " INTEGER DEFAULT 0, "                      +
                    COL_TIMESTAMP       + " INTEGER NOT NULL, "                       +
                    COL_RANK_AT_TIME    + " TEXT, "                                   +
                    COL_HIGHLIGHTS_JSON + " TEXT"                                     +
                    ");";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static DatabaseHelper instance;

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TASK_ATTEMPTS);
        db.execSQL(CREATE_DETECTIVE_SESSIONS);
        db.execSQL(CREATE_RECALL_ATTEMPTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Attempt to add mirror columns to existing DB without
        // destroying data — safe for development iteration
        try {
            db.execSQL("ALTER TABLE " + TABLE_TASK_ATTEMPTS
                    + " ADD COLUMN " + COL_MIRROR_COVERED + " TEXT DEFAULT ''");
        } catch (Exception ignored) {}
        try {
            db.execSQL("ALTER TABLE " + TABLE_TASK_ATTEMPTS
                    + " ADD COLUMN " + COL_MIRROR_MISSED + " TEXT DEFAULT ''");
        } catch (Exception ignored) {}
        try {
            db.execSQL(CREATE_RECALL_ATTEMPTS);
        } catch (Exception ignored) {}
    }

    /**
     * Saves the student's AI Mirror reflections back to the most recent
     * attempt row for the given task ID and subject.
     * Called from AIMirrorActivity after the student taps Done.
     */
    public void updateMirrorReflection(String subject, String taskId,
                                       String covered, String missed) {
        ContentValues cv = new ContentValues();
        cv.put(COL_MIRROR_COVERED, covered != null ? covered : "");
        cv.put(COL_MIRROR_MISSED,  missed  != null ? missed  : "");

        // Update the most recent completed attempt for this task
        getWritableDatabase().update(
                TABLE_TASK_ATTEMPTS,
                cv,
                COL_SUBJECT + " = ? AND "
                        + COL_TASK_ID + " = ? AND "
                        + COL_COMPLETED + " = 1",
                new String[]{subject, taskId}
        );
    }


    // ── task_attempts: INSERT ─────────────────────────────────────────────────

    /**
     * Saves a completed task attempt.
     * hintTier: 0 = no hint used, 1/2/3 = tier used.
     */
    public long insertTaskAttempt(
            String subject,
            String taskId,
            int    effortScore,
            int    hintTier,
            String answerText,
            String voiceMemoPath,
            String reflection) {

        ContentValues cv = new ContentValues();
        cv.put(COL_SUBJECT,         subject);
        cv.put(COL_TASK_ID,         taskId);
        cv.put(COL_EFFORT_SCORE,    effortScore);
        cv.put(COL_HINT_TIER,       hintTier);
        cv.put(COL_TIMESTAMP,       System.currentTimeMillis());
        cv.put(COL_ANSWER_TEXT,     answerText);
        cv.put(COL_VOICE_MEMO_PATH, voiceMemoPath != null ? voiceMemoPath : "");
        cv.put(COL_REFLECTION,      reflection != null ? reflection : "");
        cv.put(COL_COMPLETED,       1);

        return getWritableDatabase().insert(TABLE_TASK_ATTEMPTS, null, cv);
    }

    // ── task_attempts: QUERY ──────────────────────────────────────────────────

    /**
     * Returns a list of task IDs that have been completed for a subject.
     * Used by TaskActivity to skip already-done tasks.
     */
    public List<String> getCompletedTaskIds(String subject) {
        List<String> ids = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                true, // DISTINCT — prevents retakes from counting a task multiple times
                TABLE_TASK_ATTEMPTS,
                new String[]{COL_TASK_ID},
                COL_SUBJECT + " = ? AND " + COL_COMPLETED + " = 1",
                new String[]{subject},
                null, null, null, null
        );
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(0));
        }
        cursor.close();
        return ids;
    }

    /**
     * Returns total number of completed tasks for a subject.
     */
    public int getCompletedCount(String subject) {
        return getCompletedTaskIds(subject).size();
    }

    /**
     * Returns average effort score for a subject (for StatsActivity in Stage 6).
     */
    public float getAverageEffortScore(String subject) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + COL_EFFORT_SCORE + ") FROM " + TABLE_TASK_ATTEMPTS +
                        " WHERE " + COL_SUBJECT + " = ?",
                new String[]{subject}
        );
        float avg = 0f;
        if (cursor.moveToFirst()) avg = cursor.getFloat(0);
        cursor.close();
        return avg;
    }

    // ── detective_sessions: INSERT ────────────────────────────────────────────

    public long insertDetectiveSession(
            String subject,
            String topic,
            int    score,
            int    errorsCaught,
            int    falseFlags,
            String rankAtTime,
            String highlightsJson) {

        ContentValues cv = new ContentValues();
        cv.put(COL_SUBJECT,         subject);
        cv.put(COL_TOPIC,           topic);
        cv.put(COL_SCORE,           score);
        cv.put(COL_ERRORS_CAUGHT,   errorsCaught);
        cv.put(COL_FALSE_FLAGS,     falseFlags);
        cv.put(COL_TIMESTAMP,       System.currentTimeMillis());
        cv.put(COL_RANK_AT_TIME,    rankAtTime);
        cv.put(COL_HIGHLIGHTS_JSON, highlightsJson != null ? highlightsJson : "");

        return getWritableDatabase().insert(TABLE_DETECTIVE_SESSIONS, null, cv);
    }

    // ── Stats queries ─────────────────────────────────────────────────────────

    /** Total completed tasks across all subjects. */
    public int getTotalTasksCompleted() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TASK_ATTEMPTS
                        + " WHERE " + COL_COMPLETED + " = 1", null
        );
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /** Count of attempts where a hint was used (hint_tier > 0). */
    public int getTotalHintsUsed() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TASK_ATTEMPTS
                        + " WHERE " + COL_HINT_TIER + " > 0"
                        + " AND "  + COL_COMPLETED  + " = 1", null
        );
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /** Returns the most recent N detective sessions as a simple record list. */
    public static class DetectiveSessionRecord {
        public final String topic;
        public final int    score;
        public final int    errorsCaught;
        public final int    falseFlags;
        public final long   timestamp;

        public DetectiveSessionRecord(String topic, int score,
                                      int errorsCaught, int falseFlags,
                                      long timestamp) {
            this.topic        = topic;
            this.score        = score;
            this.errorsCaught = errorsCaught;
            this.falseFlags   = falseFlags;
            this.timestamp    = timestamp;
        }
    }

    public List<DetectiveSessionRecord> getRecentDetectiveSessions(int limit) {
        List<DetectiveSessionRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_DETECTIVE_SESSIONS,
                new String[]{COL_TOPIC, COL_SCORE,
                        COL_ERRORS_CAUGHT, COL_FALSE_FLAGS, COL_TIMESTAMP},
                null, null, null, null,
                COL_TIMESTAMP + " DESC",
                String.valueOf(limit)
        );
        while (cursor.moveToNext()) {
            list.add(new DetectiveSessionRecord(
                    cursor.getString(0),
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getInt(3),
                    cursor.getLong(4)
            ));
        }
        cursor.close();
        return list;
    }


    /**
     * Returns the most recent completed attempt record per task_id
     * for the given subject. One row per task_id — the latest attempt.
     *
     * Used to populate the completion screen task cards with
     * per-task effort scores and hint usage.
     */
    public List<android.util.Pair<String, int[]>> getLatestAttemptsForSubject(
            String subject) {

        List<android.util.Pair<String, int[]>> results = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Subquery: for each task_id get the row with the highest timestamp
        String query =
                "SELECT t." + COL_TASK_ID      + ", "
                        + "t." + COL_EFFORT_SCORE      + ", "
                        + "t." + COL_HINT_TIER         + ", "
                        + "t." + COL_TIMESTAMP         + " "
                        + "FROM " + TABLE_TASK_ATTEMPTS + " t "
                        + "INNER JOIN ("
                        + "  SELECT " + COL_TASK_ID + ", MAX(" + COL_TIMESTAMP + ") AS max_ts"
                        + "  FROM "   + TABLE_TASK_ATTEMPTS
                        + "  WHERE "  + COL_SUBJECT   + " = ?"
                        + "  AND "    + COL_COMPLETED + " = 1"
                        + "  GROUP BY " + COL_TASK_ID
                        + ") latest "
                        + "ON t." + COL_TASK_ID    + " = latest." + COL_TASK_ID
                        + " AND t." + COL_TIMESTAMP + " = latest.max_ts"
                        + " WHERE t." + COL_SUBJECT + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{subject, subject});

        while (cursor.moveToNext()) {
            String taskId      = cursor.getString(0);
            int    effortScore = cursor.getInt(1);
            int    hintTier    = cursor.getInt(2);
            long   timestamp   = cursor.getLong(3);

            // int[] packs: [effortScore, hintTier, timestamp_seconds]
            results.add(new android.util.Pair<>(taskId,
                    new int[]{effortScore, hintTier,
                            (int)(timestamp / 1000L)}));
        }
        cursor.close();
        return results;
    }

    /**
     * Returns the timestamp of the most recent completed attempt
     * for a given subject. Returns 0 if no attempts exist.
     * Used by HomeActivity to determine whether to show Cold Open.
     */
    public long getLastAttemptTimestamp(String subject) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(" + COL_TIMESTAMP + ") FROM " + TABLE_TASK_ATTEMPTS
                        + " WHERE " + COL_SUBJECT   + " = ?"
                        + " AND "   + COL_COMPLETED + " = 1",
                new String[]{subject}
        );
        long ts = 0L;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            ts = cursor.getLong(0);
        }
        cursor.close();
        return ts;
    }

    /**
     * Returns one randomly selected completed task ID for a subject.
     * Used to pick the recall question for Cold Open.
     */
    public String getRandomCompletedTaskId(String subject) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT " + COL_TASK_ID
                        + " FROM " + TABLE_TASK_ATTEMPTS
                        + " WHERE " + COL_SUBJECT   + " = ?"
                        + " AND "   + COL_COMPLETED + " = 1"
                        + " ORDER BY RANDOM() LIMIT 1",
                new String[]{subject}
        );
        String taskId = null;
        if (cursor.moveToFirst()) taskId = cursor.getString(0);
        cursor.close();
        return taskId;
    }

    /**
     * Saves a cold open recall attempt.
     * Answer may be empty — the attempt itself is what matters.
     */
    public void saveRecallAttempt(String subject, String taskId, String answer) {
        ContentValues cv = new ContentValues();
        cv.put(COL_RECALL_SUBJECT,   subject);
        cv.put(COL_RECALL_TASK_ID,   taskId);
        cv.put(COL_RECALL_ANSWER,    answer != null ? answer : "");
        cv.put(COL_RECALL_TIMESTAMP, System.currentTimeMillis());
        getWritableDatabase().insert(TABLE_RECALL_ATTEMPTS, null, cv);
    }

}