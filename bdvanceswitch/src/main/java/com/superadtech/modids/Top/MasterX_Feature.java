package com.superadtech.modids.Top;

import static com.superadtech.modids.MyAdZOne.VPN_NI_CHAWI;
import static com.superadtech.modids.MyAdZOne.VPN_OFF_When_App_Close;
import static com.superadtech.modids.MyAdZOne.VPN_OFF_When_App_Close_Feature;
import static com.superadtech.modids.Top.Constants.Configurationfiles;
import static com.superadtech.modids.Top.Constants.Splash_state;
import static com.superadtech.modids.Top.Constants.serverName;
import static com.superadtech.modids.Top.Constants.servers;
import static com.superadtech.modids.Top.Constants.vPassword;
import static com.superadtech.modids.Top.Constants.vUserName;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import top.oneconnectapi.app.OpenVpnApi;
import top.oneconnectapi.app.api.OneConnect;
import top.oneconnectapi.app.core.OpenVPNThread;

public class MasterX_Feature extends Activity {
    public Countries selectedCountry = null;
    private OpenVPNThread vpnThread = new OpenVPNThread();
    Boolean bg_changed = false;
    public static String STATUSX = "DISCONNECTED";

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = getIntent();
                    if (intent != null) {
                        Splash_state = intent.getBooleanExtra(Constants.From_Splash, true);
                    }

                    OneConnect oneConnect = new OneConnect();
                    oneConnect.initialize(MasterX_Feature.this, VPN_NI_CHAWI);
                    try {
                        Constants.FREE_SERVERS = oneConnect.fetch(true);
                        Constants.PREMIUM_SERVERS = oneConnect.fetch(false);

                        JSONArray jsonArray_pro = new JSONArray(Constants.PREMIUM_SERVERS);
                        for (int i = 0; i < jsonArray_pro.length(); i++) {
                            JSONObject object = (JSONObject) jsonArray_pro.get(i);
                            servers.add(new Countries(object.getString(serverName),
                                    object.getString(Configurationfiles),
                                    object.getString(vUserName),
                                    object.getString(vPassword)
                            ));
                        }

                        JSONArray jsonArray_free = new JSONArray(Constants.FREE_SERVERS);
                        for (int i = 0; i < jsonArray_free.length(); i++) {
                            JSONObject object = (JSONObject) jsonArray_free.get(i);
                            servers.add(new Countries(object.getString(serverName),
                                    object.getString(Configurationfiles),
                                    object.getString(vUserName),
                                    object.getString(vPassword)
                            ));
                        }
                        initials();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void initials() {
        LocalBroadcastManager.getInstance(MasterX_Feature.this).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));
        Random random = new Random();
        int randomNumber = random.nextInt(servers.size());
        selectedCountry = servers.get(randomNumber);
        updateUI("LOAD");
        if (selectedCountry != null) {
            prepareVpn();
        }
    }

    public void prepareVpn() {
        if (selectedCountry != null) {
            Intent intent = VpnService.prepare(this);
            Log.v("CHECKSTATE", "start");
            if (intent != null) {
                startActivityForResult(intent, 1);
            } else
                startVpn();
        }
    }

    protected void startVpn() {
        try {
            OpenVpnApi.startVpn(this, selectedCountry.getOvpn(), selectedCountry.getCountry(),
                    selectedCountry.getOvpnUserName(), selectedCountry.getOvpnUserPassword());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void updateUI(String status) {
        switch (status) {
            case "DISCONNECTED":
                STATUSX = "DISCONNECTED";
                connectionOff();
                break;

            case "CONNECTED":
                STATUSX = "CONNECTED";
                connectionOn();
                break;

            case "WAIT":
                STATUSX = "WAITING";
                break;

            case "AUTH":
                STATUSX = "AUTHENTICATION";
                break;

            case "RECONNECTING":
                STATUSX = "RECONNECTING";
                break;

            case "NONETWORK":
                STATUSX = "DISCONNECTED";
                connectionOff();
                break;

            case "LOAD":
                STATUSX = "LOAD";
                break;
        }
    }

    protected void connectionOff() {
        if (bg_changed != null && bg_changed) {
            bg_changed = false;
        }
    }

    protected void connectionOn() {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (bg_changed != null && !bg_changed) {
            bg_changed = true;
        }
        finish();
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                updateUI(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (VPN_OFF_When_App_Close_Feature.equalsIgnoreCase("true")) {
            disconnectFromVnp();
        }
    }

    protected void disconnectFromVnp() {
        try {
            vpnThread.stop();
            updateUI("DISCONNECTED");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            startVpn();
        } else {
            Toast.makeText(this, "Please Allow Permission", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MasterX_Feature.class)
                    .putExtra(Constants.From_Splash, Splash_state));
            finish();
        }
    }
}
