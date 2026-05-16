package com.example.thinkfirst;

import android.content.Context;
import android.util.Log;
import com.example.thinkfirst.model.DetectiveCase;
import com.example.thinkfirst.model.Task;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContentLoader {

    private static final String TAG = "ContentLoader";

    private final Context context;
    private final Gson    gson;

    public ContentLoader(Context context) {
        this.context = context.getApplicationContext();
        this.gson    = new Gson();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public List<Task> loadTasks(String subjectKey) {
        String path = "content/tasks/" + subjectKey + "_tasks.json";
        String json = readAsset(path);
        if (json == null) {
            Log.e(TAG, "Could not read asset: " + path);
            return new ArrayList<>();
        }
        return parseTaskArray(json);
    }

    public List<DetectiveCase> loadDetectiveCases(String subjectKey) {
        String path = "content/detective/" + subjectKey + "_cases.json";
        String json = readAsset(path);
        if (json == null) {
            Log.e(TAG, "Could not read asset: " + path);
            return new ArrayList<>();
        }
        try {
            JsonArray arr  = JsonParser.parseString(json).getAsJsonArray();
            List<DetectiveCase> list = new ArrayList<>();
            for (JsonElement el : arr) {
                try {
                    list.add(gson.fromJson(el, DetectiveCase.class));
                } catch (Exception e) {
                    Log.w(TAG, "Skipping malformed detective case: " + e.getMessage());
                }
            }
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse detective cases for " + subjectKey, e);
            return new ArrayList<>();
        }
    }

    // ── Task parsing — element by element so one bad entry
    //    never kills the whole list ──────────────────────────────────────────

    private List<Task> parseTaskArray(String json) {
        List<Task> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            Log.d(TAG, "JSON array has " + arr.size() + " elements");
            for (int i = 0; i < arr.size(); i++) {
                try {
                    Task task = gson.fromJson(arr.get(i), Task.class);
                    if (task != null && task.getId() != null) {
                        list.add(task);
                        Log.d(TAG, "Loaded task " + i + ": " + task.getId());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Skipping task at index " + i + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse task array: " + e.getMessage());
        }
        Log.d(TAG, "Total tasks loaded: " + list.size());
        return list;
    }

    // ── Asset reader — uses BufferedReader to avoid chunk-boundary
    //    corruption that broke the old byte-array approach ─────────────────

    private String readAsset(String path) {
        try {
            InputStream     is  = context.getAssets().open(path);
            BufferedReader  br  = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder   sb  = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "IOException reading asset: " + path, e);
            return null;
        }
    }
}