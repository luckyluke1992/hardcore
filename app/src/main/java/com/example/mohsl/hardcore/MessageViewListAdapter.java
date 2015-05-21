package com.example.mohsl.hardcore;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by mohsl on 11.03.2015.
 */
public class MessageViewListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> itemname;
    private final List<Message> messages;
    //private final List<Integer> imgid; //not impleemnted yet

    public MessageViewListAdapter(Activity context, List<String> itemname, List<Message> mMessages) {
        super(context, R.layout.contact_list, itemname);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.itemname=itemname;
        this.messages = mMessages;
        //this.imgid=imgid;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.message_list_item, null, true);


        TextView messageTextField = (TextView) rowView.findViewById(R.id.item);
        //ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        //TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);
        //Log.i(getContext().getString(R.string.debug_tag), messages.get(position).getReceiverId() +"   " + messages.get(position).getSenderId() + " "+ (messages.get(position).getSenderId() == 0));
        if((messages.get(position)).getSenderId() == 1) { // 1 is always own id
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageTextField.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            messageTextField.setLayoutParams(params);
        }
        else{
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageTextField.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            messageTextField.setTextColor(R.color.green); //TODO: Fix color issue
            messageTextField.setLayoutParams(params);
        }

        messageTextField.setText(itemname.get(position));
        //imageView.setImageResource(R.drawable.powered_by_google_light);
        //extratxt.setText("Description "+itemname[position]);
        return rowView;

    }
}