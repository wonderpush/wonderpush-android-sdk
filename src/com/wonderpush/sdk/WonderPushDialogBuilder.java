package com.wonderpush.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.wonderpush.sdk.R;

class WonderPushDialogBuilder {

    private static final String TAG = WonderPush.TAG;

    public interface OnChoice {
        /**
         * @param dialog
         * @param which The clicked button or null.
         */
        public void onChoice(WonderPushDialogBuilder dialog, Button which);
    }

    final Context activity;
    final JSONObject data;
    final AlertDialog.Builder builder;
    final OnChoice listener;
    AlertDialog dialog;

    String defaultTitle;
    int defaultIcon;
    final AtomicReference<Button> choice = new AtomicReference<Button>();
    final List<Button> buttons;

    public WonderPushDialogBuilder(Context activity, JSONObject data, OnChoice listener) {
        this.activity = activity;
        this.data = data;
        builder = new AlertDialog.Builder(activity);
        this.listener = listener;

        defaultTitle = this.activity.getApplicationInfo().name;
        defaultIcon = WonderPushBroadcastReceiver.getNotificationIcon(activity);
        if (defaultIcon == -1) {
            defaultIcon = this.activity.getApplicationInfo().icon;
        }

        JSONArray buttons = data.optJSONArray("buttons");
        int buttonCount = buttons != null ? buttons.length() : 0;
        if (buttonCount > 3) {
            Log.w(TAG, "Can't handle more than 3 dialog buttons! Using only the first 3.");
            buttonCount = 3;
        }
        this.buttons = new ArrayList<Button>(buttonCount);
        for (int i = 0 ; i < buttonCount ; ++i) {
            JSONObject button = buttons.optJSONObject(i);
            if (button == null) {
                continue;
            }
            this.buttons.add(new Button(button));
        }

        commonSetup();
    }

    private void commonSetup() {
        // onDismissListener has moved to show() for backward compatibility
    }

    public Context getContext() {
        return activity;
    }

    public JSONObject getData() {
        return data;
    }

    public void setDefaultTitle(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public void setDefaultIcon(int defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public WonderPushDialogBuilder setupTitleAndIcon() {
        if (data.has("title")) {
            builder.setTitle(data.optString("title", defaultTitle));
            builder.setIcon(defaultIcon);
        }
        return this;
    }

    public WonderPushDialogBuilder setupButtons() {
        if (listener == null) {
            Log.e(TAG, "Calling WonderPushDialogBuilder.setupButtons() without OnChoice listener, ignoring!");
            if (buttons.size() > 0) {
                builder.setNegativeButton(R.string.wonderpush_close, null);
            }
            return this;
        }

        // Derive button genre from desired positions
        int positiveIndex = -1;
        int negativeIndex = -1;
        int neutralIndex  = -1;
        if (buttons.size() == 1) {
            positiveIndex = 0;
        } else if (buttons.size() == 2) {
            negativeIndex = 0;
            positiveIndex = 1;
        } else if (buttons.size() >= 3) {
            negativeIndex = 0;
            neutralIndex = 1;
            positiveIndex = 2;
        }

        if (negativeIndex >= 0) {
            final Button button = buttons.get(negativeIndex);
            builder.setNegativeButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice.set(button);
                }
            });
        }
        if (neutralIndex >= 0) {
            final Button button = buttons.get(neutralIndex);
            builder.setNeutralButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice.set(button);
                }
            });
        }
        if (positiveIndex >= 0) {
            final Button button = buttons.get(positiveIndex);
            builder.setPositiveButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice.set(button);
                }
            });
        }

        return this;
    }

    public WonderPushDialogBuilder setMessage(String message) {
        builder.setMessage(message);
        return this;
    }

    public WonderPushDialogBuilder setView(View view) {
        builder.setView(view);
        return this;
    }

    public void show() {
        if (dialog == null) {
            dialog = builder.create();
            // On dismiss, handle chosen button, if any
            DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    WonderPush.logDebug("Dialog dismissed");
                    if (listener != null) {
                        listener.onChoice(WonderPushDialogBuilder.this, choice.get());
                    }
                }
            };
            dialog.setOnDismissListener(dismissListener);
        }
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    static class Button {

        public String label;
        public List<Action> actions;

        public Button() {
            actions = new ArrayList<Action>(0);
        }

        public Button(JSONObject data) {
            if (data == null) {
                return;
            }

            label = data.optString("label");
            JSONArray actions = data.optJSONArray("actions");
            int actionCount = actions != null ? actions.length() : 0;
            this.actions = new ArrayList<Action>(actionCount);
            for (int i = 0 ; i < actionCount ; ++i) {
                JSONObject action = actions.optJSONObject(i);
                this.actions.add(new Action(action));
            }
        }

        static class Action {

            public enum Type {
                CLOSE("close"),
                TRACK_EVENT("trackEvent"),
                METHOD("method"),
                LINK("link"),
                RATING("rating"),
                MAP_OPEN("mapOpen"),
                ;

                private String type;

                private Type(String type) {
                    this.type = type;
                }

                @Override
                public String toString() {
                    return type;
                }

                public static Type fromString(String type) {
                    if (type == null) {
                        throw new NullPointerException();
                    }
                    for (Type actionType : Type.values()) {
                        if (type.equals(actionType.toString())) {
                            return actionType;
                        }
                    }
                    throw new IllegalArgumentException("Constant \"" + type + "\" is not a known " + Type.class.getSimpleName());
                }
            }

            private Type type;
            private String tag;
            private String url;
            private JSONObject event; // has "type" and optionally "custom" keys
            private String method;
            private String methodArg;

            public Action() {
            }

            public Action(JSONObject data) {
                if (data == null) {
                    return;
                }

                try {
                    type = Type.fromString(data.optString("type", null));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown button action", e);
                    type = null;
                }
                tag = data.optString("tag", null);
                url = data.optString("url", null);
                event = data.optJSONObject("event");
                method = data.optString("method", null);
                methodArg = data.optString("methodArg", null);
            }

            public Type getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type;
            }

            public String getTag() {
                return tag;
            }

            public void setTag(String tag) {
                this.tag = tag;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            protected JSONObject getEvent() {
                return event;
            }

            protected void setEvent(JSONObject event) {
                this.event = event;
            }

            protected String getMethod() {
                return method;
            }

            protected void setMethod(String method) {
                this.method = method;
            }

            protected String getMethodArg() {
                return methodArg;
            }

            protected void setMethodArg(String methodArg) {
                this.methodArg = methodArg;
            }

        }
    }

}
