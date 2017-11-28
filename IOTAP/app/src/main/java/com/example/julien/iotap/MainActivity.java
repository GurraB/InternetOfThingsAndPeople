package com.example.julien.iotap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

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
    File m_train_file;
    WekaTask m_weka_task;
    Classifier m_classifier;
    boolean m_weka_ready = false;

    Instances m_dataset;
    ArrayList<Integer> m_instance = new ArrayList<>();

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

        // Init Weka
        m_train_file = new File(getFilesDir(), TrainActivity.TRAIN_FILE);
        if (m_train_file.exists()) initWeka();
    }

    @Override
    public void onConnect() {
        m_raw_data.setText("");
    }

    @Override
    public void onReceive(String data) {
        final int WINDOW_SIZE = getResources().getInteger(R.integer.window_size);
        final String[] GESTURES = getResources().getStringArray(R.array.gestures);

        if (data.matches("h(,-?\\d+){6},?")) {
            m_raw_data.setText("mvt : " + data + '\n' + m_raw_data.getText());
            if (!m_weka_ready) return;

            // Add data
            String[] ints = data.split(",");

            for (int i = 1; i < 7; ++i) {
                m_instance.add(Integer.valueOf(ints[i]));
            }

            // If have 1 mouvement
            if (m_instance.size() == WINDOW_SIZE * 6) {
                // Create the instance
                double[] values = new double[m_instance.size()+1];
                for (int i = 0; i < m_instance.size(); ++i) {
                    values[i] = m_instance.get(i);
                }
                values[m_instance.size()] = -1;
                m_instance.clear();

                // Add to test data
                DenseInstance denseInstance = new DenseInstance(1.0, values);
                denseInstance.setDataset(m_dataset);

                try {
                    // Label it
                    int label = (int) m_classifier.classifyInstance(denseInstance);
                    m_raw_data.setText("mvt : " + String.valueOf(label) + " => " + m_dataset.classAttribute().value(label) + '\n' + m_raw_data.getText());
                } catch (Exception err) {
                    Log.e("MainActivity", "Unable to classify", err);
                }
            }
        } else {
            m_raw_data.setText(data + '\n' + m_raw_data.getText());
        }
    }

    @Override
    public void onDisconnect() {
        m_instance.clear();
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

    // Méthods
    void initWeka() {
        if (m_weka_task != null) return;
        if (!m_train_file.exists()) return;

        // Start wekatask
        m_weka_task = new WekaTask();
        m_weka_task.execute();
    }

    // Subclasses
    class WekaTask extends AsyncTask<Object,Object,Object> {
        // Events
        @Override
        protected Object doInBackground(Object... objects) {
            // Loads train file
            try {
                ConverterUtils.DataSource source = new ConverterUtils.DataSource(m_train_file.getAbsolutePath());
                m_dataset = source.getDataSet();
                m_dataset.setClassIndex(m_dataset.numAttributes() - 1); // Last attribute => LABEL
            } catch (Exception err) {
                Log.e("WekaTask", "Unable to load train file", err);
                return null;
            }

            // Initialize classifier
            try {
                m_classifier = new BayesNet();
                m_classifier.buildClassifier(m_dataset);
            } catch (Exception err) {
                Log.e("WekaTask", "Unable to build classifier", err);
            }

            m_weka_ready = true;

            return null;
        }
    }
}
