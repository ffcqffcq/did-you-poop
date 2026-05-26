package com.laleme.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class PoopStore {
    private static final String PREFS = "laleme_store";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_MIGRATED_TO_SQLITE = "migrated_to_sqlite";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private final SharedPreferences prefs;
    private final PoopDatabaseHelper databaseHelper;

    PoopStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        databaseHelper = new PoopDatabaseHelper(context);
        migrateLegacyEntriesIfNeeded();
    }

    List<PoopEntry> loadEntries() {
        ArrayList<PoopEntry> entries = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                PoopDatabaseHelper.TABLE_ENTRIES,
                null,
                null,
                null,
                null,
                null,
                PoopDatabaseHelper.COL_TIME_MILLIS + " DESC"
        )) {
            while (cursor.moveToNext()) {
                entries.add(PoopEntry.fromCursor(cursor));
            }
        }
        return entries;
    }

    long addEntry(PoopEntry entry) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        return db.insert(PoopDatabaseHelper.TABLE_ENTRIES, null, valuesFor(entry));
    }

    void updateEntry(PoopEntry entry) {
        if (entry.id < 0) {
            return;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(
                PoopDatabaseHelper.TABLE_ENTRIES,
                valuesFor(entry),
                PoopDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(entry.id)}
        );
    }

    void deleteEntry(long id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(
                PoopDatabaseHelper.TABLE_ENTRIES,
                PoopDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    void setReminderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 8);
    }

    int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 30);
    }

    void setReminderTime(int hour, int minute) {
        prefs.edit()
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();
    }

    private void migrateLegacyEntriesIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED_TO_SQLITE, false)) {
            return;
        }

        String raw = prefs.getString(KEY_ENTRIES, "[]");
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                db.insert(PoopDatabaseHelper.TABLE_ENTRIES, null, valuesFor(PoopEntry.fromJson(array.getJSONObject(i))));
            }
            db.setTransactionSuccessful();
            prefs.edit()
                    .putBoolean(KEY_MIGRATED_TO_SQLITE, true)
                    .remove(KEY_ENTRIES)
                    .apply();
        } catch (JSONException ignored) {
            prefs.edit()
                    .putBoolean(KEY_MIGRATED_TO_SQLITE, true)
                    .remove(KEY_ENTRIES)
                    .apply();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues valuesFor(PoopEntry entry) {
        ContentValues values = new ContentValues();
        values.put(PoopDatabaseHelper.COL_TIME_MILLIS, entry.timeMillis);
        values.put(PoopDatabaseHelper.COL_DURATION_MINUTES, entry.durationMinutes);
        values.put(PoopDatabaseHelper.COL_NOTE, entry.note);
        values.put(PoopDatabaseHelper.COL_STOOL_TYPE, entry.stoolType);
        values.put(PoopDatabaseHelper.COL_DISCOMFORT, entry.discomfort ? 1 : 0);
        return values;
    }

    @SuppressWarnings("unused")
    private String legacyJsonForDebugging(List<PoopEntry> entries) {
        JSONArray array = new JSONArray();
        for (PoopEntry entry : entries) {
            try {
                array.put(entry.toJson());
            } catch (JSONException ignored) {
                // Values are primitive and should not fail, but skip malformed items if they ever appear.
            }
        }
        return array.toString();
    }
}
