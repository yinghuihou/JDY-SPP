package com.example.jdy_spp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;


public class SettingActivity extends Activity {

    public static final String SEND_FILE_ACTION = "send_file_action";

    private Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        send = findViewById(R.id.bt_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast();
            }
        });
    }

    private void sendBroadcast() {
        Intent intent = new Intent();
        intent.setAction(SEND_FILE_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
