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

    private  HardcoreDataSource datasource;
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

        datasource = HardcoreDataSource.getInstance(this);
        Intent intent = getIntent();
        contactName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        serverConnection = ServerConnection.getInstance();
        adressBook = AdressBook.getInstance();

        setTitle(contactName);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("refreshMessageView"));

        //GraphicShitzzz

        refreshView();
        ImageButton sendButton =(ImageButton)findViewById(R.id.sendButton);
        sendButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (InputTextField.getText().length() > 0) {
                    String message = InputTextField.getText().toString();
                    if(serverConnection.pushMessage(contactName, message)){
                        datasource.storeMessage(adressBook.getContactId(MainActivity.getUserName()), adressBook.getContactId(contactName), message);
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
        /*
        if(datasource.getUnreadMessageAvailable(datasource.getContactId(contactName))) {
            NotificationManager notifyManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notifyManager.cancelAll();
        }
        Log.i("Debug refreshView",""+datasource.getUnreadMessageAvailable(datasource.getContactId(contactName)) );
*/
        //datasource.clearConversationHistory(contactName);
        //serverConnection.pull();

        //change the message - read - status
        adressBook.setReadMessage(contactName);

        List<Message> conversationHistory = datasource.getConversationHistory(adressBook.getContactId(contactName));
        final ListView listview = (ListView) findViewById(R.id.conversation_history_layout);
        List<String> conversationList = new ArrayList<String>();
        for(int i=0; i<conversationHistory.size();i++)
        {
                conversationList.add(conversationHistory.get(i).getMessageText());
        }
        ArrayAdapter adapter = new MessageViewListAdapter(this, conversationList, conversationHistory);

        listview.setAdapter(adapter);
        /*
        TableRow r1;
        TableLayout.LayoutParams tableRowParams=
                new TableLayout.LayoutParams
                        (TableLayout.LayoutParams.FILL_PARENT,TableLayout.LayoutParams.WRAP_CONTENT);
        tableRowParams.setMargins(5,5,5,5);

        for(int i=0; i<conversationHistory.size();i++)
        {
            r1 = new TableRow(this);
            r1.setLayoutParams(tableRowParams);
            EditText messageText = new EditText(this);
            messageText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            if(conversationHistory.get(i).getReceiverId() == 0)
            {
                r1.setGravity(0);
                r1.setBackgroundColor(Color.GREEN);
            }
            messageText.setText("von: " + conversationHistory.get(i).getSenderId() + " an :" +
                    conversationHistory.get(i).getReceiverId() + ": " +conversationHistory.get(i).getMessageText());
            messageText.setKeyListener(null);
            r1.addView(messageText);
            conversationHistoryView.addView(r1);
        }*/
    }
    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
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
