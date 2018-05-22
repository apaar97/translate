package com.example.apaar97.translate;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static com.example.apaar97.translate.GlobalVars.BASE_REQ_URL;
import static com.example.apaar97.translate.GlobalVars.DEFAULT_LANG_POS;
import static com.example.apaar97.translate.GlobalVars.LANGUAGE_CODES;

public class TranslationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    public static final String LOG_TAG = TranslationActivity.class.getName();
    private static final int REQ_CODE_SPEECH_INPUT = 1;

    private TextToSpeech mTextToSpeech;                     //    Text to Speech Engine
    private Spinner mSpinnerLanguageFrom;                   //    Dropdown list for selecting base language (From)
    private Spinner mSpinnerLanguageTo;                     //    Dropdown list for selecting translation language (To)
    private String mLanguageCodeFrom = "en";                //    Language Code (From)
    private String mLanguageCodeTo = "en";                  //    Language Code (To)
    private ImageView mImageSpeak;                          //    Speak button to read out translated text (Text to Speech)
    private EditText mTextInput;                            //    Input text ( in From language )
    private TextView mTextTranslated;                       //    Output Translated text ( in To language )
    private Dialog process_tts;                             //    Dialog box for Text to Speech Engine Language Switch
    HashMap<String, String> map = new HashMap<>();
    volatile boolean activityRunning;                       //    To track status of current activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation);

        activityRunning=true;
        TextView mEmptyTextView = (TextView) findViewById(R.id.empty_view_not_connected);
        mSpinnerLanguageFrom = (Spinner) findViewById(R.id.spinner_language_from);
        mSpinnerLanguageTo = (Spinner) findViewById(R.id.spinner_language_to);
        Button mButtonTranslate = (Button) findViewById(R.id.button_translate);         //      Translate button to translate text
        ImageView mImageSwap = (ImageView) findViewById(R.id.image_swap);               //      Swap Language button to swap languages
        ImageView mImageListen = (ImageView) findViewById(R.id.image_listen);           //      Mic button for Speech to text
        mImageSpeak = (ImageView) findViewById(R.id.image_speak);
        ImageView mClearText = (ImageView) findViewById(R.id.clear_text);               //      Clear button to clear text fields
        mTextInput = (EditText) findViewById(R.id.text_input);
        mTextTranslated = (TextView) findViewById(R.id.text_translated);
        mTextTranslated.setMovementMethod(new ScrollingMovementMethod());
        process_tts = new Dialog(TranslationActivity.this);
        process_tts.setContentView(R.layout.dialog_processing_tts);
        process_tts.setTitle(getString(R.string.process_tts));
        TextView title = (TextView) process_tts.findViewById(android.R.id.title);
        // title.setSingleLine(false);
        mTextToSpeech = new TextToSpeech(this, this);

        //  CHECK INTERNET CONNECTION
        if (!isOnline()) {
            mEmptyTextView.setVisibility(View.VISIBLE);
        } else {
            mEmptyTextView.setVisibility(View.GONE);
            //  GET LANGUAGES LIST
            new GetLanguages().execute();
            //  SPEECH TO TEXT
            mImageListen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLanguageCodeFrom);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
                    try {
                        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                    } catch (ActivityNotFoundException a) {
                        Toast.makeText(getApplicationContext(), getString(R.string.language_not_supported), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            //  TEXT TO SPEECH
            mImageSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    speakOut();
                }
            });
            //  TRANSLATE
            mButtonTranslate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String input = mTextInput.getText().toString();
                    new TranslateText().execute(input);
                }
            });
            //  SWAP BUTTON
            mImageSwap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String temp = mLanguageCodeFrom;
                    mLanguageCodeFrom = mLanguageCodeTo;
                    mLanguageCodeTo = temp;
                    int posFrom = mSpinnerLanguageFrom.getSelectedItemPosition();
                    int posTo = mSpinnerLanguageTo.getSelectedItemPosition();
                    mSpinnerLanguageFrom.setSelection(posTo);
                    mSpinnerLanguageTo.setSelection(posFrom);
                    String textFrom = mTextInput.getText().toString();
                    String textTo = mTextTranslated.getText().toString();
                    mTextInput.setText(textTo);
                    mTextTranslated.setText(textFrom);
                }
            });
            //  CLEAR TEXT
            mClearText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextInput.setText("");
                    mTextTranslated.setText("");
                }
            });
            //  SPINNER LANGUAGE FROM
            mSpinnerLanguageFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mLanguageCodeFrom = LANGUAGE_CODES.get(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Toast.makeText(getApplicationContext(), "No option selected", Toast.LENGTH_SHORT).show();
                }
            });
            //  SPINNER LANGUAGE TO
            mSpinnerLanguageTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mLanguageCodeTo = LANGUAGE_CODES.get(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Toast.makeText(getApplicationContext(), "No option selected", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    //  CHECK INTERNET CONNECTION
    public  boolean isOnline()
    {   try {
            ConnectivityManager connectivityManager = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return false;
    }
    //  RESULT OF SPEECH INPUT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    /*
                            Dialog box to show list of processed Speech to text results
                            User selects matching text to display in chat
                     */
                    final Dialog match_text_dialog = new Dialog(TranslationActivity.this);
                    match_text_dialog.setContentView(R.layout.dialog_matches_frag);
                    match_text_dialog.setTitle(getString(R.string.select_matching_text));
                    ListView textlist = (ListView)match_text_dialog.findViewById(R.id.list);
                    final ArrayList<String> matches_text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,matches_text);
                    textlist.setAdapter(adapter);
                    textlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mTextInput.setText(matches_text.get(position));
                            match_text_dialog.dismiss();
                        }
                    });
                    match_text_dialog.show();
                    break;
                }
            }
        }
    }
    //  INITIALISE TEXT TO SPEECH ENGINE
    @Override
    public void onInit(int status) {
        Log.e("Inside----->", "onInit");
        if (status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(new Locale("en"));
            if (result == TextToSpeech.LANG_MISSING_DATA) {
                Toast.makeText(getApplicationContext(), getString(R.string.language_pack_missing), Toast.LENGTH_SHORT).show();
            } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), getString(R.string.language_not_supported), Toast.LENGTH_SHORT).show();
            }
            mImageSpeak.setEnabled(true);
            mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.e("Inside","OnStart");
                    process_tts.hide();
                }
                @Override
                public void onDone(String utteranceId) {
                }
                @Override
                public void onError(String utteranceId) {
                }
            });
        } else {
            Log.e(LOG_TAG,"TTS Initilization Failed");
        }
    }
    //  TEXT TO SPEECH ACTION
    @SuppressWarnings("deprecation")
    private void speakOut(){
        int result = mTextToSpeech.setLanguage(new Locale(mLanguageCodeTo));
        Log.e("Inside","speakOut "+mLanguageCodeTo+" "+result);
        if (result == TextToSpeech.LANG_MISSING_DATA ){
            Toast.makeText(getApplicationContext(),getString(R.string.language_pack_missing),Toast.LENGTH_SHORT).show();
            Intent installIntent = new Intent();
            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            startActivity(installIntent);
        } else if(result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(getApplicationContext(),getString(R.string.language_not_supported),Toast.LENGTH_SHORT).show();
        } else {
            String textMessage = mTextTranslated.getText().toString();
            process_tts.show();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");
            mTextToSpeech.speak(textMessage, TextToSpeech.QUEUE_FLUSH, map);
        }
    }
    //  WHEN ACTIVITY IS PAUSED
    @Override
    protected void onPause() {
        if(mTextToSpeech!=null){
            mTextToSpeech.stop();
        }
        super.onPause();
    }
    //  WHEN ACTIVITY IS DESTROYED
    @Override
    public void onDestroy() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        activityRunning=false;
        process_tts.dismiss();
        super.onDestroy();
    }
    //  SUBCLASS TO TRANSLATE TEXT ON BACKGROUND THREAD
    private class TranslateText extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... input) {
            Uri baseUri = Uri.parse(BASE_REQ_URL);
            Uri.Builder uriBuilder = baseUri.buildUpon();
            uriBuilder.appendPath("translate")
                    .appendQueryParameter("key",getString(R.string.API_KEY))
                    .appendQueryParameter("lang",mLanguageCodeFrom+"-"+mLanguageCodeTo)
                    .appendQueryParameter("text",input[0]);
            Log.e("String Url ---->",uriBuilder.toString());
            return QueryUtils.fetchTranslation(uriBuilder.toString());
        }
        @Override
        protected void onPostExecute(String result) {
            if(activityRunning) {
                mTextTranslated.setText(result);
            }
        }
    }
    //  SUBCLASS TO GET LIST OF LANGUAGES ON BACKGROUND THREAD
    private class GetLanguages extends AsyncTask<Void,Void,ArrayList<String>>{
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            Uri baseUri = Uri.parse(BASE_REQ_URL);
            Uri.Builder uriBuilder = baseUri.buildUpon();
            uriBuilder.appendPath("getLangs")
                    .appendQueryParameter("key",getString(R.string.API_KEY))
                    .appendQueryParameter("ui","en");
            Log.e("String Url ---->",uriBuilder.toString());
            return QueryUtils.fetchLanguages(uriBuilder.toString());
        }
        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (activityRunning) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TranslationActivity.this, android.R.layout.simple_spinner_item, result);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinnerLanguageFrom.setAdapter(adapter);
                mSpinnerLanguageTo.setAdapter(adapter);
                //  SET DEFAULT LANGUAGE SELECTIONS
                mSpinnerLanguageFrom.setSelection(DEFAULT_LANG_POS);
                mSpinnerLanguageTo.setSelection(DEFAULT_LANG_POS);
            }
        }
    }
}