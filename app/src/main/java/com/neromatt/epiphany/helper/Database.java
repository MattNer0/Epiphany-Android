package com.neromatt.epiphany.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleRack;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class Database extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "epiphany";

    // Table Names
    private static final String TABLE_NOTES   = "notes";
    private static final String TABLE_FOLDERS = "folders";
    private static final String TABLE_BUCKETS = "buckets";

    private static final String TABLE_SEQUENCE = "SQLITE_SEQUENCE";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHOTO = "photo";

    private static final String CONSTRAINT_PATH = "path_unique";

    // NOTES Table - column nmaes
    private static final String KEY_NOTE_SUMMARY    = "summary";
    private static final String KEY_NOTE_CREATED_AT = "created_at";
    private static final String KEY_NOTE_UPDATED_AT = "updated_at";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PATH + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_PHOTO + " TEXT,"
            + KEY_NOTE_SUMMARY + " TEXT,"
            + KEY_NOTE_UPDATED_AT + " INTEGER,"
            + KEY_NOTE_CREATED_AT + " INTEGER,"
            + "CONSTRAINT " + CONSTRAINT_PATH + " UNIQUE (" + KEY_PATH + ")"
            + ")";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOLDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUCKETS);

        onCreate(db);
    }

    public void saveNote(String path, Bundle data) {

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, data.getString(Constants.KEY_NOTE_TITLE, ""));
        values.put(KEY_NOTE_SUMMARY, data.getString(Constants.KEY_NOTE_SUMMARY, ""));

        if (data.containsKey(Constants.KEY_NOTE_PHOTO)) {
            values.put(KEY_PHOTO, data.getString(Constants.KEY_NOTE_PHOTO, ""));
        }

        if (data.containsKey(Constants.METATAG_UPDATED) && data.containsKey(Constants.METATAG_CREATED)) {
            values.put(KEY_NOTE_UPDATED_AT, data.getLong(Constants.METATAG_UPDATED));
            values.put(KEY_NOTE_CREATED_AT, data.getLong(Constants.METATAG_CREATED));
        } else if (data.containsKey(Constants.KEY_NOTE_METADATA)) {
            Bundle metadata = data.getBundle(Constants.KEY_NOTE_METADATA);
            String updated = metadata.getString(Constants.METATAG_UPDATED, "");
            String created = metadata.getString(Constants.METATAG_CREATED, "");
            try {
                values.put(KEY_NOTE_UPDATED_AT, SingleNote.parseDate(updated).getTime());
                values.put(KEY_NOTE_CREATED_AT, SingleNote.parseDate(created).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (isNoteInDB(path)) {
            updateNote(path, values);
        } else {
            insertNote(path, values);
        }
    }

    private void insertNote(String path, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        values.put(KEY_PATH, path);
        db.insert(TABLE_NOTES, null, values);
    }

    private void updateNote(String path, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.update(TABLE_NOTES, values, KEY_PATH + " = ?", new String[] { path });
    }

    public boolean isNoteInDB(String path) {
        String countQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " WHERE tn."
                + KEY_PATH + " = '" + path + "'";

        int count;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(countQuery, null);
        try {
            count = c.getCount();
        } finally {
            c.close();
        }

        return count > 0;
    }

    public Bundle getNoteByPath(String path) {
        Bundle res = new Bundle();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " WHERE tn."
                + KEY_PATH + " = '" + path + "'"
                + " LIMIT 1";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                res.putInt(KEY_ID, getInt(c, KEY_ID));

                File f = new File(path);
                res.putString(Constants.KEY_NOTE_PATH, f.getParentFile().getPath());
                res.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                res.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
                res.putString(Constants.KEY_NOTE_PHOTO, getString(c, KEY_PHOTO));

                res.putString(Constants.KEY_NOTE_BODY, "");
                res.putString(Constants.KEY_NOTE_SUMMARY, getString(c, KEY_NOTE_SUMMARY));
                res.putLong(Constants.METATAG_UPDATED, getLong(c, KEY_NOTE_UPDATED_AT));
                res.putLong(Constants.METATAG_CREATED, getLong(c, KEY_NOTE_CREATED_AT));
            } else {
                res = null;
            }
        } finally {
            c.close();
        }

        return res;
    }

    public boolean deleteNotes() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            int removed = db.delete(TABLE_NOTES, null, null);
            if (removed > 0) {
                db.execSQL("UPDATE " + TABLE_SEQUENCE + " SET seq = 0 WHERE name='" + TABLE_NOTES + "'");
            }

            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteNoteByID(int id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            return db.delete(TABLE_NOTES, KEY_ID + "=" + id, null) > 0;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<Bundle> getNotesByFolderPath(String path) {
        ArrayList<Bundle> res = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " WHERE " + KEY_PATH + " LIKE '" + path + "%'"
                + " ORDER BY id ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                do {
                    Bundle res_note = new Bundle();
                    res_note.putInt(Constants.KEY_ID, getInt(c, KEY_ID));
                    File f = new File(getString(c, KEY_PATH));
                    res_note.putString(Constants.KEY_NOTE_PATH, f.getParentFile().getPath());
                    res_note.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                    res_note.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
                    res_note.putString(Constants.KEY_NOTE_PHOTO, getString(c, KEY_PHOTO));
                    res_note.putString(Constants.KEY_NOTE_BODY, "");
                    res_note.putString(Constants.KEY_NOTE_SUMMARY, getString(c, KEY_NOTE_SUMMARY));
                    res_note.putLong(Constants.METATAG_UPDATED, getLong(c, KEY_NOTE_UPDATED_AT));
                    res_note.putLong(Constants.METATAG_CREATED, getLong(c, KEY_NOTE_CREATED_AT));

                    res.add(res_note);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return res;
    }

    public ArrayList<Bundle> getRecentNotes() {
        ArrayList<Bundle> res = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " ORDER BY "+KEY_NOTE_UPDATED_AT+" DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                do {
                    Bundle res_note = new Bundle();
                    res_note.putInt(Constants.KEY_ID, getInt(c, KEY_ID));
                    File f = new File(getString(c, KEY_PATH));
                    res_note.putString(Constants.KEY_NOTE_PATH, f.getParentFile().getPath());
                    res_note.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                    res_note.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
                    res_note.putString(Constants.KEY_NOTE_PHOTO, getString(c, KEY_PHOTO));
                    res_note.putString(Constants.KEY_NOTE_BODY, "");
                    res_note.putString(Constants.KEY_NOTE_SUMMARY, getString(c, KEY_NOTE_SUMMARY));
                    res_note.putLong(Constants.METATAG_UPDATED, getLong(c, KEY_NOTE_UPDATED_AT));
                    res_note.putLong(Constants.METATAG_CREATED, getLong(c, KEY_NOTE_CREATED_AT));

                    res.add(res_note);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return res;
    }

    /*public ArrayList<Bundle> getNotes() {
        ArrayList<Bundle> res = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " ORDER BY id ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                do {
                    Bundle res_note = new Bundle();
                    res_note.putInt(Constants.KEY_ID, getInt(c, KEY_ID));
                    File f = new File(getString(c, KEY_PATH));
                    res_note.putString(Constants.KEY_NOTE_PATH, f.getPath());
                    res_note.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                    res_note.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
                    res_note.putString(Constants.KEY_NOTE_PHOTO, getString(c, KEY_PHOTO));
                    res_note.putString(Constants.KEY_NOTE_BODY, "");
                    res_note.putString(Constants.KEY_NOTE_SUMMARY, getString(c, KEY_NOTE_SUMMARY));
                    res_note.putLong(Constants.METATAG_UPDATED, getLong(c, KEY_NOTE_UPDATED_AT));
                    res_note.putLong(Constants.METATAG_CREATED, getLong(c, KEY_NOTE_CREATED_AT));

                    res.add(res_note);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return res;
    }*/

    private int getInt(Cursor c, String key) {
        return c.getInt((c.getColumnIndex(key)));
    }

    private String getString(Cursor c, String key) {
        return c.getString((c.getColumnIndex(key)));
    }

    private long getLong(Cursor c, String key) {
        return c.getLong((c.getColumnIndex(key)));
    }

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }
}
