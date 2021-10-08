package com.wonderpush.tests;

import java.lang.reflect.Field;
import java.util.Arrays;

import android.app.Activity;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class NavigationActivity extends Activity {

    TextView txtNavigationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        txtNavigationText = (TextView) findViewById(R.id.actNavigation_txtNavigationText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshIntent(intent);
    }

    private void refreshIntent(Intent intent) {
        if (intent == null) {
            txtNavigationText.setText("Null intent");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ");
        sb.append(intent.getPackage());
        sb.append("\n\n");
        sb.append("Component: ");
        sb.append(intent.getComponent());
        sb.append("\n\n");
        sb.append("Action: ");
        sb.append(intent.getAction());
        sb.append("\n\n");
        sb.append("Categories: ");
        if (intent.getCategories() == null) {
            sb.append("(none)");
            sb.append("\n\n");
        } else {
            sb.append("\n");
            for (String category : intent.getCategories()) {
                sb.append("- ");
                sb.append(category);
                sb.append("\n");
            }
            sb.append("\n");
        }
        sb.append("Type: ");
        sb.append(intent.getType());
        sb.append("\n\n");
        sb.append("Data: ");
        sb.append(intent.getDataString());
        sb.append("\n\n");
        sb.append("Flags: ");
        sb.append("0x");
        sb.append(Integer.toHexString(intent.getFlags()).toUpperCase());
        sb.append("\n");
        Class<Intent> intentCls = Intent.class;
        for (Field field : intentCls.getDeclaredFields()) {
            if (!field.getName().startsWith("FLAG_")) continue;
            if (field.getType() == Integer.TYPE) {
                try {
                    int mask = field.getInt(null);
                    if ((intent.getFlags() & mask) == mask) {
                        sb.append("- ");
                        sb.append(field.getName());
                        sb.append(" (0x");
                        sb.append(Integer.toHexString(mask).toUpperCase());
                        sb.append(")\n");
                    }
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    Log.e("WonderPushDemo", "Failed to get field int value", e);
                }
            }
        }
        sb.append("\n");
        sb.append("Extras: ");
        Bundle extras = intent.getExtras();
        if (extras == null) {
            sb.append("(none)");
            sb.append("\n\n");
        } else {
            sb.append("\n");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                sb.append("- ");
                sb.append(key);
                sb.append(" = (");
                sb.append(value == null ? "null" : value.getClass().getSimpleName());
                sb.append(") ");
                if (value != null && value.getClass().isArray()) {
                    if (!value.getClass().getComponentType().isPrimitive())
                        sb.append(Arrays.toString((Object[])value));
                    if (value instanceof boolean[])
                        sb.append(Arrays.toString((boolean[])value));
                    if (value instanceof byte[])
                        sb.append(Arrays.toString((byte[])value));
                    if (value instanceof char[])
                        sb.append(Arrays.toString((char[])value));
                    if (value instanceof double[])
                        sb.append(Arrays.toString((double[])value));
                    if (value instanceof float[])
                        sb.append(Arrays.toString((float[])value));
                    if (value instanceof int[])
                        sb.append(Arrays.toString((int[])value));
                    if (value instanceof long[])
                        sb.append(Arrays.toString((long[])value));
                    if (value instanceof short[])
                        sb.append(Arrays.toString((short[])value));
                } else {
                    sb.append(String.valueOf(value));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        txtNavigationText.setText(sb.toString());
    }

}
