package com.example.julien.iotap;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;

/**
 * Created by julien on 25/11/17.
 */

public class ConnectionFragment extends Fragment {
    // Constants
    public static final String BLUETOOTH_DEVICE = "com.example.julien.iotap.MainActivity.BLUETOOTH_DEVICE";

    // Attributes
    OnReceiveListener m_listener;
    BluetoothDevice m_device;
    BluetoothSocket m_socket;
    BufferedReader m_stream;

    DeviceConnection dev_connection;
    DeviceConnected dev_connected;

    // Views
    ImageView m_status;
    TextView m_device_name;
    Button m_connection_btn;

    // Events
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            m_listener = (OnReceiveListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + " must implement OnReceiveListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.connection_fragment, container, false);

        // Get views
        m_status = layout.findViewById(R.id.connection_status);
        m_device_name = layout.findViewById(R.id.device_name);
        m_connection_btn = layout.findViewById(R.id.connection_button);

        // Setup button
        m_connection_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_device == null) return;

                // Stop current tasks
                if (dev_connection != null) dev_connection.cancel(true);
                if (dev_connected  != null) dev_connected.cancel(true);

                // Connect or disconnect
                if (m_socket == null || !m_socket.isConnected()) {
                    connect();
                } else {
                    disconnect();
                }
            }
        });

        // Get saved device
        if (savedInstanceState != null) {
            m_device = (BluetoothDevice) savedInstanceState.getParcelable(BLUETOOTH_DEVICE);
            refresh_device_name();
        }

        return layout;
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop current tasks
        if (dev_connected  != null) dev_connected.cancel(true);
        if (dev_connection != null) dev_connection.cancel(true);
        disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save bluetooth device
        outState.putParcelable(BLUETOOTH_DEVICE, m_device);
    }

    // Méthods
    public void setDevice(BluetoothDevice device) {
        m_device = device;
        refresh_device_name();
    }

    public BluetoothDevice getDevice() {
        return m_device;
    }

    private void refresh_device_name() {
        m_connection_btn.setEnabled(m_device != null);

        if (m_device == null) return;

        // Name
        String name = m_device.getName();

        if (name != null) {
            m_device_name.setText(name);
        } else {
            m_device_name.setText(m_device.getAddress());
        }
    }

    public void connect() {
        if (m_device == null) return;

        // Start connection process
        dev_connection = new DeviceConnection();
        dev_connection.execute();
    }

    public void disconnect() {
        if (dev_connection == null) return;

        try {
            if (m_socket != null) m_socket.close();
            dev_connection.onProgressUpdate(ConnectionState.DISCONNECTED);

            Log.d("ConnectionFragment", "disconnected");
        } catch (IOException err) {
            Log.e("ConnectionFragment", "Unable to close", err);
        }
    }

    // Listener
    interface OnReceiveListener {
        void onConnect();
        void onReceive(String data);
        void onDisconnect();
    }

    // Subclasses
    enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR };
    class DeviceConnection extends AsyncTask<Object,ConnectionState,Boolean> {
        // Methods
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                publishProgress(ConnectionState.CONNECTING);
                m_socket = m_device.createInsecureRfcommSocketToServiceRecord(
                        UUID.fromString(getString(R.string.rfcomm_uuid))
                );
            } catch (IOException err) {
                Log.e("ConnectionFragment", "Unable to create socket", err);
                publishProgress(ConnectionState.ERROR);
                m_socket = null;
                return false;
            }

            try {
                m_socket.connect();
                publishProgress(ConnectionState.CONNECTED);
            } catch (IOException err) {
                Log.e("ConnectionFragment", "Unable to connect", err);
                publishProgress(ConnectionState.ERROR);
                m_socket = null;
                return false;
            }

            Log.d("ConnectionFragment", "connected");
            return true;
        }

        @Override
        protected void onProgressUpdate(ConnectionState... values) {
            switch (values[0]) {
                case CONNECTED:
                    m_status.setImageResource(R.drawable.ic_bluetooth_connected_black_36dp);
                    m_connection_btn.setText(R.string.disconnect);
                    m_connection_btn.setEnabled(true);

                    m_listener.onConnect();
                    break;

                case CONNECTING:
                    m_status.setImageResource(R.drawable.ic_bluetooth_disabled_black_36dp);
                    m_connection_btn.setText(R.string.connecting);
                    m_connection_btn.setEnabled(false);
                    break;

                case ERROR:
                    new ErrorFragment().setMsg(R.string.connection_err_msg).show(getFragmentManager(), "error_dialog");

                case DISCONNECTED:
                    m_status.setImageResource(R.drawable.ic_bluetooth_disabled_black_36dp);
                    m_connection_btn.setText(R.string.connect);
                    m_connection_btn.setEnabled(true);

                    m_listener.onDisconnect();
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
                BufferedWriter stream = new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream()));
                stream.write("w20", 0, 3);
                stream.flush();
            } catch (IOException err) {
                Log.e("ConnectionFragment", "Error while sending", err);
            }

            try {
                m_stream = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
                while (true) {
                    String str = m_stream.readLine();
                    publishProgress(str);
                }
            } catch (IOException err) {
                Log.e("ConnectionFragment", "Error while reading", err);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            m_listener.onReceive(values[0]);
        }
    }
}
