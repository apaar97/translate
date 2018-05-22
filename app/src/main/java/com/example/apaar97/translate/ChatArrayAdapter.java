package com.example.apaar97.translate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/*
        Class : Defines Custom Array Adapter Class : Chat Array Adapter
                Modified to from list view of objects of type ChatMessage
 */
class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

    private ArrayList<ChatMessage> mListChatMessages = new ArrayList<>();

    @Override
    public void add(ChatMessage object) {
        super.add(object);
        mListChatMessages.add(object);
    }

    @Override
    public void clear() {
        super.clear();
        mListChatMessages.clear();
    }

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public int getCount() {
        return this.mListChatMessages.size();
    }

    public ChatMessage getItem(int index) {
        return this.mListChatMessages.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage chatMessage = getItem(position);
        View row = convertView;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (chatMessage != null) {
            if (chatMessage.getmTranslate()) {
                if (chatMessage.getmLeft()) {
                    row = inflater.inflate(R.layout.chat_translate_left, parent, false);
                } else {
                    row = inflater.inflate(R.layout.chat_translate_right, parent, false);
                }
            } else {
                if (chatMessage.getmLeft()) {
                    row = inflater.inflate(R.layout.chat_left, parent, false);
                } else {
                    row = inflater.inflate(R.layout.chat_right, parent, false);
                }
            }
            TextView TextMessage = (TextView) row.findViewById(R.id.message);
            TextMessage.setText(chatMessage.getmMessage());
        }
        return row;
    }
}