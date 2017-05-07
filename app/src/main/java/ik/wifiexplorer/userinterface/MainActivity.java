package ik.wifiexplorer.userinterface;

import ik.wifiexplorer.R;
import ik.wifiexplorer.webserver.MainMenu;
import ik.wifiexplorer.webserver.MyWiFiServer;
import ik.wifiexplorer.webserver.Utility;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private MyWiFiServer server;
    private boolean isServiceStarted = false;
    private myDeviceStateListener mDeviceStateListener;
    private myWiFiStateListener mWiFiStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeWebserver();
        initializeDeviceStateListener();
        setButtonHandlers();

    }

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.bt_start_stop)).setOnClickListener(btnClick);
    }

    private void setButtonText(boolean isServiceRunning){
        ((Button)findViewById(R.id.bt_start_stop)).setText(
                getString(isServiceRunning ? R.string.caption_stop : R.string.caption_start));
    }


    private View.OnClickListener btnClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.bt_start_stop:{

                    if( isServiceStarted ){
                        server.stop();
                        isServiceStarted = false;

                        showIpAddress( false );
                        setButtonText(false);
                    }
                    else{
                        try {
                            server.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        isServiceStarted = true;

                        showIpAddress( true );
                        setButtonText(true);
                    }

                    break;
                }
            }


        }
    };




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

    }


    private boolean deviceIsConnectedViaWifi() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();

    }

    // Zeige IP-Adresse, falls true
    private void showIpAddress(boolean show) {

        TextView ipAdress = (TextView) findViewById(R.id.tv_info);

        if (show) {
            ipAdress.setText("Enter the following IP address into the address line of your web browser on the PC\n");
            ( (TextView) findViewById(R.id.tv_info1) ).setText( "http:\\\\" + Utility.getLocalIpAddress(this) + ":8080\\" );
        }
        else {
            ipAdress.setText("access stopped." );
            ( (TextView) findViewById(R.id.tv_info1) ).setText( "" );
        }

    }


    // Initialize webserver.

    private void initializeWebserver(){

        if ( deviceIsConnectedViaWifi(  ) ){
            try {
                server = new MyWiFiServer(this );
                server.stop();
                ( (TextView) findViewById(R.id.tv_info) ).setText("Connected to WiFi.");
                setButtonText(false);
                ( (Button) findViewById(R.id.bt_start_stop)).setEnabled( true );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            ( (TextView) findViewById(R.id.tv_info) ).setText( "No WiFI connection!" );
            ( (Button) findViewById(R.id.bt_start_stop) ).setEnabled( false );
        }

    }

    private void initializeDeviceStateListener(){

        mDeviceStateListener = new myDeviceStateListener();
        TelephonyManager tel = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        tel.listen(mDeviceStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        mWiFiStateListener = new myWiFiStateListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(mWiFiStateListener, intentFilter);


    }



    private class myDeviceStateListener extends PhoneStateListener {

        /* Get the Signal strength from the provider, each tiome there is an update */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            if (null != signalStrength && signalStrength.getGsmSignalStrength() != 99) {
                int gsmSignalStrength = calculatedBm( signalStrength.getGsmSignalStrength() );
                MainMenu.setGsmSignalStrengthValue( gsmSignalStrength );
            }
        }

        private int calculatedBm( int asu ){
            return -113 + 2*asu;
        }
    }


    public class myWiFiStateListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){


                    SystemClock.sleep(3000);
                    initializeWebserver();

                } else {

                    server.stop();
                    isServiceStarted = false;

                    ( (TextView) findViewById(R.id.tv_info)).setText("WiFi connection lost!");
                    ( (Button) findViewById(R.id.bt_start_stop) ).setEnabled(false);

                    ( (TextView) findViewById(R.id.tv_info1) ).setText("");

                    setButtonText(false);
                    //setButtonHandlers();
                }
            }

        }
    }

}