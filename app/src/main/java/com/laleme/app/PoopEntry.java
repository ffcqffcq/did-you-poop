package com.laleme.app;

import android.database.Cursor;

import org.json.JSONException;
import org.json.JSONObject;

final class PoopEntry {
    static final String FIELD_TIME = "time";
    static final String FIELD_DURATION = "duration";
    static final String FIELD_NOTE = "note";
    static final String FIELD_STOOL_TYPE = "stool_type";
    static final String FIELD_DISCOMFORT = "discomfort";

    final long id;
    final long timeMillis;
    final int durationMinutes;
    final String note;
    final String stoolType;
    final boolean discomfort;

    PoopEntry(long timeMillis, int durationMinutes, String note) {
        this(-1, timeMillis, durationMinutes, note, "", false);
    }

    PoopEntry(long timeMillis, int durationMinutes, String note, String stoolType, boolean discomfort) {
        this(-1, timeMillis, durationMinutes, note, stoolType, discomfort);
    }

    PoopEntry(long id, long timeMillis, int durationMinutes, String note, String stoolType, boolean discomfort) {
        this.id = id;
        this.timeMillis = timeMillis;
        this.durationMinutes = durationMinutes;
        this.note = note == null ? "" : note.trim();
        this.stoolType = stoolType == null ? "" : stoolType.trim();
        this.discomfort = discomfort;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(FIELD_TIME, timeMillis);
        object.put(FIELD_DURATION, durationMinutes);
        object.put(FIELD_NOTE, note);
        object.put(FIELD_STOOL_TYPE, stoolType);
        object.put(FIELD_DISCOMFORT, discomfort);
        return object;
    }

    static PoopEntry fromJson(JSONObject object) {
        return new PoopEntry(
                object.optLong(FIELD_TIME, System.currentTimeMillis()),
                object.optInt(FIELD_DURATION, 0),
                object.optString(FIELD_NOTE, ""),
                object.optString(FIELD_STOOL_TYPE, ""),
                object.optBoolean(FIELD_DISCOMFORT, false)
        );
    }

    static PoopEntry fromCursor(Cursor cursor) {
        return new PoopEntry(
                cursor.getLong(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_TIME_MILLIS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_DURATION_MINUTES)),
                cursor.getString(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_NOTE)),
                cursor.getString(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_STOOL_TYPE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(PoopDatabaseHelper.COL_DISCOMFORT)) == 1
        );
    }
}
