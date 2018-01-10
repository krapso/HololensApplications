package fetr.adam.example.hololenscolorapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    String color;
    RelativeLayout relativeLayout;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeAdvertiser mBLEAdvertiser;
    static final int BEACON_ID = 1775;
    public static final int REQUEST_ENABLE_BT = 1;
    static final int PERMISSION_RESULT_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        color = "FFFFFF";
        relativeLayout = (RelativeLayout) findViewById(R.id.background);

        if (savedInstanceState == null) {
            // Use this check to determine whether BLE is supported on the device. Then you can
            // selectively disable BLE-related features.Only needed if required=false in manifest e.g.:
            // <uses-feature android:name="android.hardware.bluetooth_le" android:required="false"/>
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }

            // Is Bluetooth supported on this device?
            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled, so proceed.
                        mBLEAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                        mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
                    } else {
                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(this, R.string.bt_ads_not_supported, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int permissionCheck = ContextCompat.checkSelfPermission((Context) this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startAdvertising(BEACON_ID);
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_RESULT_CODE);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        stopAdvertising();
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled, so proceed.
                        mBLEAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    } else {
                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(this, R.string.bt_ads_not_supported_leaving, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RESULT_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    restartAdvertising(BEACON_ID);
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Access Denied. Quitting!", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startAdvertising(int manufactureId) {
        if (mBLEAdvertiser == null) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .addManufacturerData(manufactureId, color.getBytes())
                .build();
        mBLEAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBLEAdvertiser == null) return;
        mBLEAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private void restartAdvertising(int manufactureId) {
        stopAdvertising();
        startAdvertising(manufactureId);
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            byte[] receivedString = result.getScanRecord().getManufacturerSpecificData(BEACON_ID);
            if(receivedString!=null){
                String data = new String(receivedString);
                relativeLayout.setBackgroundColor(Color.parseColor(data));
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            String msg = "Service Running";
            mHandler.sendMessage(Message.obtain(null, 0, msg));
        }

        @Override
        public void onStartFailure(int errorCode) {
            if (errorCode != ADVERTISE_FAILED_ALREADY_STARTED) {
                String msg = "Service failed to start: " + errorCode;
                mHandler.sendMessage(Message.obtain(null, 0, msg));
            } else {
                //restartAdvertising();
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Toast.makeText(MainActivity.this, new SimpleDateFormat("HH.mm.ss").format(new Date()) +
            //        ": " + (CharSequence) msg.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    public void ChangeColorToRed(View view){
        color = "#FF0000";
        restartAdvertising(BEACON_ID);
    }

    public void ChangeColorToGreen(View view){
        color = "#00FF00";
        restartAdvertising(BEACON_ID);
    }

    public void ChangeColorToBlue(View view){
        color = "#0000FF";
        restartAdvertising(BEACON_ID);
    }
}
