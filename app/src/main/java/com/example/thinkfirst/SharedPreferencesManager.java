package com.example.thinkfirst;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static final String PREFS_NAME = "thinkfirst_prefs";

    // ── Keys ──────────────────────────────────────────────────────────────────
    private static final String KEY_TANK_LEVEL          = "tank_level";
    private static final String KEY_CURRENT_SUBJECT     = "current_subject";
    private static final String KEY_TOTAL_DET_POINTS    = "total_detective_points";
    private static final String KEY_CURRENT_RANK        = "current_rank";
    private static final String KEY_ONBOARDING_DONE     = "onboarding_done";
    private static final String KEY_LAST_OPENED         = "last_opened";

    // Per-subject completed task count: "completed_dsa", "completed_dbms" etc.
    private static final String KEY_COMPLETED_PREFIX    = "completed_";

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final float  DEFAULT_TANK_LEVEL    = 0.50f;
    public static final String DEFAULT_RANK          = "Rookie";

    // ── Tank delta constants ──────────────────────────────────────────────────
    public static final float TANK_DELTA_INDEPENDENT = +0.08f;
    public static final float TANK_DELTA_HINT_TIER1  = -0.05f;
    public static final float TANK_DELTA_HINT_TIER2  = -0.10f;
    public static final float TANK_DELTA_HINT_TIER3  = -0.20f;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static SharedPreferencesManager instance;

    public static SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    private final SharedPreferences prefs;

    private SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Tank gauge ────────────────────────────────────────────────────────────

    public float getTankLevel() {
        return prefs.getFloat(KEY_TANK_LEVEL, DEFAULT_TANK_LEVEL);
    }

    /**
     * Adjusts tank level by delta, clamped to [0.0, 1.0].
     * Pass a positive value to fill, negative to drain.
     */
    public void adjustTankLevel(float delta) {
        float current = getTankLevel();
        float updated = Math.max(0f, Math.min(1f, current + delta));
        prefs.edit().putFloat(KEY_TANK_LEVEL, updated).apply();
    }

    // ── Subject ───────────────────────────────────────────────────────────────

    public String getCurrentSubject() {
        return prefs.getString(KEY_CURRENT_SUBJECT, "dsa");
    }

    public void setCurrentSubject(String subjectKey) {
        prefs.edit().putString(KEY_CURRENT_SUBJECT, subjectKey).apply();
    }

    // ── Completed tasks per subject ────────────────────────────────────────────

    public int getCompletedTaskCount(String subjectKey) {
        return prefs.getInt(KEY_COMPLETED_PREFIX + subjectKey, 0);
    }

    public void incrementCompletedTaskCount(String subjectKey) {
        int current = getCompletedTaskCount(subjectKey);
        prefs.edit().putInt(KEY_COMPLETED_PREFIX + subjectKey, current + 1).apply();
    }

    // ── Detective points & rank ───────────────────────────────────────────────

    public int getTotalDetectivePoints() {
        return prefs.getInt(KEY_TOTAL_DET_POINTS, 0);
    }

    public void addDetectivePoints(int points) {
        int current = getTotalDetectivePoints();
        int updated = Math.max(0, current + points);
        prefs.edit().putInt(KEY_TOTAL_DET_POINTS, updated).apply();
        updateRankFromPoints(updated);
    }

    public String getCurrentRank() {
        return prefs.getString(KEY_CURRENT_RANK, DEFAULT_RANK);
    }

    private void updateRankFromPoints(int points) {
        // Thresholds calibrated against actual achievable points:
        // 10 cases total (2 per subject x 5 subjects), theoretical max ~430 pts.
        // Chief Inspector reachable with one near-perfect run.
        String rank;
        if      (points >= 400) rank = "Chief Inspector";
        else if (points >= 300) rank = "Senior Detective";
        else if (points >= 150) rank = "Analyst";
        else if (points >= 50)  rank = "Investigator";
        else                    rank = "Rookie";
        prefs.edit().putString(KEY_CURRENT_RANK, rank).apply();
    }

    // ── Detective mode unlock ─────────────────────────────────────────────────

    /**
     * Detective mode unlocks for a subject once the student
     * has completed 3 or more tasks in that subject.
     */
    public boolean isDetectiveModeUnlocked(String subjectKey) {
        return getCompletedTaskCount(subjectKey) >= 3;
    }


    // ── Detective case cycling ────────────────────────────────────────────────
    private static final String KEY_DET_CASE_INDEX_PREFIX = "det_case_idx_";

    public int getDetectiveCaseIndex(String subject) {
        return prefs.getInt(KEY_DET_CASE_INDEX_PREFIX + subject, 0);
    }

    public void advanceDetectiveCaseIndex(String subject, int totalCases) {
        if (totalCases <= 0) return;
        int next = (getDetectiveCaseIndex(subject) + 1) % totalCases;
        prefs.edit().putInt(KEY_DET_CASE_INDEX_PREFIX + subject, next).apply();
    }


    // ── Misc ──────────────────────────────────────────────────────────────────

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public void updateLastOpened() {
        prefs.edit().putLong(KEY_LAST_OPENED, System.currentTimeMillis()).apply();
    }
}