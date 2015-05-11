package com.example.mohsl.hardcore;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.app.Notification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public final static String EXTRA_MESSAGE = "com.example.hardcore.MESSAGE";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "Hardcore";
    private HardcoreDataSource datasource;
    private KeyHandler keyHandler;

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        Log.i(TAG,"GCM intent released");
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
               //TODO: sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
               //TODO: sendNotification("Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.

                Log.i(TAG, "Received: " + extras.toString());

                sendNotification(extras);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(Bundle data) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        //Message parsing
        datasource = HardcoreDataSource.getInstance(this);
        Map<String, String> messageMap = new HashMap<>();

        //ugly parsing
/*
        String s = msg.toString().replace("[","");
        s = s.toString().replace("]","");
        s = s.replace("{","");
        s = s.replace("}","");
        s = s.split("Bundle")[1];
        String[] messageArray = s.split(",");


        for(int i=0; i<messageArray.length; i++){
            s = messageArray[i].trim();
            String[] temp = s.split("=");
            messageMap.put(temp[0], temp[1]);
        }
*/
        // nice parsing

        String content =  data.getString("content");
        String sender =  data.getString("sender");

        messageMap.put("sender", sender);
        messageMap.put("content", content);

        keyHandler=KeyHandler.getInstance();
        //decrypt Message
        Log.i(TAG,messageMap.toString());
        String encryptedMessage = keyHandler.decryptMessage(messageMap.get("content"));
        Log.i(TAG,encryptedMessage);
        messageMap.put("content", encryptedMessage);


        long[] pattern= {0,500,0};
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher) //originally: R.drawable.ic_stat_gcm
                        .setContentTitle("Hardcore Message")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(messageMap.get("sender") + ": " + messageMap.get("content")))
                        .setContentText(messageMap.get("sender") + ": " + messageMap.get("content"))
                        .setVisibility(android.app.Notification.VISIBILITY_PRIVATE)
                        .setVibrate(pattern)
                        .setLights(Color.MAGENTA,500,500);


        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        int senderId=0;
        int receiverId=0;
        senderId = datasource.getContactId(messageMap.get("sender"));
        receiverId = datasource.getContactId(MainActivity.getUserName());
        datasource.storeMessage(senderId,receiverId, messageMap.get("content"));
        datasource.setUnreadMessage(senderId);

        //send intent to mainActivity for refresh purpose:
        Intent intentToMain = new Intent("refreshMainView");
        // You can also include some extra data.
        //intentToMain.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentToMain);

        //send intent to MessageViewActivity for refresh purpose:
        Intent intentToMessageView = new Intent("refreshMessageView");
        // You can also include some extra data.
        intentToMessageView.putExtra("message", messageMap.get("sender"));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentToMessageView);
    }
}
