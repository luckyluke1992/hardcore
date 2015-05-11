package com.example.mohsl.hardcore;

import java.security.Key;

/**
 * Created by mohsl on 11.03.2015.
 */
public class Contact {
    private String name;
    private boolean unreadMessageAvailable;
    private Key pubKey;

    public Contact(String mName, boolean mUnreadmessageAvailable, Key mPubKey){
        pubKey=mPubKey;
        name = mName;
        unreadMessageAvailable = mUnreadmessageAvailable;
    }

    public String getName(){
        return name;
    }
    public boolean isUnreadMessageAvailable(){
        return unreadMessageAvailable;
    }
    public Key getPubKey(){
        return pubKey;
    }
}

