package com.example.mohsl.hardcore;

/**
 * Created by mohsl on 26.11.2014.
 */
public class Message {
    private int senderId;
    private int receiverId;
    private String messageText;

    public Message(int mSenderId, int mReceiverId, String mMessagetext) {
        senderId = mSenderId;
        receiverId = mReceiverId;
        messageText = mMessagetext;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getMessageText() {
        return messageText;
    }
}
