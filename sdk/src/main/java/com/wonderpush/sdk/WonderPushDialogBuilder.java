package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.view.View;

import org.json.JSONObject;

class WonderPushDialogBuilder {

    public interface OnChoice {
        /**
         * @param dialog
         * @param which The clicked button or null.
         */
        void onChoice(WonderPushDialogBuilder dialog, ButtonModel which);
    }

    final Context activity;
    final NotificationModel notif;
    final AlertDialog.Builder builder;
    final OnChoice listener;
    AlertDialog dialog;
    long shownAtElapsedRealtime;
    long endedAtElapsedRealtime;
    JSONObject interactionEventCustom;

    String defaultTitle;
    int defaultIcon;

    /**
     * Read styled attributes, they will defined in an AlertDialog shown by the given activity.
     * You must call {@link TypedArray#recycle()} after having read the desired attributes.
     * @see Context#obtainStyledAttributes(android.util.AttributeSet, int[], int, int)
     * @param activity
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRef
     * @return
     */
    @SuppressLint("Recycle")
    public static TypedArray getDialogStyledAttributes(Context activity, int[] attrs, int defStyleAttr, int defStyleRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        AlertDialog dialog = builder.create();
        TypedArray ta = dialog.getContext().obtainStyledAttributes(null, attrs, defStyleAttr, defStyleRef);
        dialog.dismiss();
        return ta;
    }

    public WonderPushDialogBuilder(Context activity, NotificationModel notif, OnChoice listener) {
        this.activity = activity;
        this.notif = notif;
        builder = new AlertDialog.Builder(activity);
        this.listener = listener;

        defaultTitle = this.activity.getApplicationInfo().name;
        defaultIcon  = this.activity.getApplicationInfo().icon;

        commonSetup();
    }

    private void commonSetup() {
        // onDismissListener has moved to show() for backward compatibility
    }

    public Context getContext() {
        return activity;
    }

    public NotificationModel getNotificationModel() {
        return notif;
    }

    public void setDefaultTitle(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public void setDefaultIcon(int defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    public WonderPushDialogBuilder setupTitleAndIcon() {
        if (notif.getTitle() != null) {
            builder.setTitle(notif.getTitle());
            builder.setIcon(defaultIcon);
        }
        return this;
    }

    public WonderPushDialogBuilder setupButtons() {
        if (listener == null) {
            WonderPush.logError("Calling WonderPushDialogBuilder.setupButtons() without OnChoice listener, ignoring!");
            if (notif.getButtonCount() > 0) {
                builder.setNegativeButton(R.string.wonderpush_close, null);
            }
            return this;
        }

        // Derive button genre from desired positions
        int positiveIndex = -1;
        int negativeIndex = -1;
        int neutralIndex  = -1;
        if (notif.getButtonCount() == 1) {
            positiveIndex = 0;
        } else if (notif.getButtonCount() == 2) {
            negativeIndex = 0;
            positiveIndex = 1;
        } else if (notif.getButtonCount() >= 3) {
            negativeIndex = 0;
            neutralIndex = 1;
            positiveIndex = 2;
        }

        if (negativeIndex >= 0) {
            final ButtonModel button = notif.getButton(negativeIndex);
            builder.setNegativeButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    notif.getChosenButton().set(button);
                }
            });
        }
        if (neutralIndex >= 0) {
            final ButtonModel button = notif.getButton(neutralIndex);
            builder.setNeutralButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    notif.getChosenButton().set(button);
                }
            });
        }
        if (positiveIndex >= 0) {
            final ButtonModel button = notif.getButton(positiveIndex);
            builder.setPositiveButton(button.label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    notif.getChosenButton().set(button);
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
        shownAtElapsedRealtime = SystemClock.elapsedRealtime();
        if (dialog == null) {
            dialog = builder.create();
            // On dismiss, handle chosen button, if any
            DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    endedAtElapsedRealtime = SystemClock.elapsedRealtime();
                    WonderPush.logDebug("Dialog dismissed");
                    if (listener != null) {
                        listener.onChoice(WonderPushDialogBuilder.this, notif.getChosenButton().get());
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

    public long getShownDuration() {
        return endedAtElapsedRealtime - shownAtElapsedRealtime;
    }

    protected JSONObject getInteractionEventCustom() {
        return interactionEventCustom;
    }

    protected void setInteractionEventCustom(JSONObject interactionEventCustom) {
        this.interactionEventCustom = interactionEventCustom;
    }

}
