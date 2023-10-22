package com.superadtech.modids.Top;

import static com.superadtech.modids.MyAdZOne.AD_MOB_OpenAd_Fails_INTER_SHOW;
import static com.superadtech.modids.MyAdZOne.AD_MOB_SPLASH_INTER_FORCE;
import static com.superadtech.modids.MyAdZOne.All_Ads_Show;
import static com.superadtech.modids.MyAdZOne.VPN_NI_CHAWI;
import static com.superadtech.modids.MyAdZOne.VPN_OFF_When_App_Close;
import static com.superadtech.modids.MyAdZOne.VPN_SHOW;
import static com.superadtech.modids.MyAdZOne.app_BannerPeriority;
import static com.superadtech.modids.MyAdZOne.app_NativeAdCodeType;
import static com.superadtech.modids.MyAdZOne.app_UpdatePackageName;
import static com.superadtech.modids.MyAdZOne.app_onesingle_appid;
import static com.superadtech.modids.MyAdZOne.app_updateAppDialogStatus;
import static com.superadtech.modids.MyAdZOne.app_versionCode;
import static com.superadtech.modids.MyAdZOne.install_ref_Activate;
import static com.superadtech.modids.Top.Constants.Configurationfiles;
import static com.superadtech.modids.Top.Constants.Next_Activity_Name;
import static com.superadtech.modids.Top.Constants.Splash_state;
import static com.superadtech.modids.Top.Constants.serverName;
import static com.superadtech.modids.Top.Constants.servers;
import static com.superadtech.modids.Top.Constants.vPassword;
import static com.superadtech.modids.Top.Constants.vUserName;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.onesignal.OneSignal;
import com.superadtech.modids.AESSUtils;
import com.superadtech.modids.AppOpenManager;
import com.superadtech.modids.Applicationclass;
import com.superadtech.modids.MyAdZOne;
import com.superadtech.modids.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import top.oneconnectapi.app.OpenVpnApi;
import top.oneconnectapi.app.api.OneConnect;
import top.oneconnectapi.app.core.OpenVPNThread;

public class MasterX extends Activity {
    public Activity activity = this;
    public Countries selectedCountry = null;
    private OpenVPNThread vpnThread = new OpenVPNThread();
    public static Boolean bg_changed = false;
    public static String STATUS = "DISCONNECTED";

    static MySpalshCallback mySpalshCallback;

    public interface MySpalshCallback {
        void onSuccessMethod();
    }

    public static String mode = "";
    public static boolean isShowOpen = false;
    public static AppOpenManager appOpenManager;
    public static Intent intentx;
    public static boolean need_internet = false;
    public static boolean is_retry;
    public static Handler refreshHandler;
    public static Runnable runnable;
    public static int vercode;

    @Override
    protected void onStop() {
        super.onStop();
    }

