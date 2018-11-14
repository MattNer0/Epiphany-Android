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

    private static final int DATABASE_VERSION = 2;
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

    private static final String CONSTRAINT_PATH = "path_unique";

    // NOTES Table - column nmaes
    private static final String KEY_NOTE_SUMMARY    = "summary";
    private static final String KEY_NOTE_CREATED_AT = "created_at";
    private static final String KEY_NOTE_UPDATED_AT = "updated_at";

    private static final String KEY_FOLDER_UPDATED_AT = "updated_at";

    private static final String KEY_BUCKET_ORDER = "bucket_order";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PATH + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_NOTE_SUMMARY + " TEXT,"
            + KEY_NOTE_UPDATED_AT + " INTEGER,"
            + KEY_NOTE_CREATED_AT + " INTEGER,"
            + "CONSTRAINT " + CONSTRAINT_PATH + " UNIQUE (" + KEY_PATH + ")"
            + ")";

    private static final String CREATE_TABLE_FOLDERS = "CREATE TABLE " + TABLE_FOLDERS + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PATH + " TEXT,"
            + KEY_FOLDER_UPDATED_AT + " INTEGER,"
            + "CONSTRAINT " + CONSTRAINT_PATH + " UNIQUE (" + KEY_PATH + ")"
            + ")";

    private static final String CREATE_TABLE_BUCKETS = "CREATE TABLE " + TABLE_BUCKETS + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PATH + " TEXT,"
            + KEY_BUCKET_ORDER + " INTEGER,"
            + KEY_FOLDER_UPDATED_AT + " INTEGER,"
            + "CONSTRAINT " + CONSTRAINT_PATH + " UNIQUE (" + KEY_PATH + ")"
            + ")";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
        db.execSQL(CREATE_TABLE_FOLDERS);
        db.execSQL(CREATE_TABLE_BUCKETS);
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

        if (data.containsKey(Constants.KEY_NOTE_METADATA)) {
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

    private long insertNote(String path, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        values.put(KEY_PATH, path);
        return db.insert(TABLE_NOTES, null, values);
    }

    private int updateNote(String path, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.update(TABLE_NOTES, values, KEY_PATH + " = ?", new String[] { path });
    }

    private boolean isNoteInDB(String path) {
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

    private long insertBucket(String path, int order) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, path);
        values.put(KEY_BUCKET_ORDER, order);
        values.put(KEY_FOLDER_UPDATED_AT, new Date().getTime());

        return db.insert(TABLE_BUCKETS, null, values);
    }

    private int updateBucket(String path, int order) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BUCKET_ORDER, order);
        values.put(KEY_FOLDER_UPDATED_AT, new Date().getTime());

        return db.update(TABLE_BUCKETS, values, KEY_PATH + " = ?", new String[] { path });
    }

    public void saveBucket(String path, int order) {
        String selectQuery = "SELECT  * FROM " + TABLE_BUCKETS + " tn"
                + " WHERE tn."
                + KEY_PATH + " = '" + path + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                updateBucket(path, order);
            } else {
                insertBucket(path, order);
            }
        } finally {
            c.close();
        }
    }

    private long insertFolder(String path) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, path);
        values.put(KEY_FOLDER_UPDATED_AT, new Date().getTime());

        return db.insert(TABLE_FOLDERS, null, values);
    }

    private int updateFolder(String path) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_FOLDER_UPDATED_AT, new Date().getTime());

        return db.update(TABLE_FOLDERS, values, KEY_PATH + " = ?", new String[] { path });
    }

    public boolean shouldUpdateFolder(String path) {
        String selectQuery = "SELECT  * FROM " + TABLE_FOLDERS + " tn"
                + " WHERE tn."
                + KEY_PATH + " = '" + path + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        boolean should_update = true;
        try {
            if (c.moveToFirst()) {
                long time = getLong(c, KEY_NOTE_UPDATED_AT);
                long now = new Date().getTime();

                int diffInDays = (int)( (now-time)/(1000 * 60 * 60 * 24) );
                if (diffInDays > 1) {
                    updateFolder(path);
                } else {
                    should_update = false;
                }
            } else {
                insertFolder(path);
            }
        } finally {
            c.close();
        }

        return should_update;
    }

    public Bundle getNoteByPath(String path) {
        Bundle res = new Bundle();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " WHERE tn."
                + KEY_PATH + " = '" + path + "'"
                + " LIMIT 1";

        //Log.i(Constants.LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                res.putInt(KEY_ID, getInt(c, KEY_ID));

                File f = new File(path);
                res.putString(Constants.KEY_NOTE_PATH, f.getPath());
                res.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                res.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
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

    public boolean deleteFolders() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            int removed = db.delete(TABLE_FOLDERS, null, null);
            if (removed > 0) {
                db.execSQL("UPDATE "+TABLE_SEQUENCE+" SET seq = 0 WHERE name='"+TABLE_FOLDERS+"'");
            }

            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBuckets() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            int removed = db.delete(TABLE_BUCKETS, null, null);
            if (removed > 0) {
                db.execSQL("UPDATE "+TABLE_SEQUENCE+" SET seq = 0 WHERE name='"+TABLE_BUCKETS+"'");
            }

            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<MainModel> getBuckets() {
        ArrayList<MainModel> res = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_BUCKETS + " tn"
                + " ORDER BY "+KEY_BUCKET_ORDER+" ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                do {
                    Bundle res_bucket = new Bundle();
                    File f = new File(getString(c, KEY_PATH));
                    res_bucket.putString("path", f.getPath());
                    res_bucket.putString("name", f.getName());
                    res_bucket.putInt("order", getInt(c, KEY_BUCKET_ORDER));
                    res.add(new SingleRack(res_bucket));
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return res;
    }

    public ArrayList<Bundle> getNotes() {
        ArrayList<Bundle> res = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " tn"
                + " ORDER BY id ASC";

        //Log.i(Constants.LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c.moveToFirst()) {
                do {
                    Bundle res_note = new Bundle();
                    res_note.putInt(KEY_ID, getInt(c, KEY_ID));
                    File f = new File(getString(c, KEY_PATH));
                    res_note.putString(Constants.KEY_NOTE_PATH, f.getPath());
                    res_note.putString(Constants.KEY_NOTE_FILENAME, f.getName());
                    res_note.putString(Constants.KEY_NOTE_TITLE, getString(c, KEY_NAME));
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
