package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Courses.db";
    private static final int DATABASE_VERSION = 1;

    // טבלה להודעות
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_COURSE_NAME = "course_name";
    private static final String COLUMN_MESSAGE = "message";

    // טבלה לקבצים
    private static final String TABLE_FILES = "files";
    private static final String COLUMN_FILE_PATH = "file_path";

    // טבלה להערות
    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_NOTE = "note";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // יצירת טבלת הודעות
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_COURSE_NAME + " TEXT, " +
                COLUMN_MESSAGE + " TEXT)";
        db.execSQL(createMessagesTable);

        // יצירת טבלת קבצים
        String createFilesTable = "CREATE TABLE " + TABLE_FILES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_COURSE_NAME + " TEXT, " +
                COLUMN_FILE_PATH + " TEXT)";
        db.execSQL(createFilesTable);

        // יצירת טבלת הערות
        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_COURSE_NAME + " TEXT, " +
                COLUMN_NOTE + " TEXT)";
        db.execSQL(createNotesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    // הוספת הודעה
    public void insertMessage(String courseName, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_NAME, courseName);
        values.put(COLUMN_MESSAGE, message);
        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    // הוספת קובץ
    public void insertFile(String courseName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_NAME, courseName);
        values.put(COLUMN_FILE_PATH, filePath);
        db.insert(TABLE_FILES, null, values);
        db.close();
    }

    // הוספת הערה
    public void insertNote(String courseName, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_NAME, courseName);
        values.put(COLUMN_NOTE, note);
        db.insert(TABLE_NOTES, null, values);
        db.close();
    }

    // קבלת הודעות
    public ArrayList<String> getMessages(String courseName) {
        ArrayList<String> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES,
                new String[]{COLUMN_MESSAGE},
                COLUMN_COURSE_NAME + "=?",
                new String[]{courseName},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                messages.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }

    // קבלת קבצים
    public ArrayList<String> getFiles(String courseName) {
        ArrayList<String> files = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FILES,
                new String[]{COLUMN_FILE_PATH},
                COLUMN_COURSE_NAME + "=?",
                new String[]{courseName},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                files.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return files;
    }

    // קבלת הערות
    public ArrayList<String> getNotes(String courseName) {
        ArrayList<String> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES,
                new String[]{COLUMN_NOTE},
                COLUMN_COURSE_NAME + "=?",
                new String[]{courseName},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                notes.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}
