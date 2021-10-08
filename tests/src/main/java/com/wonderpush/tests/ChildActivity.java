package com.wonderpush.tests;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ChildActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child);

        TextView txtRandom = findViewById(R.id.actChild_txtRandom);
        txtRandom.setText("" + (long) Math.floor(Math.random() * 10_000));
    }

}
