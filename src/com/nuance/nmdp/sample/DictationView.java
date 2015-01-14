package com.nuance.nmdp.sample;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Timer;

public class DictationView extends ActionBarActivity
{

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    Context context;
    Timer timer;
    int nbUpdates;
    public static  final String Byakko = "Byakko_Tamingz";
    private static final String TAG = "Caption Services";
    private static final int MAX_PACKETS_STACK = 1;
    private String longMsg;
    private String latMsg;
    private boolean initialized = false;
    private SharedPreferences mPrefs;
    //Default Parameters
    private String server = "202.44.12.173";
    private int port = 8000;
    private String mountpoint = "kmutt.mp3";
    private String password = "ser";
    private int abitrate = 64;
    private int achannels = 1;
    private int arate = 8000;
    private int mp3quality = 9; // from 1 to 9
    private String name = "Caption Service";
    private String description = "ENE KMUTT";
    private String genre = "Caption_Service Media";
    private String url = "http://ene.kmutt.ac.th";
    private int x_streaming = 0;
    private long x_starttime = 0;
    private long x_curtime = 0;
    //
    private AudioRecord arec;
    private short[] audioIn;
    private int nread;
    private int bSize;


    public class printHandler {
        public void print( String message )
        {
            Log.v(TAG, message);
        }
    };

    private printHandler myPrintHandler = new printHandler();

    public synchronized native void setPrintHook(printHandler handler);

    public synchronized native static void initStreaming(String server, int port, String mountpoint, String password,
                                                         int abitrate, int achannels, int arate, int mp3quality,
                                                         String name, String decription, String genre, String url);

    // init encoder
    private synchronized native void initEncoder();

    // init encoder
    private synchronized native String testAlloc();

    // shutdown encoder
    private synchronized native void shutdownEncoder();

    // connect to the server
    private synchronized native int connectStreaming();

    // write stream headers
    private synchronized native void writeHeaders();

    // write an audio block
    private synchronized native int isStreaming();

    // write an audio block
    private synchronized native void writeAudioBlock(short[] audioIn, int size);

    // disconnect from the server
    private synchronized native void disconnectStreaming();

    // release streaming structures
    private synchronized native void releaseStreaming();

    static {

        try {
            Log.v( TAG, "loading liblamenative.so");
            System.loadLibrary("lamenative");
        }
        catch (UnsatisfiedLinkError e) {
            Log.e( TAG, "Error: Could not load liblamenative.so : " + e.getMessage());
        }

        try {
            Log.v( TAG, "loading liblameclientnative.so");
            System.loadLibrary("lameclientnative");
        }
        catch (UnsatisfiedLinkError e) {
            Log.e( TAG, "Error: Could not load liblameclientnative.so : " + e.getMessage());
        }

    }
    //++++++++++++++++++++++++++++++++++++++++Method from Caption Service++++++++++++++++++++

