package com.teocci.p2pchatroom;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.teocci.p2pchatroom.interfaces.NsdHelperListener;

public class ChatActivity extends Activity
{
    protected static final int NUM_OF_RETRIES = 3;
    private final String TAG = "ChatActivity";
    Handler mainHandler;
    //    ChatServer.UICallbacks mCallbacks;
    NsdHelper nsdHelper;
    TextView debugView;
//    ListView listView;
//    ChatterRoomAdapter mAdapter;

    private ChatService boundService;
    private static boolean isBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {

        public void onServiceConnected(ComponentName className, IBinder service)
        {
            boundService = ((ChatService.ChatBinder) service).getService();
            Log.i(TAG, "Service is bounded!!!");
            boundService.initAll(debugView);
            debugView.setText(boundService.copyOfDebugCache());
        }

        public void onServiceDisconnected(ComponentName className)
        {
            boundService = null;
        }
    };

    private void doBindService()
    {
        if (!isBound) {
            bindService(new Intent(this, ChatService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            isBound = true;
        }
    }

    private void doUnbindService()
    {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void startChatRoomService(String type)
    {
        Intent i = new Intent(this, ChatService.class);
        Bundle extra = new Bundle();
        extra.putString(ChatService.REQ_TYPE, type);
        i.putExtras(extra);
        startService(i);
    }

    public static int PORT = 5134;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mainHandler = new Handler();
        // Setting up views
        Switch registerSth = (Switch) findViewById(R.id.registerSwitch);
        Switch discoverSth = (Switch) findViewById(R.id.discoverySwitch);
        final ScrollView sv = (ScrollView) findViewById(R.id.scrollBar);
//        registerSth.setChecked(true);
        debugView = (TextView) findViewById(R.id.debugMessage);
        // Black background white text int is ARGB format
        debugView.setBackgroundColor(0xff101010);
        debugView.setTextColor(0xfff9f9f9);
        final EditText et = (EditText) findViewById(R.id.editText);
        Button sendBtn = (Button) findViewById(R.id.sendMessage);

        sendBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Null check is done inside Service Object
                boundService.postMessageToServer(et.getText().toString());
//                boundService.postMessageToClient(et.getText().toString());
                if (boundService.hasServerOrClient()) {
                    et.setText("");
                }
            }
        });
        registerSth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked) { // User change it to checked status
                    if (!nsdHelper.isRegistered()) // Only register it when not already registered
                        nsdHelper.registerService(PORT);
                } else {
                    if (nsdHelper.isRegistered())
                        nsdHelper.unRegisterService();
                }
            }
        });

        discoverSth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked) {
                    if (!nsdHelper.isDiscovering())
                        nsdHelper.discoverServices();
                } else {
                    if (nsdHelper.isDiscovering())
                        nsdHelper.stopDiscoverServices();
                }

            }
        });

        // Setting up NsdHelper
        NsdHelperListener mListener = new NsdHelperListener()
        {
            private String TAG = "ChatActivity";

            @Override
            public void notifyRegistrationComplete()
            {
                // Only run server when necessary
                if (!boundService.hasServerOrClient())
                    startChatRoomService(ChatService.SERVER_TYPE);
            }

            @Override
            public void notifyDiscoveredOneItem(final NsdServiceInfo NsdItem)
            {
                Log.i(TAG, " trying to connect to address" + NsdItem.getHost().toString() + " : " + NsdItem.getPort());
                boundService.initAndPostForClient(NsdItem, NUM_OF_RETRIES);
            }

            @Override
            public void outputDebugMessage(final String msg)
            {
                if (boundService == null) return;
                boundService.outputDebugMessageToUI(msg);
            }
        };

        nsdHelper = new NsdHelper(this, mainHandler, mListener);
        // Start server just in case
        startChatRoomService(ChatService.SERVER_TYPE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    protected void onDestroy()
    {
        // Clean up our Nsd Service upon exit
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (nsdHelper.isRegistered()) {
//            nsdHelper.stopDiscoverServices();
            nsdHelper.registerService(PORT);
        }
        doBindService();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        // Stop discovering
//        nsdHelper.stopDiscoverServices();
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        if (nsdHelper.isDiscovering()) {
            nsdHelper.stopDiscoverServices();
        }
        if (nsdHelper.isRegistered()) {
            nsdHelper.unRegisterService();
        }
        doUnbindService();
        super.onStop();
    }
}
