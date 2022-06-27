package com.tag.bledemo2;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String version = "alpha.12";

    Button btn_advertise16, btn_advertise128;
    Button btn_discover16, btn_discover128;
    TextView textView, androidVersion, appSDK;
    private BluetoothLeScanner mBluetoothLeScanner;
    boolean scanning = false;
    boolean includeDeviceName=true;
    ParcelUuid pUuid, pUuid16, pUuid128;
    int temp = 18;
    String dataString;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        androidVersion = (TextView) findViewById(R.id.android_version);
        appSDK = (TextView) findViewById(R.id.app_sdk);

        Field[] fields = Build.VERSION_CODES.class.getFields();
        String codeName = "UNKNOWN";
        int android_device_api_level = -1;
        for (Field field : fields) {
            try {
                if (field.getInt(Build.VERSION_CODES.class) == Build.VERSION.SDK_INT) {
                    codeName = field.getName();
                    android_device_api_level = Build.VERSION.SDK_INT;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        androidVersion.setText("Android: "+codeName+"; API level: "+android_device_api_level);


        //compileSdkVersion;
        appSDK.setText("App version "+version+" built with: Android 4.2 (Jelly Bean); API level: 17");

        btn_advertise16 = (Button) findViewById(R.id.btn_advertise_16bit);
        btn_advertise128 = (Button) findViewById(R.id.btn_advertise_128bit);
        btn_discover16 = (Button) findViewById(R.id.btn_discover_16bits);
        btn_discover128 = (Button) findViewById(R.id.btn_discover_128bits);
        textView = (TextView) findViewById(R.id.text_result);
        pUuid = null;
        pUuid16 = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid16).toLowerCase())); //ble_uuid? value/string.xml??.
        pUuid128 = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid128).toLowerCase())); //ble_uuid? value/string.xml??.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    0);
            textView.setText("Permissie ACCESS_LOCATION verkregen");
        }

        btn_advertise16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advertise(16);
            }
        });

        btn_advertise128.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advertise(128);
            }
        });

        btn_discover16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discover(16);
            }
        });

        btn_discover128.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discover(128);
            }
        });

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    }

    public void advertise(int bits) {
        if (scanning) {
            mBluetoothLeScanner.stopScan(filteredScanCallback);
            textView.setText("Idle...");
            scanning = false;
        }
        temp = temp +1;
        dataString = ""+temp;
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        if (advertiser == null) {

            Toast.makeText(this, "Please turn on bluetooth.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Advertise Setting
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // Advertise ??
                /*
                ?? code
                The ADVERTISE_MODE_LOW_POWER option is the default setting for advertising and transmits the least frequently in order to conserve the most power.
                The ADVERTISE_MODE_BALANCED option attempts to conserve power without waiting too long between advertisements.
                */
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false).build();

        // Set Advertise Data
        if (bits == 16) {
            pUuid = pUuid16;
        } else {
            pUuid = pUuid128;
        }
        final AdvertiseData data = new AdvertiseData.Builder().setIncludeDeviceName(includeDeviceName)
                .addServiceUuid(pUuid).addServiceData(pUuid, dataString.getBytes(Charset.forName("UTF-8")))
                .build();

        // Set Advertise Callback
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d("BLE", "Data.tostring : " + data.toString());
                Log.d("BLE", "Data.serviceData.tostring : " + data.getServiceData());
                Log.d("BLE", "Data.serviceData.size : " + data.getServiceData().size());
                Log.d("BLE", "Advertising Success");
                Toast.makeText(MainActivity.this, "Advertising Success", Toast.LENGTH_SHORT).show();
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.d("BLE", "Data.tostring : " + data.toString());
                Log.d("BLE", "Data.serviceData.tostring : " + data.getServiceData());
                Log.d("BLE", "Data.serviceData.size : " + data.getServiceData().size());

                String description = "";
                if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                    description = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                    description = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED)
                    description = "ADVERTISE_FAILED_ALREADY_STARTED";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                    description = "ADVERTISE_FAILED_DATA_TOO_LARGE - try again!";
                    includeDeviceName = false;
                } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
                    description = "ADVERTISE_FAILED_INTERNAL_ERROR";
                else
                    description = "unknown";

                Log.e("BLE", "Advertising Failure : " + description);
                super.onStartFailure(errorCode);
                textView.setText("Advertising Failure : " + description);
            }
        };

        advertiser.startAdvertising(settings, data, advertiseCallback);
        mHandler.postDelayed(() -> advertiser.stopAdvertising(advertiseCallback), 500);
        //textView.setText("send "+data.getServiceUuids().get(0).toString());
        textView.setText("send: "+dataString);

    }

    public void discover(int bits) {
        if (scanning) {
            mBluetoothLeScanner.stopScan(filteredScanCallback);
            mBluetoothLeScanner.stopScan(unfilteredScanCallback);
            textView.setText("Idle...");
            scanning = false;
        }
            mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (mBluetoothLeScanner == null) {

                Toast.makeText(this, "Please turn on bluetooth.", Toast.LENGTH_SHORT).show();
                return;
            }
            /*
            startLeScan with 128 bit UUIDs doesn't work on native Android BLE implementation
            See:
            https://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation/19060589#19060589
             */
            if (bits == 16) {
                pUuid = pUuid16;
                ScanFilter filter = new ScanFilter.Builder()
                                    .setServiceUuid(pUuid).build();
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(filter);
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                mBluetoothLeScanner.startScan(filters, settings, filteredScanCallback);
                textView.setText("Scanning for 16 bits UUIDS ...");
            } else {
                mBluetoothLeScanner.startScan(unfilteredScanCallback);
                textView.setText("Scanning for 16 bits and 128 bits UUIDS ...");
            }

            scanning = true;
    }

    /**
     * implements ScanCallBack
     * Deze implementatie is bedoeld om de data bij een gegeven UUID uit te
     * lezen na een filtered startscan.
     * Echter, android 4.3 heeft een bug bij 128 bit UUIDs,
     * Dit werkt dus aleen bij de 16 bit UUUDs
     */
    private ScanCallback filteredScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) {
                Toast.makeText(MainActivity.this, "Scan Result = null", Toast.LENGTH_SHORT).show();
                return;
            }
            //            StringBuilder builder = new StringBuilder(result.getDevice().getName());
            //            StringBuilder builder = new StringBuilder(result.getDevice().getName());
            //            builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
            ParcelUuid uuid = result.getScanRecord().getServiceUuids().get(0);
            byte[] data = result.getScanRecord().getServiceData(uuid);
            if (data == null)
                return;
            Log.i("BLE", "result UUID : " + uuid.toString());
            Log.i("BLE", "result data : " + data);
            String res = new String(result.getScanRecord().getServiceData(uuid), Charset.forName("UTF-8"));
            Log.i("BLE", "onScanResult" + res);
            Toast.makeText(MainActivity.this, "Scan Result", Toast.LENGTH_SHORT).show();
            textView.setText(res);
            //textView.setText("Idle...");
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

    /**
     * implements ScanCallBack
     * Deze implementatie is bedoeld om de data bij een gegeven UUID uit te
     * lezen na een unfiltered startscan.
     * Android 4.3 heeft een bug bij 128 bit UUIDs,
     * Deze implementatie zal ook 128 bits UIDs uitlezen
     */
    private ScanCallback unfilteredScanCallback = new ScanCallback() {
        int fail = 0;
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) {
                Toast.makeText(MainActivity.this, "Scan Result: null", Toast.LENGTH_SHORT).show();
                return;
            }
            /*
            if (result.getScanRecord().getServiceUuids() == null) {
                Toast.makeText(MainActivity.this, "Scan Result: no ServiceUUID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (result.getScanRecord().getServiceUuids().get(0) == null) {
                Toast.makeText(MainActivity.this, "Scan Result: ServiceUUID=null", Toast.LENGTH_SHORT).show();
                return;
            }
             */
            String advertise = result.getScanRecord().toString();
            if (advertise.contains(pUuid16.getUuid().toString())) {
                System.out.println(" 16bit "+advertise);
            }
            if (advertise.contains(pUuid128.getUuid().toString())) {
                System.out.println("128bit "+advertise);
            }

            List<UUID> uuids = parseUUIDs(result.getScanRecord().getBytes());
            System.out.println("BLE UUIDs "+uuids.size());
            if (uuids.size() > 0) {
                for (int i = 0; i < uuids.size(); i++) {
                    System.out.println(uuids.get(i).toString());
                }
            }

            //Toast.makeText(MainActivity.this, "Scan Result: try to find UUID", Toast.LENGTH_SHORT).show();

            boolean found = false;
            int index = -1;
            String type = "";

            if (uuids.contains(pUuid16.getUuid())) {
                found = true;
                index = uuids.indexOf(pUuid16.getUuid());
                type = "016bit ";
            }
            if (uuids.contains(pUuid128.getUuid())) {
                found = true;
                index = uuids.indexOf(pUuid128.getUuid());
                type = "128bit ";
            }

            //int index = find(uuids, pUuid128);

            if (found) {
                ParcelUuid uuid = result.getScanRecord().getServiceUuids().get(index);
                System.out.println(type+result.getScanRecord().toString());
                byte[] data = result.getScanRecord().getServiceData(uuid);
                if (data == null) {
                    Toast.makeText(MainActivity.this, "Scan Result: UUID found", Toast.LENGTH_SHORT).show();
                    fail++;
                    textView.setText("couldn't read data from "+uuid.toString()+" ("+fail+")");
                    return;
                }
                Log.i("BLE", "result UUID : " + uuid.toString());
                Log.i("BLE", "result data : " + data);
                String res = new String(result.getScanRecord().getServiceData(uuid), Charset.forName("UTF-8"));
                Log.i("BLE", "onScanResult" + res);
                Toast.makeText(MainActivity.this, "Scan Result", Toast.LENGTH_SHORT).show();
                textView.setText(res);
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

    Integer find(List<UUID> uuids, UUID search) {
        if (uuids.size() > 0) {
            for (int i = 0; i < uuids.size(); i++) {
                UUID el = uuids.get(i);
                if (el.toString().toLowerCase().equals(search.toString().toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    String fullAdvertiseText(AdvertiseData data) {
        // Android usually uses a random resolvable MAC address instead of its public one,
        // and you can't extract that one. It changes every 15 minutes anyway.
        List<ParcelUuid> sulist = data.getServiceUuids();
        return ""+sulist.size();
    }

    /**
     * Android 4.3 bug 36978399:
     * BLE filtering in startLeScan(UUIDs, callback) doesn't work for 128-bit UUIDs
     *
     * Although Android 4.3 doesn't seem to support filtering by 128-bit UUIDs, these UUIDs
     * are likely present in the byte[] scanRecord returned by the LeScanCallback.
     *
     * Here's an implementation which fixes that bug and adds 128-bit support
     *
     * @param advertisedData
     * @return List of UUIDs in the advertisedData
     */
    List<UUID> parseUUIDs(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return uuids;
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        return super.onOptionsItemSelected(item);
    }
    */
}