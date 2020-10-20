package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

class InAppManager {

    private final static String TAG = WonderPush.TAG;

    protected static void handleInApp(Context context, NotificationModel notif) {
        if (notif.getInAppMessage() != null && notif.getType() != NotificationModel.Type.DATA) {
            InAppMessaging.getInstance().displayInAppMessage(notif.getInAppMessage());
            return;
        }
        try {
            switch (notif.getType()) {
                case SIMPLE:
                case DATA:
                    // Nothing to do
                    break;
                case URL:
                    InAppManager.handleURLNotification(context, (NotificationUrlModel) notif);
                    break;
                case TEXT:
                    InAppManager.handleTextNotification(context, (NotificationTextModel) notif);
                    break;
                case MAP:
                    InAppManager.handleMapNotification(context, (NotificationMapModel) notif);
                    break;
                case HTML:
                    InAppManager.handleHTMLNotification(context, (NotificationHtmlModel) notif);
                    break;
                default:
                    Log.e(TAG, "Missing built-in in-app for type " + notif.getType());
                    break;
            }
        } catch (ClassCastException ex) {
            Log.e(TAG, "Wrong in-app class for type " + notif.getType(), ex);
        }
    }

    private static WonderPushDialogBuilder createDialogNotificationBase(final Context context, final NotificationModel notif) {
        WonderPushDialogBuilder builder = new WonderPushDialogBuilder(context, notif, new WonderPushDialogBuilder.OnChoice() {
            @Override
            public void onChoice(WonderPushDialogBuilder dialog, ButtonModel which) {
                handleDialogButtonAction(dialog, which);
            }
        });
        builder.setupTitleAndIcon();

        return builder;
    }

    private static void createDefaultCloseButtonIfNeeded(WonderPushDialogBuilder builder) {
        if (builder.getNotificationModel().getButtonCount() == 0) {
            ButtonModel defaultButton = new ButtonModel();
            defaultButton.label = builder.getContext().getResources().getString(R.string.wonderpush_close);
            builder.getNotificationModel().addButton(defaultButton);
        }
    }

    private static void handleDialogButtonAction(WonderPushDialogBuilder dialog, ButtonModel buttonClicked) {
        JSONObject eventData = new JSONObject();
        try {
            eventData.put("buttonLabel", buttonClicked == null ? null : buttonClicked.label);
            eventData.put("reactionTime", dialog.getShownDuration());
            eventData.putOpt("custom", dialog.getInteractionEventCustom());
            eventData.put("campaignId", dialog.getNotificationModel().getCampaignId());
            eventData.put("notificationId", dialog.getNotificationModel().getNotificationId());
        } catch (JSONException e) {
            WonderPush.logError("Failed to fill the @NOTIFICATION_ACTION event", e);
        }
        WonderPush.trackInternalEvent("@NOTIFICATION_ACTION", eventData);

        if (buttonClicked == null) {
            WonderPush.logDebug("User cancelled the dialog");
            return;
        }
        Context context = dialog.getContext();
        try {
            NotificationManager.handleActions(context, new NotificationMetadata(dialog.getNotificationModel()), buttonClicked.actions);
        } catch (Exception ex) {
            Log.e(NotificationManager.TAG, "Unexpected error while handling button actions", ex);
        }
    }

    private static void handleTextNotification(final Context context, final NotificationTextModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        if (notif.getMessage() == null) {
            Log.w(NotificationManager.TAG, "Got no message to display for a plain notification");
        } else {
            builder.setMessage(notif.getMessage());
        }

        createDefaultCloseButtonIfNeeded(builder);
        builder.setupButtons();

        builder.show();
    }

    @SuppressLint("InflateParams")
    private static void handleMapNotification(final Context context, final NotificationMapModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        final NotificationMapModel.Map map = notif.getMap();
        if (map == null) {
            Log.e(NotificationManager.TAG, "Could not get the map from the notification");
            return;
        }
        final NotificationMapModel.Place place = map.getPlace();
        if (place == null) {
            Log.e(NotificationManager.TAG, "Could not get the place from the map");
            return;
        }
        final NotificationMapModel.Point point = place.getPoint();

        final View dialogView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.wonderpush_notification_map_dialog, null, false);
        final TextView text = (TextView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_text);
        if (notif.getMessage() != null) {
            text.setVisibility(View.VISIBLE);
            text.setText(notif.getMessage());
            text.setMovementMethod(new ScrollingMovementMethod());
        }
        final ImageView mapImg = (ImageView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_map);
        builder.setView(dialogView);

