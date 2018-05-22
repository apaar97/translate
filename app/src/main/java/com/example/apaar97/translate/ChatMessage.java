package com.example.apaar97.translate;

/*
        Class : Define Chat Message
                mLeft           :   Determines position of chat box on screen
                mTranslate      :   Determines the type of chat box ( Base Text / Translated Text )
                mMessage        :   Chat box Text
                mLanguage Code  :   Chat box Text Language Code
 */
public class ChatMessage {
    private boolean mLeft;
    private boolean mTranslate;
    private String mMessage;
    private String mLanguageCode;
    ChatMessage(boolean left,boolean translate,String message,String code){
        mLeft=left;
        mTranslate=translate;
        mMessage=message;
        mLanguageCode=code;
    }
    public String getmMessage() {
        return mMessage;
    }
    public boolean getmLeft(){
        return mLeft;
    }
    public String getmLanguageCode() { return mLanguageCode; }
    public boolean getmTranslate() { return mTranslate; }
}
