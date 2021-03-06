package com.example.mohsl.hardcore;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HardcoreDataSource {

    private static KeyHandler keyHandeler;
    private static HardcoreDataSource instance;
    // Database fields
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] allColumns = {MySQLiteHelper.COLUMN_CONTACT_ID,
            MySQLiteHelper.COLUMN_CONTACT, MySQLiteHelper.COLUMN_PUBLIC_KEY, MySQLiteHelper.COLUMN_UNREAD_MESSAGE};

    public static HardcoreDataSource getInstance(Context context)
    {
        if(instance == null) {
            if(context == null)
            {
                throw new NullPointerException("context cannot be null for the first initialization");
            }
            instance = new HardcoreDataSource(context);
            keyHandeler = KeyHandler.getInstance();
        }
        return instance;
    }
    public HardcoreDataSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
        database = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(database,1,1);
    }


    public List<Integer> getAllContacts() {
        List<Integer> contacts = new ArrayList<Integer>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            contacts.add(Integer.parseInt(cursor.getString(0)));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contacts;
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
            contacts.add(new Contact(Integer.parseInt(cursor.getString(0)),cursor.getString(1), Boolean.parseBoolean(cursor.getString(3)), keyHandeler.getKeyFromSerialization(cursor.getString(2))));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contacts;
    }

    public Contact getContact(int contactId) {
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

    /*
    public List<String> getAllContactNames() {
        List<String> contacts = new ArrayList<String>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                new String[]{MySQLiteHelper.COLUMN_CONTACT_ID, MySQLiteHelper.COLUMN_CONTACT} , null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            contacts.add(cursor.getString(1));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return contacts;
    }*/

    public List<Message> getConversationHistory(int contactId) {
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

    public void storeMessage(int senderId, int receiverId, String message)
    {
        final String Insert_Data="INSERT INTO " + MySQLiteHelper.TABLE_MESSAGES +
                " VALUES( null, " + senderId + ", " + receiverId + ", '" + message + "' )";
        database.execSQL(Insert_Data);
    }

    public void storeUnreadMessageInDb(String sendername){
        final String Insert_Data="UPDATE " + MySQLiteHelper.TABLE_CONTACTS +
                " SET " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +"='true' WHERE " +
                MySQLiteHelper.COLUMN_CONTACT + "= '" + sendername +"'";
        database.execSQL(Insert_Data);
    }
/*
    public boolean getUnreadMessageAvailable(int contactId){
        boolean available = false;

        Cursor cursor = database.query(MySQLiteHelper.TABLE_CONTACTS,
                new String[]{MySQLiteHelper.COLUMN_MESSAGE_ID, MySQLiteHelper.COLUMN_UNREAD_MESSAGE,},  MySQLiteHelper.COLUMN_CONTACT_ID + " = " + contactId , null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            available =Boolean.getBoolean(cursor.getString(1));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return available;
    }*/

    public void storeReadMessageInDb(String sendername){
        final String Insert_Data="UPDATE " + MySQLiteHelper.TABLE_CONTACTS +
                " SET " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +"='false' WHERE " +
                MySQLiteHelper.COLUMN_CONTACT + "= '" + sendername + "'";
        database.execSQL(Insert_Data);
    }

    public void storeContact(Contact contact){

        final String Insert_Data="INSERT INTO " + MySQLiteHelper.TABLE_CONTACTS +
                " (" + MySQLiteHelper.COLUMN_CONTACT + ", " + MySQLiteHelper.COLUMN_UNREAD_MESSAGE +
                ", " + MySQLiteHelper.COLUMN_PUBLIC_KEY  + ") " +
                " VALUES ('" + contact.getName() + "', 'false', '" + keyHandeler.getSerializationFromKey(contact.getPubKey()) + "')";
        database.execSQL(Insert_Data);
    }

    public String getContactName(int contactId)
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
    /*
    public boolean storeNewContact(String contactName, Key pubKey)
    {

    }
*/
    public int getContactId(String contactName)
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

    public void clearConversationHistory(String contactName)
    {
        int SenderId = getContactId(MainActivity.getUserName());
        //int ReceiverId = getContactId(contactName);

        final String DeleteMessages= "DELETE FROM " +MySQLiteHelper.TABLE_MESSAGES +
                " WHERE " + MySQLiteHelper.COLUMN_SENDER_ID +" <> " + SenderId +";";

        database.execSQL(DeleteMessages);
        //database.execSQL("DELETE FROM " + MySQLiteHelper.TABLE_MESSAGES);
    }

    public void close() {
        dbHelper.close();
    }
}