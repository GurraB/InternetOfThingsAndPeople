package com.example.julien.iotap;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class DeviceActivity extends AppCompatActivity {
    // Constants
    public final static int REQUEST_ENABLE_BT = 1;
    public final static int REQUEST_ACCESS_COARSE_LOCATION = 2;

    // Attributes
    BluetoothAdapter m_BluetoothAdapter;
    ArrayList<BluetoothDevice> m_founddevices = new ArrayList<>();

    ListView m_pairedlist_view;
    ListView m_foundlist_view;
    Toolbar m_toolbar;

    boolean m_registred = false;
    BroadcastReceiver m_bluetoothreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    boolean exist = false;
                    for (BluetoothDevice b : m_founddevices) {
                        if (b.getAddress().equals(device.getAddress())) {
                            exist = true;
                            break;
                        }
                    }

                    if (!exist) {
                        m_founddevices.add(device);
                        m_foundlist_view.setAdapter(new BtAdapter(m_founddevices));
                    }

                    Log.i("Discovery", String.format("Found %s on %s", device.getName(), device.getAddress()));

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    // If bond ok : return the device
                    if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE) == BluetoothDevice.BOND_BONDED) {
                        Intent data = new Intent();
                        data.putExtra(MainActivity.BLUETOOTH_DEVICE, device);

                        DeviceActivity.this.setResult(RESULT_OK, data);
                        DeviceActivity.this.finish();
                    }
            }
        }
    };

    // Events
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set-up layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Get Views
        m_pairedlist_view = findViewById(R.id.list_paired_devices);
        m_foundlist_view = findViewById(R.id.list_found_devices);
        m_toolbar = findViewById(R.id.device_toolbar);

        // Setup the toolbar
        setSupportActionBar(m_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup listeners
        m_pairedlist_view.setOnItemClickListener(new DeviceClick());
        m_foundlist_view.setOnItemClickListener(new DeviceClick());

        // Permission Check
        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            btEnable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    updateBtLists();
                } else {
                    new ErrorFragment().setMsg(R.string.btdisabled_err_msg).show(getFragmentManager(), "error_dialog");
                }

                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION:
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    new ErrorFragment().setMsg(R.string.noallowed_err_msg).show(getFragmentManager(), "error_dialog");
                    finish();
                } else {
                    btEnable();
                }

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.device_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateBtLists();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (m_BluetoothAdapter.cancelDiscovery()) {
            Log.i("Discovery", "Discovery stoped");
        } else {
            Log.i("Discovery", "Unable to stop discovery");
        }

        if (m_registred) unregisterReceiver(m_bluetoothreceiver);
    }

    // Méthods
    private void btEnable() {
        // Register receiver for bluetooth discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(m_bluetoothreceiver, filter);
        m_registred = true;

        // Enable bluetooth
        if (!m_BluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            updateBtLists();
        }
    }

    private void updateBtLists() {
        // Paired devices
        Set<BluetoothDevice> paired_devices = m_BluetoothAdapter.getBondedDevices();
        m_pairedlist_view.setAdapter(new BtAdapter(paired_devices));

        // Start discovery
        m_founddevices.clear();
        m_foundlist_view.setAdapter(new BtAdapter(m_founddevices));

        if (m_BluetoothAdapter.startDiscovery()) {
            Log.i("Discovery", "Discovery started");
        } else {
            Log.i("Discovery", "Unable to start discovery");
        }
    }

    // Sub-classes
    private class DeviceClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            BluetoothDevice device = (BluetoothDevice) adapterView.getAdapter().getItem(pos);

            // If paired return it else ask for bond
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d("DeviceActivity", "Already bond with " + device.getAddress());

                Intent data = new Intent();
                data.putExtra(MainActivity.BLUETOOTH_DEVICE, device);

                setResult(RESULT_OK, data);
                finish();
            } else {
                Log.d("DeviceActivity", "Ask to bond with " + device.getAddress());
                device.createBond();
            }
        }
    }

    private class BtAdapter extends BaseAdapter {
        // Attributes
        ArrayList<BluetoothDevice> m_devices = new ArrayList<>();

        // Constructors
        public BtAdapter(Collection<BluetoothDevice> devices) {
            m_devices.addAll(devices);
        }

        @Override
        public int getCount() {
            return m_devices.size();
        }

        @Override
        public Object getItem(int i) {
            return m_devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.device_list_item, viewGroup, false);
            }

            String name = m_devices.get(i).getName();
            if (name == null) {
                name = m_devices.get(i).getAddress();
            }

            ((TextView) view.findViewById(R.id.btlist_name)).setText(name);

            return view;
        }
    }
}
