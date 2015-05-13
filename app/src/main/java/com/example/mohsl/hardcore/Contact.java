package com.example.mohsl.hardcore;

import java.security.Key;

/**
 * Created by mohsl on 11.03.2015.
 */
public class Contact {
    private int id;
    private String name;
    private boolean unreadMessageAvailable;
    private Key pubKey;

    public Contact(int mId, String mName, boolean mUnreadmessageAvailable, Key mPubKey){
         id = mId;
        pubKey=mPubKey;
        name = mName;
        unreadMessageAvailable = mUnreadmessageAvailable;
    }

    public int getId(){return id;}
    public String getName(){
        return name;
    }
    public boolean isUnreadMessageAvailable(){
        return unreadMessageAvailable;
    }
    public void setUnreadMessageAvailable(boolean isAvailable){
        unreadMessageAvailable = isAvailable;
    }
    public Key getPubKey(){
        return pubKey;
    }
}

