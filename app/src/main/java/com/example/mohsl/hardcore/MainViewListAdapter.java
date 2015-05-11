package com.example.mohsl.hardcore;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by mohsl on 11.03.2015.
 */
public class MainViewListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> itemname;
    private final List<Contact> contacts;
    //private final List<Integer> imgid; //not impleemnted yet

    public MainViewListAdapter(Activity context, List<String> itemname, List<Contact> mContacts) {
        super(context, R.layout.mylist, itemname);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.itemname=itemname;
        this.contacts = mContacts;
        //this.imgid=imgid;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.mylist, null,true);

        if((contacts.get(position)).isUnreadMessageAvailable() ) {
            rowView.setBackgroundColor(Color.RED);
        }
        TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        //TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);

        if((contacts.get(position)).isUnreadMessageAvailable() ) {
            imageView.setBackgroundColor(Color.RED);
        }
        txtTitle.setText(itemname.get(position));
        imageView.setImageResource(R.drawable.ic_launcher);
        //extratxt.setText("Description "+itemname[position]);
        return rowView;

    };
}