    String model = "";
    String intentdata = "";
    int cversion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            model = receivedIntent.getStringExtra("model");
            intentdata = receivedIntent.getStringExtra("intent");
            cversion = receivedIntent.getIntExtra("cversion", 0);
            Splash_state = receivedIntent.getBooleanExtra(Constants.From_Splash, true);
        }

        if (Splash_state) {
            sendRequest(model, intentdata, cversion);
        }
    }

    public void Start_Tolpology() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = getIntent();
                    if (intent != null) {
                        Next_Activity_Name = intent.getStringExtra(Constants.Next_Activity);
                        Splash_state = intent.getBooleanExtra(Constants.From_Splash, true);
                    }

                    OneConnect oneConnect = new OneConnect();
                    oneConnect.initialize(MasterX.this, VPN_NI_CHAWI);
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
        LocalBroadcastManager.getInstance(MasterX.this).registerReceiver(broadcastReceiver,
                new IntentFilter("connectionState"));
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

    public void startVpn() {
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
                STATUS = "DISCONNECTED";
                connectionOff();
                break;

            case "CONNECTED":
                STATUS = "CONNECTED";
                connectionOn();
                break;

            case "WAIT":
                STATUS = "WAITING";
                break;

            case "AUTH":
                STATUS = "AUTHENTICATION";
                break;

            case "RECONNECTING":
                STATUS = "RECONNECTING";
                break;

            case "NONETWORK":
                STATUS = "DISCONNECTED";
                connectionOff();
                break;

            case "LOAD":
                STATUS = "LOAD";
                break;
        }
    }

    protected void connectionOff() {
        if (bg_changed != null && bg_changed) {
            bg_changed = false;
        }
    }

    protected void connectionOn() {
        if (Splash_state) {
            Allloadeddarts();
        } else {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        }

        if (bg_changed != null && !bg_changed) {
            bg_changed = true;
        }
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
        if (VPN_OFF_When_App_Close.equalsIgnoreCase("true")) {
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
            activity.startActivity(new Intent(activity, MasterX.class).putExtra(Constants.From_Splash, Splash_state));
            finish();
        }
    }
    ////////////////////////////////////////////End/////////////////////////////////////////////////

    public void sendRequest(String model, String intent1, final int cversion) {
        need_internet = !model.isEmpty();

        vercode = cversion;
        final Dialog dialog = new Dialog(activity);
        dialog.setCancelable(false);
        View view = activity.getLayoutInflater().inflate(R.layout.retry_layout, null);
        dialog.setContentView(view);
        final TextView retry_buttton = view.findViewById(R.id.retry_buttton);

        if (!isNetworkAvailable(activity) && need_internet) {
            is_retry = false;
            dialog.show();
        }

        dialog.dismiss();
        refreshHandler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable(activity)) {
                    is_retry = true;
                    retry_buttton.setText("Retry Again");
                } else if (need_internet) {
                    dialog.show();
                    is_retry = false;
                    retry_buttton.setText("Please Connect To Internet");
                }
                refreshHandler.postDelayed(this, 1000);
            }
        };

        try {
            mode = AESSUtils.Logd(model);
        } catch (Exception e) {
        }
        refreshHandler.postDelayed(runnable, 1000);
        retry_buttton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (is_retry) {
                    if (need_internet) {
                        activity.startActivity(new Intent(activity, activity.getClass()));
                        activity.finish();
                    } else {
                        SuccessloadedRedirect();
                    }
                } else {
                    activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                }
            }
        });

        RequestQueue queue = Volley.newRequestQueue(activity);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, mode, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getInt("success") == 1) {
                        MyAdZOne.getInstance(activity).configDatas(jsonObject);
                        if (All_Ads_Show.equalsIgnoreCase("true")) {
                            if (install_ref_Activate.equalsIgnoreCase("true")) {
                                Instant_Check.getInstance(activity).Check_Internet_Inst(new Instant_Check.MyInstallCallback() {
                                    @Override
                                    public void Succeessfull() {
                                        Log.d("installrefcheck", "Succeessfull: yes this in success");
                                        Start_Tolpology();
                                    }

                                    @Override
                                    public void UnSucceessfull() {
                                        Allloadeddarts();
                                        Log.d("installrefcheck", "unSucceessfull: yes this in notsuccess");
                                    }
                                });
                            } else {
                                if (VPN_SHOW.equalsIgnoreCase("true")) {
                                    Start_Tolpology();
                                } else {
                                    Allloadeddarts();
                                }
                            }
                        } else {
                            NoInzilseAllloadeddarts();
                        }
                    } else {
                        Toast.makeText(activity, "Not Found Data!!!", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    if (need_internet) {
                        dialog.dismiss();
                        refreshHandler = new Handler();
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (isNetworkAvailable(activity)) {
                                    is_retry = true;
                                    retry_buttton.setText("Retry Again");
                                } else {
                                    dialog.show();
                                    is_retry = false;
                                    retry_buttton.setText("Please Connect To Internet");
                                }
                                refreshHandler.postDelayed(this, 1000);
                            }
                        };
                    } else {
                        SuccessloadedRedirect();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (need_internet) {
                    dialog.dismiss();
                    refreshHandler = new Handler();
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isNetworkAvailable(activity)) {
                                is_retry = true;
                                retry_buttton.setText("Retry Again");
                            } else {
                                dialog.show();
                                is_retry = false;
                                retry_buttton.setText("Please Connect To Internet");
                            }
                            refreshHandler.postDelayed(this, 1000);
                        }
                    };
                } else {
                    SuccessloadedRedirect();
                }
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(8000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void getInlize() {
        Log.e("TAG", "getInlize: ");
        if (All_Ads_Show.equalsIgnoreCase("true")) {
            AudienceNetworkAds.initialize(activity);

            MobileAds.initialize(activity, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
        }
    }

    public void NoInzilseAllloadeddarts() {
        if (app_onesingle_appid != null) {
            OneSignal.initWithContext(activity);
            OneSignal.setAppId(app_onesingle_appid);
        }

        if (app_updateAppDialogStatus.equalsIgnoreCase("true") && checkUpdate(vercode)) {
            showUpdateDialog(activity, app_UpdatePackageName);
            return;
        }
        SuccessloadedRedirect();
    }

    public void Allloadeddarts() {

        getInlize();

        MyAdZOne.getInstance(activity).loadInterstitialAd(activity);

        if (app_BannerPeriority.equalsIgnoreCase("native")) {
            MyAdZOne.getInstance(activity).Load_NativeBannerAds();
        } else {
            MyAdZOne.getInstance(activity).Load_BannerAds();
        }

        if (app_NativeAdCodeType.equalsIgnoreCase("new")) {
            MyAdZOne.getInstance(activity).Load_NativeNewAds();
        }

        if (app_onesingle_appid != null) {
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
            OneSignal.initWithContext(activity);
            OneSignal.setAppId(app_onesingle_appid);
        }

        if (app_updateAppDialogStatus.equalsIgnoreCase("true") && checkUpdate(vercode)) {
            showUpdateDialog(activity, app_UpdatePackageName);
            return;
        }

        isShowOpen = false;
        AppOpenManager.OnAppOpenClose onAppOpenClose = new AppOpenManager.OnAppOpenClose() {
            @Override
            public void OnAppOpenFailToLoad() {
                if (isShowOpen) {
                    isShowOpen = false;
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MyAdZOne.appOpenFailLoadeds(new MyAdZOne.OnAdListner() {
                                @Override
                                public void OnClose() {
                                    if (AD_MOB_SPLASH_INTER_FORCE.equalsIgnoreCase("true")) {
                                        MyAdZOne.getInstance(activity).Show_Next_InterstitialAd(activity, new MyAdZOne.MyCallback() {
                                            public void callbackCall() {
                                                AD_MOB_SPLASH_INTER_FORCE = "false";
                                                SuccessloadedRedirect();
                                            }
                                        }, 0);
                                    } else {
                                        if (AD_MOB_OpenAd_Fails_INTER_SHOW.equalsIgnoreCase("true")) {
                                            MyAdZOne.getInstance(activity).Show_Next_InterstitialAd(activity, new MyAdZOne.MyCallback() {
                                                public void callbackCall() {
                                                    AD_MOB_OpenAd_Fails_INTER_SHOW = "false";
                                                    SuccessloadedRedirect();
                                                }
                                            }, 0);
                                        } else {
                                            SuccessloadedRedirect();
                                        }
                                    }
                                }
                            });
                        }
                    }, 3500);
                }
            }

            @Override
            public void OnAppOpenClose() {
                if (isShowOpen) {
                    isShowOpen = false;
                    SuccessloadedRedirect();
                }
            }
        };
        isShowOpen = true;
        appOpenManager = new AppOpenManager(Applicationclass.getInstant(), onAppOpenClose);
    }

    public void SuccessloadedRedirect() {
        mySpalshCallback = Constants.mySpalshCallback;
        if (mySpalshCallback != null) {
            mySpalshCallback.onSuccessMethod();
            mySpalshCallback = null;
        }
    }

    public static boolean isNetworkAvailable(Activity contnex) {
        ConnectivityManager manager =
                (ConnectivityManager) contnex.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission")
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }

    public static boolean checkUpdate(int mobile_version) {
        if (app_updateAppDialogStatus.equalsIgnoreCase("true")) {
            String str[] = app_versionCode.split(",");

            try {
                for (String s : str) {
                    int server_update_value = Integer.parseInt(s.trim());
                    if (server_update_value > mobile_version) {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void showUpdateDialog(Activity context, String updatePackageName) {
        final Dialog dialog = new Dialog(context);
        dialog.setCancelable(false);
        View view = context.getLayoutInflater().inflate(R.layout.redirect_newapp_dialog, null);
        dialog.setContentView(view);
        TextView update = view.findViewById(R.id.update);

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Uri marketUri = Uri.parse("https://play.google.com/store/apps/details?id=" + updatePackageName);
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                    context.startActivity(marketIntent);
                } catch (ActivityNotFoundException ignored1) {
                }
            }
        });
        dialog.create();
        dialog.show();
    }

}
