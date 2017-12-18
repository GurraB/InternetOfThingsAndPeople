package com.example.julien.iotap;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by julien on 24/11/17.
 *
 * Activity that creates and manage the content of the "train file"
 */

public class TrainActivity extends AppCompatActivity implements ConnectionFragment.OnReceiveListener, ResetConfirmFragment.OnResetListener {
    // Constants
    public static final String TRAIN_FILE = "train.csv";

    // Attributes
    Toolbar m_toolbar;
    ToggleButton m_train_button;
    TextView m_content;
    Spinner m_gesture;
    Button m_reset_button;

    ConnectionFragment m_connection_manager;
    ArrayList<String> m_buffer = new ArrayList<>();
    File m_train_file;

    // Events
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_train);

        // Get Views
        m_toolbar = findViewById(R.id.train_toolbar);
        m_train_button = findViewById(R.id.train_button);
        m_content = findViewById(R.id.content);
        m_gesture = findViewById(R.id.gesture_spinner);
        m_reset_button = findViewById(R.id.reset_button);

        // Get Fragments
        m_connection_manager = (ConnectionFragment) getFragmentManager().findFragmentById(R.id.train_connection_manager);

        // Setup toolbar
        setSupportActionBar(m_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup listeners
        m_train_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_gesture.setEnabled(!m_train_button.isChecked());
            }
        });

        m_reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResetConfirmFragment().show(getFragmentManager(), "reset_confirm_dialog");
            }
        });

        // Open train file
        m_train_file = new File(getFilesDir(), TRAIN_FILE);
        try {
            if (m_train_file.createNewFile()) initFile();
        } catch (IOException err) {
            Log.e("TrainActivity", "Unable to create train file", err);
        }

        updateContent();
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
    public void onConnect() {
        m_train_button.setEnabled(true);
    }

    @Override
    public void onReceive(String data) {
        final int WINDOW_SIZE = getResources().getInteger(R.integer.window_size);

        // clean
        while ((!data.startsWith("h")) && (data.length() > 0)) {
            data = data.substring(1);
        }

        if (data.matches("h(,-?\\d+){6},?") && m_train_button.isChecked()) {
            m_buffer.add(data);

            if (m_buffer.size() == WINDOW_SIZE) {
                try {
                    BufferedOutputStream sw = new BufferedOutputStream(new FileOutputStream(m_train_file, true));

                    // Write data
                    for (int i = 0; i < WINDOW_SIZE; ++i) {
                        sw.write(m_buffer.get(i).substring(2).getBytes());
                    }

                    // Add label
                    String label = (String) m_gesture.getSelectedItem();
                    sw.write(String.format("\"%s\"%n", label).getBytes());

                    // Close stream
                    sw.flush();
                    sw.close();

                    m_buffer.clear();
                } catch (Exception err) {
                    Log.e("TrainActivity", "Unable to write to train file", err);
                }

                updateContent();
            }
        }
    }

    @Override
    public void onDisconnect() {
        m_train_button.setEnabled(false);
        m_train_button.setChecked(false);
        m_gesture.setEnabled(true);
        m_buffer.clear();
    }

    @Override
    public void onReset() {
        try {
            initFile();
        } catch (IOException err) {
            Log.e("TrainActivity", "Unable to reset train file", err);
        }

        updateContent();
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

    // Méthods
    private void initFile() throws IOException {
        final int WINDOW_SIZE = getResources().getInteger(R.integer.window_size);

        // Init file
        BufferedOutputStream sw = new BufferedOutputStream(new FileOutputStream(m_train_file));

        for (int i = 1; i <= WINDOW_SIZE; ++i) {
            sw.write(String.format("AccX%1$s,AccY%1$s,AccZ%1$s,GyrX%1$s,GyrY%1$s,GyrZ%1$s,", i).getBytes());
        }

        sw.write(String.format("Label%n").getBytes());
        sw.flush();
        sw.close();
    }

    private void updateContent() {
        final String[] gestures = getResources().getStringArray(R.array.gestures);
        HashMap<String,Integer> stats = new HashMap<>();
        int nb_lines = 0;

        // Init stats
        for (int i = 0; i < gestures.length; ++i) {
            stats.put(gestures[i], 0);
        }

        try {
            BufferedReader sr = new BufferedReader(new FileReader(m_train_file));
            sr.readLine(); // Skip the 1st one

            while (true) {
                String s = sr.readLine();
                if (s == null) break;

                // Extract label
                String label = s.substring(s.indexOf('"')+1, s.lastIndexOf('"'));
                stats.put(label, stats.get(label)+1);
                ++nb_lines;
            }

            // Print on screen
            m_content.setText("File Contents :");
            for (int i = 0; i < gestures.length; ++i) {
                m_content.append(String.format("%n%s : %s", gestures[i], stats.get(gestures[i])));
            }
        } catch (Exception err) {
            Log.e("TrainActivity", "Unable to read train file", err);
        }
    }
}