        if (notif.getButtonCount() == 0) {
            // Close button
            ButtonModel closeButton = new ButtonModel();
            closeButton.label = context.getResources().getString(R.string.wonderpush_close);
            ActionModel closeAction = new ActionModel();
            closeAction.setType(ActionModel.Type.CLOSE);
            closeButton.actions.add(closeAction);
            notif.addButton(closeButton);
            // Open button
            ButtonModel openButton = new ButtonModel();
            openButton.label = context.getResources().getString(R.string.wonderpush_open);
            ActionModel openAction = new ActionModel();
            openAction.setType(ActionModel.Type.MAP_OPEN);
            openAction.setMap(map);
            openButton.actions.add(openAction);
            notif.addButton(openButton);
        }
        builder.setupButtons();

        builder.show();

        new AsyncTask<Object, Object, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... args) {
                try {
                    String loc;
                    if (point != null) {
                        loc = point.getLat() + "," + point.getLon();
                    } else if (place.getName() != null) {
                        loc = place.getName();
                    } else {
                        loc = place.getQuery();
                    }
                    if (loc == null) {
                        Log.e(NotificationManager.TAG, "No location for map");
                        return null;
                    }
                    int screenWidth = InstallationManager.getScreenWidth(context);
                    int screenHeight = InstallationManager.getScreenHeight(context);
                    double ratio = screenWidth / (double)screenHeight;
                    int width = ratio >= 1 ? Math.min(640, screenWidth) : (int)Math.floor(ratio * Math.min(640, screenHeight));
                    int height = ratio <= 1 ? Math.min(640, screenHeight) : (int)Math.floor(Math.min(640, screenWidth) / ratio);
                    String size = width + "x" + height;
                    int scale = InstallationManager.getScreenDensity(context) >= 192 ? 2 : 1;
                    String locale = WonderPush.getLocale();
                    if (locale == null) locale = "en";
                    URL url = new URL("https://maps.google.com/maps/api/staticmap"
                            + "?center=" + loc
                            + "&zoom=" + (place.getZoom() != null ? place.getZoom() : 13)
                            + "&size=" + size
                            + "&sensors=false"
                            + "&markers=color:red%7C" + loc
                            + "&scale=" + scale
                            + "&language=" + locale
                    );
                    return BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (MalformedURLException e) {
                    Log.e(NotificationManager.TAG, "Malformed map URL", e);
                } catch (IOException e) {
                    Log.e(NotificationManager.TAG, "Could not load map image", e);
                } catch (Exception e) {
                    Log.e(NotificationManager.TAG, "Unexpected error while loading map image", e);
                }
                return null;
            }
            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp == null) {
                    mapImg.setVisibility(View.GONE);
                    text.setMaxLines(Integer.MAX_VALUE);
                } else {
                    mapImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    mapImg.setImageBitmap(bmp);
                }
            }
        }.execute();
    }

    private static WonderPushDialogBuilder createWebNotificationBasePre(final Context context, final NotificationModel notif, WonderPushView webView) {
        final WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);
        builder.setupButtons();

        webView.setShowCloseButton(notif.getButtonCount() < 1);

        webView.setStateListener(new WonderPushView.OnStateListener() {
            @Override
            public void onLoading() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onLoaded() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onError() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onClose() {
                builder.dismiss();
            }
        });

        builder.setView(webView);

        return builder;
    }

    private static void handleHTMLNotification(final Context context, final NotificationHtmlModel notif) {
        if (notif.getMessage() == null) {
            Log.w(NotificationManager.TAG, "No HTML content to display in the notification!");
            return;
        }

        // Build the dialog
        WonderPushView webView = new WonderPushView(context);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(context, notif, webView);
        builder.setupButtons();

        // Set content
        webView.loadDataWithBaseURL(notif.getBaseUrl(), notif.getMessage(), "text/html", "utf-8", null);

        // Show time!
        builder.show();
    }

    private static void handleURLNotification(Context context, NotificationUrlModel notif) {
        if (notif.getUrl() == null) {
            Log.e(NotificationManager.TAG, "No URL to display in the notification!");
            return;
        }

        WonderPushView webView = new WonderPushView(context);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(context, notif, webView);
        builder.setupButtons();

        // Set content
        webView.setFullUrl(notif.getUrl());

        // Show time!
        builder.show();
    }

}
