package com.nuance.nmdp.sample;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by admin on 1/14/15 AD.
 */
public class capabout extends ActionBarActivity {
    SharedPreferences mPrefs;
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String para = extras.getString(DictationView.Byakko);
        }
        mPrefs = getSharedPreferences("gissmp3", Context.MODE_PRIVATE);
    }
}
