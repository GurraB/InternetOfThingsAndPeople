package com.example.julien.iotap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Created by julien on 17/11/17.
 *
 * Allow the user to search the bluetooth device he want to use
 */

public class MainActivity extends AppCompatActivity {
    // Constants
    public static final int DEVICE_ACTIVITY = 1;

    public static final String BLUETOOTH_DEVICE = "com.example.julien.iotap.MainActivity.BLUETOOTH_DEVICE";

    // Unique UUID for this application
    private static final UUID RFCOMM_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");

    // Attributes
    BluetoothAdapter m_BluetoothAdapter;
    BluetoothDevice m_bluetoothDevice;
    BluetoothSocket m_socket;
    BufferedReader m_stream;

    DeviceConnection dev_connection;
    DeviceConnected  dev_connected;

    Toolbar m_toolbar;
    TextView m_device_name;
    TextView m_bluetooth_status;
    TextView m_raw_data;

    // Events
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get Views
        m_toolbar          = findViewById(R.id.main_toolbar);
        m_device_name      = findViewById(R.id.device_name);
        m_bluetooth_status = findViewById(R.id.bluetooth_status);
        m_raw_data         = findViewById(R.id.raw_data);

        // Setup toolbar
        setSupportActionBar(m_toolbar);

        // Setup listener
        m_bluetooth_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_bluetoothDevice == null) return;

                // Stop current tasks
                if (dev_connected  != null) dev_connected.cancel(true);
                if (dev_connection != null) dev_connection.cancel(true);

                // Connect or disconnect
                if (m_socket == null || !m_socket.isConnected()) {
                    connect();
                } else {
                    try {
                        m_stream.close();
                        m_socket.close();
                        dev_connection.onProgressUpdate(ConnectionState.DISCONNECTED);
                    } catch (IOException err) {
                        Log.e("MainActivity", "Unable to close", err);
                    }
                }
            }
        });

        // check has bluetooth ?
        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (m_BluetoothAdapter == null) {
            new ErrorFragment().setMsg(R.string.nobt_err_msg).show(getFragmentManager(), "error_dialog");
            return;
        }

        // Retrive saved bluetooth device
        if (savedInstanceState != null) {
            m_bluetoothDevice = savedInstanceState.getParcelable(BLUETOOTH_DEVICE);
            refresh_device_name();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                // Stop current tasks
                if (dev_connected  != null) dev_connected.cancel(true);
                if (dev_connection != null) dev_connection.cancel(true);

                startActivityForResult(new Intent(this, DeviceActivity.class), DEVICE_ACTIVITY);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DEVICE_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    m_bluetoothDevice = data.getParcelableExtra(BLUETOOTH_DEVICE);
                    refresh_device_name();
                    connect();
                }

                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current bluetooth device
        if (m_bluetoothDevice != null) outState.putParcelable(BLUETOOTH_DEVICE, m_bluetoothDevice);
    }

    // Methods
    private void refresh_device_name() {
        if (m_bluetoothDevice == null) return;

        // Name
        String name = m_bluetoothDevice.getName();

        if (name != null) {
            m_device_name.setText(name);
        } else {
            m_device_name.setText(m_bluetoothDevice.getAddress());
        }
    }

    private void connect() {
        if (m_bluetoothDevice == null) return;
        dev_connection = new DeviceConnection();
        dev_connection.execute();
    }

    // Subclasses
    enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR };
    class DeviceConnection extends AsyncTask<Object,ConnectionState,Boolean> {
        // Methods
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                publishProgress(ConnectionState.CONNECTING);
                m_socket = m_bluetoothDevice.createInsecureRfcommSocketToServiceRecord(RFCOMM_UUID);
            } catch (IOException err) {
                Log.e("MainActivity", "Unable to create socket", err);
                publishProgress(ConnectionState.ERROR);
                m_socket = null;
                return false;
            }

            try {
                m_socket.connect();
                publishProgress(ConnectionState.CONNECTED);
            } catch (IOException err) {
                Log.e("MainActivity", "Unable to connect", err);
                publishProgress(ConnectionState.ERROR);
                m_socket = null;
                return false;
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(ConnectionState... values) {
            switch (values[0]) {
                case CONNECTED:
                    m_bluetooth_status.setText(R.string.connected);
                    break;

                case CONNECTING:
                    m_bluetooth_status.setText(R.string.connecting);
                    break;

                case ERROR:
                    new ErrorFragment().setMsg(R.string.connection_err_msg).show(getFragmentManager(), "error_dialog");

                case DISCONNECTED:
                    m_bluetooth_status.setText(R.string.disconnected);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            if (ok) {
                dev_connected = new DeviceConnected();
                dev_connected.execute();
            }
        }
    }

    class DeviceConnected extends AsyncTask<Object,String,Object> {
        @Override
        protected Object doInBackground(Object... objects) {
            try {
                m_stream = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
                while (true) {
                    String str = m_stream.readLine();
                    publishProgress(str);
                }
            } catch (IOException err) {
                Log.e("MainActivity", "Error while reading", err);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            m_raw_data.setText(values[0] + m_raw_data.getText());
        }
    }
}
