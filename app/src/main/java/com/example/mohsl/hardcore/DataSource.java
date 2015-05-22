package com.example.mohsl.hardcore;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DataSource {

    private static KeyHandler keyHandeler;
    private static DataSource instance;
    // Database fields
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] allColumns = {MySQLiteHelper.COLUMN_CONTACT_ID,
            MySQLiteHelper.COLUMN_CONTACT, MySQLiteHelper.COLUMN_PUBLIC_KEY, MySQLiteHelper.COLUMN_UNREAD_MESSAGE};

    public static DataSource getInstance(Context context)
    {
        if(instance == null) {
            if(context == null)
            {
                throw new NullPointerException("context cannot be null for the first initialization");
            }
            instance = new DataSource(context);
            keyHandeler = KeyHandler.getInstance();
        }
        return instance;
    }

    private DataSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
        database = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(database, 1, 1);
    }


    public List<Contact> getAllContactsFromDb() {
        List<Contact> contacts = new ArrayList<Contact>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                new String[]{MySQLiteHelper.COLUMN_CONTACT_ID, MySQLiteHelper.COLUMN_CONTACT, MySQLiteHelper.COLUMN_PUBLIC_KEY, MySQLiteHelper.COLUMN_UNREAD_MESSAGE} , null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            //0 --> ID
            //1 --> Name
            //2 --> PUbKey
            //3 --> MessageAvailable
            //Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "integer from _id field: " +Integer.parseInt(cursor.getString(0)) );
            contacts.add(new Contact(Integer.parseInt(cursor.getString(0)),cursor.getString(1), Boolean.parseBoolean(cursor.getString(3)), keyHandeler.getKeyFromSerialization(cursor.getString(2))));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contacts;
    }

    public Contact getContactFromDb(int contactId) {
        Contact contact = null;

        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                new String[]{MySQLiteHelper.COLUMN_CONTACT_ID, MySQLiteHelper.COLUMN_CONTACT, MySQLiteHelper.COLUMN_PUBLIC_KEY, MySQLiteHelper.COLUMN_UNREAD_MESSAGE} , MySQLiteHelper.COLUMN_CONTACT_ID + " = " + contactId, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            //0 --> ID
            //1 --> Name
            //2 --> PUbKey
            //3 --> MessageAvailable
            contact = new Contact(Integer.parseInt(cursor.getString(0)),cursor.getString(1), Boolean.parseBoolean(cursor.getString(3)), keyHandeler.getKeyFromSerialization(cursor.getString(2)));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contact;
    }

    public List<Message> getConversationHistoryFromDb(int contactId) {
        List<Message> messages = new ArrayList<Message>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                new String[]{MySQLiteHelper.COLUMN_MESSAGE_ID, MySQLiteHelper.COLUMN_SENDER_ID,
                            MySQLiteHelper.COLUMN_RECEIVER_ID, MySQLiteHelper.COLUMN_MESSAGE},
                            MySQLiteHelper.COLUMN_SENDER_ID + " = " + contactId +
                            " OR " +  MySQLiteHelper.COLUMN_RECEIVER_ID + " = " + contactId ,
                            null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            messages.add(new Message(Integer.parseInt(cursor.getString(1)),
                                        Integer.parseInt(cursor.getString(2)), cursor.getString(3)));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return messages;
    }

    public List<Message> getConversationHistoryWithMyOwnFromDb(int contactId) {
        List<Message> messages = new ArrayList<Message>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                new String[]{MySQLiteHelper.COLUMN_MESSAGE_ID, MySQLiteHelper.COLUMN_SENDER_ID,
                        MySQLiteHelper.COLUMN_RECEIVER_ID, MySQLiteHelper.COLUMN_MESSAGE},
                MySQLiteHelper.COLUMN_SENDER_ID + " = " + contactId +
                        " AND " +  MySQLiteHelper.COLUMN_RECEIVER_ID + " = " + contactId ,
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            messages.add(new Message(Integer.parseInt(cursor.getString(1)),
                    Integer.parseInt(cursor.getString(2)), cursor.getString(3)));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return messages;
    }
    public void storeMessageInDb(Message message)
    {
        final String Insert_Data="INSERT INTO " + MySQLiteHelper.TABLE_MESSAGES +
                " VALUES( null, " + message.getSenderId() + ", " + message.getReceiverId() + ", '" + message.getMessageText() + "' )";
        database.execSQL(Insert_Data);
    }

    public void storeUnreadMessageInDb(String sendername){
        final String Insert_Data="UPDATE " + MySQLiteHelper.TABLE_CONTACTS +
                " SET " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +"='true' WHERE " +
                MySQLiteHelper.COLUMN_CONTACT + "= '" + sendername +"'";
        database.execSQL(Insert_Data);
    }

    public void storeReadMessageInDb(String sendername){
        final String Insert_Data="UPDATE " + MySQLiteHelper.TABLE_CONTACTS +
                " SET " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +"='false' WHERE " +
                MySQLiteHelper.COLUMN_CONTACT + "= '" + sendername + "'";
        database.execSQL(Insert_Data);
    }

    public void storeContactInDb(Contact contact){

        final String Insert_Data="INSERT INTO " + MySQLiteHelper.TABLE_CONTACTS +
                " (" + MySQLiteHelper.COLUMN_CONTACT + ", " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +
                ", " + MySQLiteHelper.COLUMN_PUBLIC_KEY  + ") " +
                " VALUES ('" + contact.getName() + "', 'false', '" + keyHandeler.getSerializationFromKey(contact.getPubKey()) + "')";
        database.execSQL(Insert_Data);
    }

    public String getContactNameFromDb(int contactId)
    {
        String contactName = "unknown";
        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                allColumns,  MySQLiteHelper.COLUMN_CONTACT_ID + " = " + contactId , null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            contactName =cursor.getString(1);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contactName;
    }

    public int getContactIdFromDb(String contactName)
    {
        int contactId = 0;
        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                allColumns,  MySQLiteHelper.COLUMN_CONTACT + " = '" + contactName + "'", null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            contactId =Integer.parseInt(cursor.getString(0));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contactId;
    }


    public void close() {
        dbHelper.close();
    }
}