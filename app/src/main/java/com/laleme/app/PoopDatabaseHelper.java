package com.laleme.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class PoopDatabaseHelper extends SQLiteOpenHelper {
    static final String TABLE_ENTRIES = "entries";
    static final String COL_ID = "_id";
    static final String COL_TIME_MILLIS = "time_millis";
    static final String COL_DURATION_MINUTES = "duration_minutes";
    static final String COL_NOTE = "note";
    static final String COL_STOOL_TYPE = "stool_type";
    static final String COL_DISCOMFORT = "discomfort";

    private static final String DATABASE_NAME = "laleme.db";
    private static final int DATABASE_VERSION = 1;

    PoopDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ENTRIES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TIME_MILLIS + " INTEGER NOT NULL, "
                + COL_DURATION_MINUTES + " INTEGER NOT NULL, "
                + COL_NOTE + " TEXT NOT NULL DEFAULT '', "
                + COL_STOOL_TYPE + " TEXT NOT NULL DEFAULT '', "
                + COL_DISCOMFORT + " INTEGER NOT NULL DEFAULT 0"
                + ")");
        db.execSQL("CREATE INDEX idx_entries_time ON " + TABLE_ENTRIES
                + " (" + COL_TIME_MILLIS + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations belong here.
    }
}
