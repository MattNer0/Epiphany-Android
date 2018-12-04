package com.neromatt.epiphany.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.neromatt.epiphany.Constants;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ErrorActivity extends AppCompatActivity {
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);

        Intent intent = getIntent();
        String stackTraceString = intent.getStringExtra(Constants.EXTRA_STACK_TRACE);

        Log.i(Constants.LOG, stackTraceString);

        TextView stack_view = findViewById(R.id.stack_textview);
        stack_view.setText(stackTraceString);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar!=null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }
}
