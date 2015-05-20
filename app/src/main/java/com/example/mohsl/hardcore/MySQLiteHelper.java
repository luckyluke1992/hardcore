package com.example.mohsl.hardcore;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MySQLiteHelper extends SQLiteOpenHelper {


    //contact db
    public static final String TABLE_CONTACTS = "contacts";
    public static final String COLUMN_CONTACT_ID = "_id";
    public static final String COLUMN_CONTACT = "contact";
    public static final String COLUMN_PUBLIC_KEY= "publickey";
    public static final String COLUMN_UNREAD_MESSAGE = "unreadmessage";
    //message db
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_MESSAGE_ID = "_id";
    public static final String COLUMN_SENDER_ID = "senderid";
    public static final String COLUMN_RECEIVER_ID = "receiverid";
    public static final String COLUMN_MESSAGE = "message";

    private static final String DATABASE_NAME = "hardcore.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CONTACTS_CREATE = "create table "
            + TABLE_CONTACTS + " (" + COLUMN_CONTACT_ID
            + " integer primary key autoincrement, " + COLUMN_CONTACT
            + " text not null, " + COLUMN_PUBLIC_KEY
            + ", " + COLUMN_UNREAD_MESSAGE + ");";

    private static final String DATABASE_MESSAGES_CREATE =
            "create table "
            + TABLE_MESSAGES + "(" + COLUMN_MESSAGE_ID
            + " integer primary key autoincrement, " + COLUMN_SENDER_ID +", "
            + COLUMN_RECEIVER_ID + ", " + COLUMN_MESSAGE
            + " text not null);";

    MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Hardcore, DB create:", DATABASE_CONTACTS_CREATE);
        db.execSQL(DATABASE_CONTACTS_CREATE);
        db.execSQL(DATABASE_MESSAGES_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}