package com.example.mohsl.hardcore;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DisplayMessageActivity extends Activity {

    private DataSource datasource;
    private ServerConnection serverConnection;
    private AdressBook adressBook;

    private ListView conversationHistoryView;
    private ImageButton sendButton;
    private EditText InputTextField;
    private String contactName;
    private boolean isActive=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        InputTextField = (EditText) findViewById(R.id.editText1);

        conversationHistoryView = (ListView) findViewById(R.id.conversation_history_layout);

        datasource = DataSource.getInstance(this);
        Intent intent = getIntent();
        contactName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        serverConnection = ServerConnection.getInstance();
        adressBook = AdressBook.getInstance();

        setTitle(contactName);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("refreshMessageView"));

        refreshView();
        ImageButton sendButton =(ImageButton)findViewById(R.id.sendButton);
        sendButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (InputTextField.getText().length() > 0) {
                    String messagetext = InputTextField.getText().toString();
                    if(serverConnection.pushMessage(contactName, messagetext)){
                        Message messageObject = new Message(adressBook.getUserId(), adressBook.getContactId(contactName), messagetext);
                        datasource.storeMessageInDb(messageObject);
                        refreshView();
                        InputTextField.setText("");
                    }
                    else{
                        Toast.makeText(getApplicationContext(), R.string.error_message_when_no_server_connection,
                                Toast.LENGTH_LONG).show();
                    }

                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_message, menu);
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

        return super.onOptionsItemSelected(item);
    }

    public void refreshView()
    {
        adressBook.setReadMessage(contactName);
        List<Message> conversationHistory = datasource.getConversationHistoryFromDb(adressBook.getContactId(contactName));
        final ListView listview = (ListView) findViewById(R.id.conversation_history_layout);
        List<String> conversationList = new ArrayList<String>();
        for(int i=0; i<conversationHistory.size();i++)
        {
                conversationList.add(conversationHistory.get(i).getMessageText());
        }
        ArrayAdapter adapter = new MessageViewListAdapter(this, conversationList, conversationHistory);

        listview.setAdapter(adapter);

    }
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action correctlynamed is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.i(getString(R.string.debug_tag),"message:" + message);
            if(message.equals(contactName) && isActive){
                NotificationManager notifManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notifManager.cancelAll();
                refreshView();
            }
        }
    };
    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        isActive=true;
        super.onResume();
    }
    @Override
    protected void onPause() {
        isActive = false;
        super.onResume();
    }
}