    public synchronized void initStreaming() {

        // initialize streaming with all the parameters
        initStreaming(server, port, mountpoint, password,
                abitrate, achannels, arate, mp3quality,
                name, description, genre, url);

        //init encoder
        initEncoder();
    }
    public synchronized void startStreaming() {
        Log.d("Start Streaming","Start!!!!");
        int ret;

        // connect to the server
        if ( ( ret = connectStreaming() ) == 0 ) writeHeaders();

        // LinearLayout linlay = (LinearLayout) findViewById(R.id.LinearLayout1);
        TextView tstatus=(TextView)findViewById(R.id.textStatus);
        TextView ttime=(TextView)findViewById(R.id.textTime);
        TextView tlisten=(TextView)findViewById(R.id.textList);
        // Button stopButton = (Button) findViewById(R.id.stopb);

        if ( ret != 0 )
        {
            tstatus.setText( "Connection error" );
            ttime.setText( "00:00:00" );
            tlisten.setText( "" );
            //stopButton.setText( "Back" );
            //linlay.setBackgroundColor( Color.RED );
            return;
        }
        else
        {
            tstatus.setText( "Streaming" );
            ttime.setText( "00:00:00" );
            x_starttime = System.currentTimeMillis();
            tlisten.setText( "http://"+server+":"+port+"/"+mountpoint );
            //linlay.setBackgroundColor( Color.GREEN );
        }

        audioIn = new short[2*arate];

        //bSize from getMinBufferSize returns 1024 Now use as 2048
        bSize = 2048;
        //bSize = AudioRecord.getMinBufferSize( arate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT );
        Log.v( TAG, "audio buffer size  : "+bSize );

        arec = new AudioRecord(MediaRecorder.AudioSource.MIC, arate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*bSize );

        arate = arec.getSampleRate();
        Log.v( TAG, "sample rate : "+arate );

        int state = arec.getState();
        if ( state != AudioRecord.STATE_INITIALIZED )
        {
            Log.v( TAG, "problem initializing audio recorder : "+state );
        }
        else
        {
            Log.v( TAG, "audio recorder initialized : "+state );
        }

        // set the listener to get audio data
        arec.setRecordPositionUpdateListener( new AudioRecord.OnRecordPositionUpdateListener()
        {
            public void onMarkerReached(AudioRecord recorder)
            {
                // set maximum priority
                android.os.Process.setThreadPriority(-20);

                // send audio block
                if ( nread >= 0 )
                {
                    x_streaming = isStreaming();
                    Log.v( TAG, "isStream  : "+x_streaming );
                    if ( x_streaming == 1)
                    {
                        x_curtime = System.currentTimeMillis();
                        int tsecs = (int)(x_curtime-x_starttime)/1000;
                        int secs = tsecs%60;
                        int mins = ((int)tsecs/60)%60;
                        int hours = ((int)tsecs/3600);
                        String tTime;
                        if ( hours < 10 ) tTime = "0"+hours+":"; else tTime = hours+":";
                        if ( mins < 10 ) tTime += "0"+mins+":"; else tTime += mins+":";
                        if ( secs < 10 ) tTime += "0"+secs; else tTime += ""+secs;
                        TextView ttime=(TextView)findViewById(R.id.textTime);
                        ttime.setText( tTime );
                        writeAudioBlock(audioIn, nread);
                    }
                    if ( arec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING )
                    {
                        recorder.setNotificationMarkerPosition( bSize );
                        // reading audio ahead, silly trick
                        nread = recorder.read(audioIn, 0, arate );
                    }
                }
                else
                {
                    Log.v( TAG, "error recording sound : "+nread );
                }
            }
            public void onPeriodicNotification(AudioRecord recorder)
            {
                // send audio block
                if ( nread >= 0 )
                {
                    x_streaming = isStreaming();
                    if ( x_streaming == 1)
                    {
                        writeAudioBlock(audioIn, nread);
                    }
                    if ( arec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING )
                    {
                        recorder.setPositionNotificationPeriod( bSize );
                        // reading audio ahead, silly trick
                        nread = recorder.read(audioIn, 0, arate );
                        Log.v( TAG, "periodic "+nread+" samples" );
                    }
                }
                else
                {
                    Log.v( TAG, "error recording sound : "+nread );
                }
            }
        });

        // set marker position
        arec.setNotificationMarkerPosition( bSize );

        arec.startRecording();
        state = arec.getRecordingState();
        //state = 3;
        if ( state != AudioRecord.RECORDSTATE_RECORDING )
        {
            Log.v( TAG, "problem starting audio recorder : "+state );
        }
        else
        {
            Log.v( TAG, "audio recorder started : "+state );
        }

        // read first block to stream
        nread = arec.read(audioIn, 0, arec.getNotificationMarkerPosition() );

    }

    /** stop streaming                            */
    /** called when leaving the streaming screen  */
    public synchronized void shutdownStreaming() {

        TextView tstatus=(TextView)findViewById(R.id.textStatus);
        TextView ttime=(TextView)findViewById(R.id.textTime);
        TextView tlisten=(TextView)findViewById(R.id.textList);

        tstatus.setText("Ready to Stream");
        ttime.setText("");
        tlisten.setText("");

        // stop recording
        int state = arec.getRecordingState();
        if ( state == AudioRecord.RECORDSTATE_RECORDING )
        {
            arec.stop();
            arec.release();
        }

        // shutdown encoder resoources
        shutdownEncoder();

        // release streaming ressources
        releaseStreaming();
    }

