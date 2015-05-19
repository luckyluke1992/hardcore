package com.example.mohsl.hardcore;

import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;


public class ServerConnection {

    private static ServerConnection instance;
    private HardcoreDataSource datasource;
    private KeyHandler keyHandler;
    private AdressBook adressBook;
    public static final String TAG = "Hardcore";

    public static ServerConnection getInstance()
    {
        if(instance == null)
        {
            instance = new ServerConnection();
        }
        return instance;
    }

    public ServerConnection() {
        datasource = HardcoreDataSource.getInstance(MainActivity.getAppContext());
        keyHandler = KeyHandler.getInstance();
        adressBook = AdressBook.getInstance();
    }

    public void pull() //Shouldn´t be used anymore
    {
        StringBuffer res = new StringBuffer();
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("http://luckyluke.selfhost.bz:1337/messages/" + MainActivity.getUserName());
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection != null) {
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("GET");

                //int responseCode = urlConnection.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    res.append(inputLine);
                }
                in.close();
                urlConnection.disconnect();
            } else {
                Log.i(TAG, "Couldn´t connect to Server");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //JSON parsing
        String s = res.toString();
        JSONArray reader = null;
        JSONObject messageObject = null;
        int senderId=0;
        int receiverId=0;
        String message="null";
        try {
            reader = new JSONArray(s); // array containing all messages
            //go through all messages and store them
            for(int i=0; i<reader.length();i++) {
                messageObject = reader.getJSONObject(i);
                //MainActivity.fillBox(messageObject.toString()); // because debug in android is cancer
                senderId = adressBook.getContactId(messageObject.getString("sender"));
                receiverId = adressBook.getContactId(messageObject.getString("receiver"));
                message = messageObject.getString("content");
                datasource.storeMessage(senderId,receiverId, message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void registerUser(String regId)
    {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://luckyluke.selfhost.bz:1337/users/register");
        String responseText = "undefined";

        try {
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("username", MainActivity.getUserName());
                jsonObj.put("regid", regId);
                jsonObj.put("publickey", keyHandler.getSerializationFromKey(keyHandler.getPubKey()));
                jsonObj.put("keytimestamp", "notImplementedYet");
                Log.i("Hardcore",jsonObj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            httppost.setEntity(entity);

            // Execute HTTP Post Request

            //try to process response
            //TODO
            HttpResponse response = httpclient.execute(httppost);
            responseText = response.getStatusLine().getReasonPhrase();
            int rsp = response.getStatusLine().getStatusCode();
            Log.i(TAG,"Response from Java Server was:" + responseText + "; " + rsp);

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, "Couldn´t connect to Server");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, "Couldn´t connect to Server");
        }
    }
    public boolean pushMessage(String receiverName, String message)
    {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://luckyluke.selfhost.bz:1337/messages/send");

         String[] encryption = keyHandler.getEncryptedMessageAndKeyBlock(message, adressBook.getContact(receiverName).getPubKey());
        try {
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("content", encryption[0]); //encrypted message
                jsonObj.put("receiver", receiverName);
                jsonObj.put("sender", MainActivity.getUserName());
                jsonObj.put("keyblock", encryption[1]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            httppost.setEntity(entity);
            // Execute HTTP Post Request
            Log.i(TAG,jsonObj.toString());

            //TODO process response
            HttpResponse response = httpclient.execute(httppost);

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, "Couldn´t connect to Server");
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, "Couldn´t connect to Server");
            return false;
        }
        return true;
    }

    public Key requestPubKey(String name){

        StringBuffer res = new StringBuffer();
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("http://luckyluke.selfhost.bz:1337/users/get/" + name);
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection != null) {
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("GET");

                //TODO: int responseCode = urlConnection.getResponseCode();
                Toast toast = Toast.makeText(MainActivity.getAppContext(),urlConnection.getResponseMessage().toString(), Toast.LENGTH_LONG);
                toast.show();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    res.append(inputLine);
                }
                in.close();
                urlConnection.disconnect();
            } else {
                Log.i(TAG, "Couldn´t conect to Server");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Key key = null;

        JSONObject obj = null;
        try {
            obj = new JSONObject(res.toString());
            Log.i(TAG, res.toString());
            key = keyHandler.getKeyFromSerialization(obj.getString("publickey"));
            Log.i(TAG, obj.getString("publickey"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return key;
    }

    public int checkIfContactExists(String name) {
        StringBuffer res = new StringBuffer();
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("http://luckyluke.selfhost.bz:1337/users/get/" + name);
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection != null) {
                urlConnection.setReadTimeout(1000);
                urlConnection.setConnectTimeout(1500);
                urlConnection.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    res.append(inputLine);
                }
                in.close();
                urlConnection.disconnect();
            } else {
                return 2;
            }
        } catch (Exception e) {
           return 2;
        }
        JSONObject obj = null;
        try {
            obj = new JSONObject(res.toString());
            Log.i(TAG, res.toString());
            if(obj.has("publickey")){
                return 1;
            }
            else{
                return 0;
            }

        } catch (JSONException e) {
            return 2;
        }
    }
}
