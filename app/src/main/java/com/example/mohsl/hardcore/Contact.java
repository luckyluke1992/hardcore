package com.example.mohsl.hardcore;

import java.security.Key;
import java.security.PublicKey;

/**
 * Created by mohsl on 11.03.2015.
 */
public class Contact {
    private int id;
    private String name;
    private boolean unreadMessageAvailable;
    private PublicKey pubKey;

    public Contact(int mId, String mName, boolean mUnreadmessageAvailable, PublicKey mPubKey){
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
    public PublicKey getPubKey(){
        return pubKey;
    }
}

