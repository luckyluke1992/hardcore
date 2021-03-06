package com.example.mohsl.hardcore;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends Activity {

    public final static String EXTRA_MESSAGE = "com.example.hardcore.MESSAGE";
    private KeyHandler keyHandler;
    private ServerConnection serverConnection;
    private AdressBook adressBook;
    public static final String PROPERTY_REG_ID = "registration_id";
    public static String PREFERENCE_FIRST_RUN ="first-run";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static String USERNAME = "undefined";
    private static int USERID = 0;
    String SENDER_ID = "759857875885";
    String regid;
    GoogleCloudMessaging gcm;
    private static Context context;

    public static String getUserName() {
        return USERNAME ;
    }
    public static int  getUserId() {
        return USERID;
    }

    private static TextView messageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //dirty context fix
        MainActivity.context = getApplicationContext();
        context = getApplicationContext();

        //Quick n Dirty
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Clean graphic initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageBox = (TextView) findViewById(R.id.textView1);

        //connect adressbook
        adressBook = AdressBook.getInstance();


        //conntec to db and establish network connection
        serverConnection = ServerConnection.getInstance();

        //connect to Keyhandler
        keyHandler = KeyHandler.getInstance();



        //Check wheter first execution:
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = p.getBoolean(PREFERENCE_FIRST_RUN, true);
        p.edit().putBoolean(PREFERENCE_FIRST_RUN, false).commit();
         //TODO: bit crappy, since prefs are accessed twice :S

        if(firstRun) {
            /*
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.input_instruction_for_a_friends_username));

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(getString(R.string.shared_prefs_username), input.getText().toString()).commit();
                    USERNAME = PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.shared_prefs_username),input.getText().toString());
                    USERID = datasource.getContactId(getUserName());
                    keyHandler.generateAndStoreKeys();
                    Contact contact = new Contact(USERNAME, false, keyHandler.getPubKey());
                    datasource.storeContact(contact);
                    startRegistration();
                }
            });
            builder.show();
            */
            //TODO: implement following Alertcode

            currentDialog =  new MaterialDialog.Builder(this)
                    .title(getString(R.string.input_dialog_to_input_own_username))
                    .content(getString(R.string.dialog_hint_when_selecting_own_username))
                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                    .positiveText(getString(R.string.dialog_select_own_username_proceed))
                    .autoDismiss(false)
                    .input(R.string.dialog_add_friend_input_hint, R.string.empty, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            int returnCode = serverConnection.checkIfContactExists(input.toString());
                            if (returnCode ==1 ) {
                                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(getString(R.string.shared_prefs_username), input.toString()).commit();
                                USERNAME = PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.shared_prefs_username), input.toString());
                                USERID = adressBook.getContactId(getUserName());
                                keyHandler.generateAndStoreKeys();
                                adressBook.storeOwnContact(USERNAME);
                                startRegistration();
                                dialog.dismiss();
                            } else if(returnCode == 0 ) {
                                dialog.setContent(getString(R.string.registration_username_taken_message));
                            }
                            else if(returnCode == 2){
                                dialog.setContent(getString(R.string.error_message_when_no_server_connection));
                            }
                        }
                    }).show();

        }
        else{
            USERNAME = PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.shared_prefs_username), getString(R.string.undefined));
            USERID = adressBook.getContactId(getUserName());
            keyHandler.readInKeys();
            startRegistration();
        }
        refreshView();
        /*
        List<Contact> contacts = datasource.getAllContactsFromDb();
        List<String> contactnames = datasource.getAllContactNames();

        ListView main = (ListView) findViewById(R.id.main_layout);
        CustomListAdapter adapter=new CustomListAdapter(this, contactnames, contacts);
        main.setOnItemClickListener(new sendMessageListener());
        main.setAdapter(adapter);*/
        //initialize intentReciever for refresh purposes
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.intent_refresh_main_view)));
    }/*
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, contacts);
        main.setOnItemClickListener(new sendMessageListener());
        main.setAdapter(modeAdapter);
    }
*/
    private void startRegistration(){
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(getString(R.string.debug_tag), "No valid Google Play Services APK found.");
        }
        Log.i(getString(R.string.debug_tag),"Estabblish connection and registered user");
        refreshView();
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(getString(R.string.debug_tag), "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(String.valueOf(R.string.debug_tag), "App version changed.");
            return "";
        }
        Log.i(getString(R.string.debug_tag), "RegistrationId found: " + registrationId);
        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    public class sendMessageListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(view.getContext(), DisplayMessageActivity.class);
            String selectedContact = (String) parent.getAdapter().getItem(position);
            intent.putExtra(EXTRA_MESSAGE, selectedContact);
            startActivity(intent);
        }
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    serverConnection.registerUser(regid);

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
               Log.i(String.valueOf(R.string.debug_tag),msg);
            }
        }.execute(null, null, null);
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(getString(R.string.debug_tag), "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id==R.id.action_new_message)
        {
            currentDialog =  new MaterialDialog.Builder(this)
                    .title(getString(R.string.instruction_message_to_input_name_to_search))
                    .content(R.string.dialog_add_friend_content)
                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                    .positiveText(R.string.search)
                    .autoDismiss(false)
                    .input(R.string.dialog_add_friend_input_hint, R.string.empty, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            // Do something
                            if(adressBook.isFriend(input.toString())){
                                dialog.setContent(getString(R.string.response_dialog_if_is_already_friend));
                            }
                            else if (1 == serverConnection.checkIfContactExists(input.toString())) {
                                confirmContact(input.toString());
                                dialog.dismiss();
                            } else if (0 ==  serverConnection.checkIfContactExists(input.toString())){
                                dialog.setContent(getString(R.string.response_dialog_when_searched_friend_not_found));
                            }
                            else if( 2 == serverConnection.checkIfContactExists(input.toString())){
                                dialog.setContent(getString(R.string.error_message_when_no_server_connection));
                            }
                        }
                    }).show();

            /*final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Input a friends name");
            dialog.setCancelable(true);

            final Context myActivity = this;
            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (serverConnection.checkIfContactExists(input.getText().toString())) {
                        Log.i(TAG, "contact exists");
                        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Contact newContact = new Contact(input.getText().toString(), false, serverConnection.requestPubKey(input.getText().toString()));
                                Log.i(TAG, newContact.toString());
                                datasource.storeContact(newContact);
                            }
                        });
                        //dialog.setView(input);
                        dialog.show();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.setView(input);
            dialog.show();*/
        }
        else if(id==R.id.action_search)
        {
            Toast toast = Toast.makeText(context,getString(R.string.not_implemented),Toast.LENGTH_SHORT);
            toast.show();
        }

        return super.onOptionsItemSelected(item);
    }

    public MaterialDialog currentDialog;

    public void confirmContact(String mContactName) {
        final String contactName = mContactName;
        if(currentDialog != null)
        {
            currentDialog.dismiss();
            currentDialog = null;
        }

        currentDialog = new MaterialDialog.Builder(this)
                .title(R.string.dialog_add_friend_conirm_dialog)
                .content(getString(R.string.dialog_add_friend_verification_message) + contactName.toString() + "?")
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        adressBook.storeNewContact(contactName.toString(), serverConnection.requestPubKey(contactName.toString()));
                        refreshView();
                    }
                })
                .show();
    }

    public static void fillBox(String debug) {
        messageBox.setText(debug);
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(getString(R.string.debug_tag), "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            //String message = intent.getStringExtra("message");
            //Log.d("receiver", "Got message: " + message);
            refreshView();
        }
    };
    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void refreshView(){
        ListView main = (ListView) findViewById(R.id.main_layout);
        MainViewListAdapter adapter=new MainViewListAdapter(this, adressBook.getAllContactNames(), adressBook.getContactList());
        main.setOnItemClickListener(new sendMessageListener());
        main.setAdapter(adapter);
        fillBox(getString(R.string.welcome_hello_message) + USERNAME + "!");
    }

    public static Context getAppContext() {
        return MainActivity.context;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshView();
    }
}