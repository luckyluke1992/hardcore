package com.example.mohsl.hardcore;

import android.util.Log;

import java.security.Key;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mohsl on 13.05.2015.
 */
public class AdressBook {

    private static AdressBook instance;
    private static DataSource dataSource;
    private static KeyHandler keyHandler;
    private  String USERNAME;
    private  int USERID = 1; // db starts at id 1

    private List<Contact> contactList;

    public static  AdressBook getInstance(){
        if(instance == null){
            instance = new AdressBook();
        }
        return instance;
    }

    private AdressBook(){;
        dataSource =  DataSource.getInstance(MainActivity.getAppContext());
        contactList = dataSource.getAllContactsFromDb();
        keyHandler = KeyHandler.getInstance();
    }

    public  String getUserName() {
        return USERNAME ;
    }
    public  int  getUserId() {
        return USERID;
    }
    public  void setUserName(String mUserName) {
        USERNAME = mUserName ;
    }

    public int getContactId(String name) {
        int id=0;
        for (int i=0; i<contactList.size();i++){
            //Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "element: " + i + " name:" + contactList.get(i).getName() + " id from db: " + contactList.get(i).getId());
            if(contactList.get(i).getName().equals(name)){
                id = contactList.get(i).getId();
            }
        }
        return id;
    }

    public void storeNewContact(String name, PublicKey publicKey){
        Contact newContact = new Contact(getNumberOfContacts(), name, false, publicKey);
        dataSource.storeContactInDb(newContact);
        contactList = dataSource.getAllContactsFromDb();
    }

    public int getNumberOfContacts(){
        return contactList.size();
    }

    public void storeOwnContact(String name){
        Contact newContact = new Contact(getNumberOfContacts(), name, false, keyHandler.getPubKey());
        dataSource.storeContactInDb(newContact);
        setUserName(name);
        contactList = dataSource.getAllContactsFromDb();
    }

    public List<String> getAllContactNames(){
        List<String> contactNameList = new ArrayList<>();
        for (int i=0; i<contactList.size();i++){
            contactNameList.add(contactList.get(i).getName());
        }
        return contactNameList;
    }

    public boolean isContact(String name) {
        boolean isFriend = false;
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).getName().equals(name)) {
                isFriend = true;
            }
        }
        return isFriend;
    }

    public Contact getContact(String name) {
        Contact specificContact = null;
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).getName().equals(name)) {
                specificContact = contactList.get(i);
            }
        }
        return specificContact;
    }

    public List<Contact> getContactList(){
        return contactList;
    }

    public void setReadMessage(String contactName){
        dataSource.storeReadMessageInDb(contactName);
        getContact(contactName).setUnreadMessageAvailable(false);
    }

    public void setUnreadMessage(String contactName){
        dataSource.storeUnreadMessageInDb(contactName);
        getContact(contactName).setUnreadMessageAvailable(true);
    }


}
