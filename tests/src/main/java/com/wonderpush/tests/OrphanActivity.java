package com.wonderpush.tests;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class OrphanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orphan);

        TextView txtRandom = findViewById(R.id.actOrphan_txtRandom);
        txtRandom.setText("" + (long) Math.floor(Math.random() * 10_000));
    }

}
