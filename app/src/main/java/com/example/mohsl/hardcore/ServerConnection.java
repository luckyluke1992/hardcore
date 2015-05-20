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
    private KeyHandler keyHandler;
    private AdressBook adressBook;

    public static ServerConnection getInstance()
    {
        if(instance == null)
        {
            instance = new ServerConnection();
        }
        return instance;
    }

    private  ServerConnection() {
        keyHandler = KeyHandler.getInstance();
        adressBook = AdressBook.getInstance();
    }

    public boolean registerUser(String regId)
    {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(MainActivity.getAppContext().getString(R.string.server_base_url) +"users/register");
        try {
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("username", adressBook.getUserName());
                jsonObj.put("regid", regId);
                jsonObj.put("publickey", keyHandler.getSerializationFromKey(keyHandler.getPubKey()));
                jsonObj.put("keytimestamp", "notImplementedYet");
                Log.i("Hardcore",jsonObj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            httppost.setEntity(entity);
            // Execute HTTP Post Request
            //TODO try to process response
            HttpResponse response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Couldn´t connect to Server");
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Couldn´t connect to Server");
            return false;
        }
        return true;
    }
    public boolean pushMessage(String receiverName, String message)
    {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(MainActivity.getAppContext().getString(R.string.server_base_url) + "messages/send");
        String[] encryption = keyHandler.getEncryptedMessageAndKeyBlock(message, adressBook.getContact(receiverName).getPubKey());
        try {
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("content", encryption[0]); //encrypted message
                jsonObj.put("receiver", receiverName);
                jsonObj.put("sender", adressBook.getUserName());
                jsonObj.put("keyblock", encryption[1]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            httppost.setEntity(entity);
            // Execute HTTP Post Request
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag),jsonObj.toString());
            //TODO process response
            HttpResponse response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Couldn´t connect to Server");
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Couldn´t connect to Server");
            return false;
        }
        return true;
    }

    public Key requestPubKey(String name){

        StringBuffer res = new StringBuffer();
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(MainActivity.getAppContext().getString(R.string.server_base_url) +"users/get/" + name);
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
                Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Couldn´t conect to Server");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Key key = null;

        JSONObject obj = null;
        try {
            obj = new JSONObject(res.toString());
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), res.toString());
            key = keyHandler.getKeyFromSerialization(obj.getString("publickey"));
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), obj.getString("publickey"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return key;
    }

    public int checkIfContactExists(String name) { // 0 -> false, 1 --> true, 2--> Connection issue
        StringBuffer res = new StringBuffer();
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL( MainActivity.getAppContext().getString(R.string.server_base_url) +"users/get/" + name);
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
            Log.i(String.valueOf(R.string.debug_tag), "couldnt connect to server");
            Log.i(String.valueOf(R.string.debug_tag), "connecting to url: " + MainActivity.getAppContext().getString(R.string.server_base_url) +"users/get/" + name);
           return 2;
        }
        JSONObject obj = null;
        try {
            obj = new JSONObject(res.toString());
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), res.toString());
            if(obj.has("publickey")){
                return 1;
            }
            else{
                return 0;
            }

        } catch (JSONException e) {
            Log.i(String.valueOf(R.string.debug_tag), "problem with json: " + obj.toString());
            return 2;
        }
    }
}