    /** stop streaming                            */
    /** called when capture is stoped             */
    public synchronized void stopStreaming() {

        // disconnect from the server
        disconnectStreaming();

    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private static SpeechKit _speechKit;
    static SpeechKit getSpeechKit() {
        return _speechKit;
    }


    private static final int LISTENING_DIALOG = 0;
    private Handler _handler = null;
    private final Recognizer.Listener _listener;
    private Recognizer _currentRecognizer;
    private ListeningDialog _listeningDialog;
    private ArrayAdapter<String> _arrayAdapter;
    private boolean _destroyed;
    
    private class SavedState
    {
        String DialogText;
        String DialogLevel;
        boolean DialogRecording;
        Recognizer Recognizer;
        Handler Handler;
    }

    public DictationView()
    {
        super();
        _listener = createListener();
        _currentRecognizer = null;
        _listeningDialog = null;
        _destroyed = true;
    }

    @Override
    protected void onPrepareDialog(int id, final Dialog dialog) {
        switch(id)
        {
        case LISTENING_DIALOG:
            _listeningDialog.prepare(new Button.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    if (_currentRecognizer != null)
                    {
                        _currentRecognizer.stopRecording();
                    }
                }
            });
            break;
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id)
        {
        case LISTENING_DIALOG:
            return _listeningDialog;
        }
        return null;
    }



    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC); // So that the 'Media Volume' applies to this activity
        setContentView(R.layout.dictation);
        //Menu On Screen
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        //+++++++++++++++++++++++++++++++++++++++++
        TextView tstatus = (TextView)findViewById(R.id.textStatus);
        tstatus.setText(R.string.statusr);
        TextView ttime = (TextView)findViewById(R.id.textTime);
        ttime.setText("");
        TextView tlist = (TextView)findViewById(R.id.textList);
        tlist.setText("");

        context = this.getApplicationContext();

        mPrefs = context.getSharedPreferences("gissmp3",0);
        server = mPrefs.getString("giss_server", server);
        port = mPrefs.getInt("giss_port", port);
        mountpoint = mPrefs.getString("giss_mountpoint", mountpoint);
        password = mPrefs.getString("giss_password", password);
        abitrate = mPrefs.getInt("giss_abitrate", abitrate);
        achannels = mPrefs.getInt("giss_achannels", achannels);
        arate = mPrefs.getInt("giss_arate", arate);
        mp3quality = mPrefs.getInt("giss_mp3quality", mp3quality);
        name = mPrefs.getString("giss_name", name);
        description = mPrefs.getString("giss_description", description);
        genre = mPrefs.getString("giss_genre", genre);
        url = mPrefs.getString("giss_url", url);

        final Button butStart = (Button)findViewById(R.id.buttonCon);
        if(butStart!=null){
            butStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(x_streaming==0){
                        x_streaming=1;
                        initStreaming();
                        startStreaming();
                        butStart.setText(R.string.disconnect);
                    }
                    else if(x_streaming==1){
                        x_streaming=0;
                        stopStreaming();
                        shutdownStreaming();
                        butStart.setText(R.string.connect);
                    }
                }
            });

        }
        initialized = true;
        //+++++++++++++++++++++++++++++++++++++++++

        _speechKit = (SpeechKit) getLastNonConfigurationInstance();
        if (_speechKit == null) {
            _speechKit = SpeechKit.initialize(getApplication().getApplicationContext(), AppInfo.SpeechKitAppId, AppInfo.SpeechKitServer, AppInfo.SpeechKitPort, AppInfo.SpeechKitSsl, AppInfo.SpeechKitApplicationKey);
            _speechKit.connect();
            // TODO: Keep an eye out for audio prompts not working on the Droid 2 or other 2.2 devices.
            Prompt beep = _speechKit.defineAudioPrompt(R.raw.beep);
            _speechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100), null, null);
        }
        
        _destroyed = false;
        
        // Use the same handler for both buttons
        final Button dictationButton = (Button)findViewById(R.id.btn_startDictation);

        Button.OnClickListener startListener = new Button.OnClickListener()
        {
            @Override
            public void onClick(View v) {
            	_listeningDialog.setText("Initializing...");   
                showDialog(LISTENING_DIALOG);
            	Log.d("STATUS","on Listening Thread");
            	_listeningDialog.setStoppable(false);
                setResults(new Recognition.Result[0]);
                
                if (v == dictationButton)
                    _currentRecognizer = DictationView.getSpeechKit().createRecognizer(Recognizer.RecognizerType.Dictation, Recognizer.EndOfSpeechDetection.Long, "th_TH", _listener, _handler);
                else
                    _currentRecognizer = DictationView.getSpeechKit().createRecognizer(Recognizer.RecognizerType.Search, Recognizer.EndOfSpeechDetection.Short, "th_TH", _listener, _handler);
                _currentRecognizer.start();
            }
        };
        dictationButton.setOnClickListener(startListener);

        // Initialize the listening dialog
        createListeningDialog();
        
        SavedState savedState = (SavedState)getLastNonConfigurationInstance();
        if (savedState == null)
        {
            // Initialize the handler, for access to this application's message queue
            _handler = new Handler();
        } else
        {
            // There was a recognition in progress when the OS destroyed/
            // recreated this activity, so restore the existing recognition
            _currentRecognizer = savedState.Recognizer;
            _listeningDialog.setText(savedState.DialogText);
            _listeningDialog.setLevel(savedState.DialogLevel);
            _listeningDialog.setRecording(savedState.DialogRecording);
            _handler = savedState.Handler;
            
            if (savedState.DialogRecording)
            {
                // Simulate onRecordingBegin() to start animation
                _listener.onRecordingBegin(_currentRecognizer);
            }
            
            _currentRecognizer.setListener(_listener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _destroyed = true;
        if (_currentRecognizer !=  null)
        {
            _currentRecognizer.cancel();
            _currentRecognizer = null;
        }
    }

    private Recognizer.Listener createListener()
    {
        return new Recognizer.Listener()
        {            
            @Override
            public void onRecordingBegin(Recognizer recognizer) 
            {
                _listeningDialog.setText("Recording...");
            	_listeningDialog.setStoppable(true);
                _listeningDialog.setRecording(true);
                
                // Create a repeating task to update the audio level
                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        if (_listeningDialog != null && _listeningDialog.isRecording() && _currentRecognizer != null)
                        {
                            _listeningDialog.setLevel(Float.toString(_currentRecognizer.getAudioLevel()));
                            _handler.postDelayed(this, 500);
                        }
                    }
                };
                r.run();
            }

            @Override
            public void onRecordingDone(Recognizer recognizer) 
            {
                _listeningDialog.setText("Processing...");
                _listeningDialog.setLevel("");
                _listeningDialog.setRecording(false);
            	_listeningDialog.setStoppable(false);
            }

            @Override
            public void onError(Recognizer recognizer, SpeechError error) 
            {
            	if (recognizer != _currentRecognizer) return;
            	if (_listeningDialog.isShowing()) dismissDialog(LISTENING_DIALOG);
                _currentRecognizer = null;
                _listeningDialog.setRecording(false);

                // Display the error + suggestion in the edit box
                String detail = error.getErrorDetail();
                String suggestion = error.getSuggestion();
                
                if (suggestion == null) suggestion = "";
                setResult(detail + "\n" + suggestion);
                // for debugging purpose: printing out the speechkit session id
                android.util.Log.d("Nuance SampleVoiceApp", "Recognizer.Listener.onError: session id ["
                        + DictationView.getSpeechKit().getSessionId() + "]");
            }

            @Override
            public void onResults(Recognizer recognizer, Recognition results) {
                if (_listeningDialog.isShowing()) dismissDialog(LISTENING_DIALOG);
                _currentRecognizer = null;
                _listeningDialog.setRecording(false);
                int count = results.getResultCount();
                Recognition.Result [] rs = new Recognition.Result[count];
                for (int i = 0; i < count; i++)
                {
                    rs[i] = results.getResult(i);
                }
                setResults(rs);
                // for debugging purpose: printing out the speechkit session id
                android.util.Log.d("Nuance SampleVoiceApp", "Recognizer.Listener.onResults: session id ["
                        + DictationView.getSpeechKit().getSessionId() + "]");
            }
        };
    }
    
    private void setResult(String result)
    {
        EditText t = (EditText)findViewById(R.id.text_DictationResult);
        if (t != null)
            t.setText(result);
    }

    private void setResults(Recognition.Result[] results)
    {
        //_arrayAdapter.clear();
        if (results.length > 0)
        {
            setResult(results[0].getText());

        }  else
        {
            setResult("");
        }
    }
    
    private void createListeningDialog()
    {
        _listeningDialog = new ListeningDialog(this);
        _listeningDialog.setOnDismissListener(new OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (_currentRecognizer != null) // Cancel the current recognizer
                {
                    _currentRecognizer.cancel();
                    _currentRecognizer = null;
                }
                
                if (!_destroyed)
                {
                    // Remove the dialog so that it will be recreated next time.
                    // This is necessary to avoid a bug in Android >= 1.6 where the 
                    // animation stops working.
                    DictationView.this.removeDialog(LISTENING_DIALOG);
                    createListeningDialog();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        else if (id==R.id.action_about){

            return true;
        }
        else if (id==R.id.action_exit){

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
