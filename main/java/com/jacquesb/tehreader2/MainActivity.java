package com.jacquesb.tehreader2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    MyView mijnview = null;
    static String temperature = new String("...");
    static String status = new String("initialiseren...");

    /**
     * smartWatch Yvonne = P22B1
     * smartWatch Jacques = M31C1
     */
    //String myDevice = "P22B1";
    //String myDevice = "M31C1";
    boolean diag = true;

    int count;
    //Intent gattServiceIntent = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mijnview = new MyView(this);
        setContentView(mijnview);

        /* For new android versions this is needed */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    0);
            status = "Permissie ACCESS_LOCATION verkregen";
        } else {
            status = "Permissie ACCESS_LOCATION niet verkregen, scanner werkt niet...";
        }
        if (diag) System.out.println(status);

        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        if (advertiser == null) {

            Toast.makeText(this, "Please turn on bluetooth.", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothManager bluetoothManager = null;
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || diag) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                status = "Dit apparaat heeft geen bluetooth";
            } else {
                status = "Dit apparaat heeft bluetooth";
                if (diag) System.out.println(status);
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                }
                if (bluetoothAdapter.isEnabled()) {
                    // start scanning
                    status = "Zoeken naar bluetooth-devices";
                    if (diag) System.out.println(status);
                    temperature = status;
                    mijnview.invalidate();
                    discover();
                }
            }
        //} else {
        //    status = new String("Kan bluetooth niet activeren (oude android versie?)");
        //}


        // Brute force manier om elke halve seconde het canvas te updaten.
        MyTimerTask myTask = new MyTimerTask();
        Timer myTimer = new Timer();
        //	        public void schedule (TimerTask task, long delay, long period)
        //	        Schedule a task for repeated fixed-delay execution after a specific delay.
        //
        //	        Parameters
        //	        task  the task to schedule.
        //	        delay  amount of time in milliseconds before first execution.
        //	        period  amount of time in milliseconds between subsequent executions.

        myTimer.schedule(myTask, 500, 500);

    }
    ParcelUuid pUuid16;

    void discover() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {

                Toast.makeText(this, "Please turn on bluetooth.", Toast.LENGTH_SHORT).show();
                return;
            }
            /*
            startLeScan with 128 bit UUIDs doesn't work on native Android BLE implementation
            See:
            https://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation/19060589#19060589
             */
            pUuid16 = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))); //ble_uuid? value/string.xml??.

            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(pUuid16).build();

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);

            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

            //bluetoothLeScanner.startScan(filters, settings, mScanCallback);
            bluetoothLeScanner.startScan(mScanCallback);

    }

    ArrayList<String> leDevices = new ArrayList<>();
    ArrayList<BluetoothDevice> bleDevicesList = new ArrayList<>();
    Hashtable<String, BluetoothDevice> bleDevices = new Hashtable<>();

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null)
                return;
            if (result.getScanRecord().getServiceUuids() == null)
                return;
            if (result.getScanRecord().getServiceUuids().get(0) == null)
                return;

            boolean found = false;
            pUuid16 = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))); //ble_uuid? value/string.xml??.
            ParcelUuid pUuid128 = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid1))); //ble_uuid? value/string.xml??.
            ParcelUuid uuid = result.getScanRecord().getServiceUuids().get(0);
            if (uuid.equals(pUuid16)) found = true;
            if (uuid.equals(pUuid128)) found = true;

            if (found) {
                byte[] data = result.getScanRecord().getServiceData(uuid);
                if (data == null)
                    return;
                Log.i("BLE", "result UUID : " + uuid.toString());
                Log.i("BLE", "result data : " + data);
                String res = new String(result.getScanRecord().getServiceData(uuid), Charset.forName("UTF-8"));
                Log.i("BLE", "onScanResult" + res);
                //Toast.makeText(MainActivity.this, "Scan Result", Toast.LENGTH_SHORT).show();
                temperature = res;
                status = " TEH broadcast";
                mijnview.invalidate();
                count = -10;
            } else {
                if (! bleDevicesList.contains(result.getDevice())) {
                    bleDevicesList.add(result.getDevice());
                    leDevices.add(result.getDevice().getName());
                    if (result.getDevice().getName() != null) {
                        bleDevices.put(result.getDevice().getName(), result.getDevice());
                    }
                }

            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Toast.makeText(MainActivity.this, "Why here?", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE", "Discovery onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    class MyTimerTask extends TimerTask {
        public void run() {
            // mijnview.invalidate Fatal error: Android “Only the original thread that created a view hierarchy can touch its views.”

            // http://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi#5162096
            // You have to move the portion of the background task that updates the ui onto the main thread. There is a simple piece of code for this:
            // Documentation for Activity.runOnUiThread
            // Just nest this inside the method that is running in the background, then copy paste the code that implements any updates in the middle of the block.
            // Include only the smallest amount of code possible, otherwise you start to defeat the purpose of the background thread.

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    count++;
                    if (count > 0) {
                        temperature = " ---- ";
                        status = "scanning for TEH...";
                        if (AppData.TimerData.devs == leDevices.size()) {
                            count++;
                        } else {
                            AppData.TimerData.devs = leDevices.size();
                            count = 0;
                        }
                            if (leDevices.size() == 0) {
                                status = "Geen BLE gevonden, scanning for TEH .... (" + count + ")";
                            } else if (leDevices.size() == 1) {
                                String last = leDevices.get(leDevices.size() - 1);
                                if (last == null) {
                                    last = "has no name";
                                }
                                status = leDevices.size() + " BLE  found: " + leDevices.get(leDevices.size() - 1) + ", scanning for TEH ... (" + count + ")";
                            } else {
                                String last = leDevices.get(leDevices.size() - 1);
                                if (last == null) {
                                    last = "has no name";
                                }
                                status = leDevices.size() + " BLEs found, last: " + leDevices.get(leDevices.size() - 1) + ", scanning for TEH ... (" + count + ")";
                            }
                    }
                    //refresh screen
                    mijnview.invalidate();
                    if (count > 100) {
                        // reset
                        count = 0;
                        leDevices = new ArrayList<>();
                        bleDevicesList = new ArrayList<>();
                        bleDevices = new Hashtable<>();
                        AppData.TimerData.devs = 0;
                    }
                }
            });
        }
    }


    private static class MyView extends View {
        public MyView(Context context) {
            super(context);
            //this.run();
            this.invalidate();

        }
        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nrg_logo);
            canvas.drawBitmap(myBitmap, 20, 20, null);
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            double relation = Math.sqrt(getWidth() * getHeight());
            relation = relation / 250;
            double myFontSize = 18;
            p.setTextSize((float) (myFontSize * relation));

            canvas.drawText(new String(temperature+" degrees Celsius"), (float)50, (float)600, p);
            myFontSize = 8;
            p.setTextSize((float) (myFontSize * relation));
            canvas.drawText(status, (float)50, (float)700, p);
        }
    }

}