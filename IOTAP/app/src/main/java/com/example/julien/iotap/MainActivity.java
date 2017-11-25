package com.example.julien.iotap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by julien on 17/11/17.
 *
 * Allow the user to search the bluetooth device he want to use
 */

public class MainActivity extends AppCompatActivity implements ConnectionFragment.OnReceiveListener {
    // Constants
    public static final int DEVICE_ACTIVITY = 1;

    // Attributes
    BluetoothAdapter m_BluetoothAdapter;

    Toolbar m_toolbar;
    TextView m_device_name;
    TextView m_bluetooth_status;
    TextView m_raw_data;

    ConnectionFragment m_connection_manager;

    // Events
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get Views
        m_toolbar  = findViewById(R.id.main_toolbar);
        m_raw_data = findViewById(R.id.raw_data);

        // Get fragments
        m_connection_manager = (ConnectionFragment) getFragmentManager().findFragmentById(R.id.main_connection_manager);

        // Setup toolbar
        setSupportActionBar(m_toolbar);

        // check has bluetooth ?
        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (m_BluetoothAdapter == null) {
            new ErrorFragment().setMsg(R.string.nobt_err_msg).show(getFragmentManager(), "error_dialog");
        }
    }

    @Override
    public void onConnect() {
        m_raw_data.setText("");
    }

    @Override
    public void onReceive(String data) {
        m_raw_data.setText(data + '\n' + m_raw_data.getText());
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
                startActivityForResult(new Intent(this, DeviceActivity.class), DEVICE_ACTIVITY);
                return true;

            case R.id.action_train:
                Intent intent = new Intent(this, TrainActivity.class);
                intent.putExtra(ConnectionFragment.BLUETOOTH_DEVICE, m_connection_manager.getDevice());

                startActivity(intent);
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
                    m_connection_manager.setDevice((BluetoothDevice) data.getParcelableExtra(ConnectionFragment.BLUETOOTH_DEVICE));
                }

                break;
        }
    }
}
