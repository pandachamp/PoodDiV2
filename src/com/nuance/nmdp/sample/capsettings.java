package com.nuance.nmdp.sample;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by admin on 1/14/15 AD.
 */
public class capsettings extends ActionBarActivity {

    String server;
    int port;
    String mount;
    String user = "source";
    String pass;

    String tomount;

    EditText eServer = null;
    EditText ePort = null;
    EditText eMount = null;
    EditText eUser = null;
    EditText ePass = null;

    Button ButApply = null;
    Button ButCancle = null;
    Button ButTT=null;

    SharedPreferences mPrefs;
    SharedPreferences.Editor editS;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String para = extras.getString(DictationView.Byakko);
        }
        mPrefs = getSharedPreferences("gissmp3", Context.MODE_PRIVATE);

        server = mPrefs.getString("giss_server", "202.44.12.173");
        port = mPrefs.getInt("giss_port",8000);
        mount = mPrefs.getString("giss_mountpoint","kmutt.mp3");
        pass = mPrefs.getString("giss_password","ser");
        editS = mPrefs.edit();

        eServer = (EditText)findViewById(R.id.editServer);
        eServer.setText(server);
        ePort = (EditText)findViewById(R.id.editPort);
        ePort.setText(String.valueOf(port));
        eMount = (EditText)findViewById(R.id.editMP);
        tomount = eMount.getText().toString();
        eMount.setText(mount);
        eUser = (EditText)findViewById(R.id.editUser);
        eUser.setText(user);
        ePass = (EditText)findViewById(R.id.editPassword);
        ePass.setText(pass);


        ButApply = (Button) findViewById(R.id.apply);
        ButApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tomount = eMount.getText().toString();
                if(tomount.length()>0){
                    editS.putString("giss_mountpoint",tomount);
                    editS.commit();
                }
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(eMount.getWindowToken(), 0);
                Bundle b = new Bundle();
                b.putString(DictationView.Byakko,"Send success");
                Intent i = new Intent();
                i.putExtras(b);
                if (getParent() == null) {
                    setResult(Activity.RESULT_OK, i);
                } else {
                    getParent().setResult(Activity.RESULT_OK, i);
                }
                finish();
            }
        });

        ButCancle = (Button) findViewById(R.id.cancel);
        ButCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(eMount.getWindowToken(), 0);
                Bundle b = new Bundle();
                b.putString(DictationView.Byakko,"Send success");
                Intent i = new Intent();
                i.putExtras(b);
                if (getParent() == null) {
                    setResult(Activity.RESULT_CANCELED, i);
                } else {
                    getParent().setResult(Activity.RESULT_CANCELED, i);
                }
                finish();
            }
        });
    }
}
