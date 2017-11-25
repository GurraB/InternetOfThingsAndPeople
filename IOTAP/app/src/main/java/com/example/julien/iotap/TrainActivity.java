package com.example.julien.iotap;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Created by julien on 24/11/17.
 *
 * Activity that creates and manage the content of the "train file"
 */

public class TrainActivity extends AppCompatActivity implements ConnectionFragment.OnReceiveListener {
    // Constants
    public static final String TRAIN_FILE = "train.csv";

    // Attributes
    Toolbar m_toolbar;

    ConnectionFragment m_connection_manager;

    // Events
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_train);

        // Get Views
        m_toolbar = findViewById(R.id.train_toolbar);

        // Get Fragments
        m_connection_manager = (ConnectionFragment) getFragmentManager().findFragmentById(R.id.train_connection_manager);

        // Setup toolbar
        setSupportActionBar(m_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Setup Fragment
        Intent intent = getIntent();
        BluetoothDevice device = intent.getParcelableExtra(ConnectionFragment.BLUETOOTH_DEVICE);

        if (device != null) {
            m_connection_manager.setDevice(device);
        } else {
            new ErrorFragment().setMsg(R.string.no_dev_sel_err_msg).show(getFragmentManager(), "error_dialog");
        }
    }

    @Override
    public void onReceive(String data) {

    }

    @Override
    public void onConnect() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
