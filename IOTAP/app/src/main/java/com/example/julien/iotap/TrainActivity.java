package com.example.julien.iotap;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by julien on 24/11/17.
 *
 * Activity that creates and manage the content of the "train file"
 */

public class TrainActivity extends AppCompatActivity {
    // Constants
    public static final String TRAIN_FILE = "train.csv";

    // Views
    Toolbar m_toolbar;

    // Events
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_train);

        // Get Views
        m_toolbar = findViewById(R.id.train_toolbar);

        // Setup toolbar
        setSupportActionBar(m_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
