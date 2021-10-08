package com.wonderpush.tests;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.wonderpush.sdk.WonderPush;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView txtRandom = findViewById(R.id.actMain_txtRandom);
        txtRandom.setText("" + (long) Math.floor(Math.random() * 10_000));
    }

    @Override
    protected void onResume() {
        super.onResume();
        closeClickedNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        closeClickedNotification();
    }

    private void closeClickedNotification() {
        Intent intent = getIntent();
        if (intent == null) return;
        String tag = intent.getStringExtra("closeNotificationTag");
        int localNotificationId = intent.getIntExtra("closeNotificationId", -1);
        if (localNotificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(tag, localNotificationId);
        }
    }

    public void btnChildActivity_onClick(View view) {
        startActivityForResult(new Intent(this, ChildActivity.class), 0);
    }

    public void btnTriggerInappCard_onClick(View view) {
        WonderPush.trackEvent("trigger-inapp-card");
    }

    public void btnTriggerInappModal_onClick(View view) {
        WonderPush.trackEvent("trigger-inapp-modal");
    }

    public void btnTriggerInappImage_onClick(View view) {
        WonderPush.trackEvent("trigger-inapp-image");
    }

    public void btnTriggerInappBanner_onClick(View view) {
        WonderPush.trackEvent("trigger-inapp-banner");
    }

}
