package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

class NotificationManager {

    static final String TAG = WonderPush.TAG;

    private static WeakReference<Intent> sLastHandledIntentRef;

    public static boolean showPotentialNotification(final Activity activity, Intent intent) {
        if (containsExplicitNotification(intent) || containsWillOpenNotificationAutomaticallyOpenable(intent)) {
            final NotificationModel notif = NotificationModel.fromLocalIntent(intent);
            if (notif == null) {
                Log.e(TAG, "Failed to extract notification object");
                return false;
            }
            if (containsExplicitNotification(intent)) {
                intent.setDataAndType(null, null);
            } else if (containsWillOpenNotification(intent)) {
                // Keep it
                //intent.removeExtra(INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION);
            }
            sLastHandledIntentRef = new WeakReference<>(intent);
            WonderPush.logDebug("Handling opened notification: " + notif.getInputJSONString());
            try {
                JSONObject trackData = new JSONObject();
                trackData.put("campaignId", notif.getCampaignId());
                trackData.put("notificationId", notif.getNotificationId());
                trackData.put("actionDate", WonderPush.getTime());
                WonderPush.trackInternalEvent("@NOTIFICATION_OPENED", trackData);

                WonderPushConfiguration.setLastOpenedNotificationInfoJson(trackData);

                // Notify the application that the notification has been opened
                Intent notificationOpenedIntent = new Intent(WonderPush.INTENT_NOTIFICATION_OPENED);
                boolean fromUserInteraction = intent.getBooleanExtra("fromUserInteraction", true);
                notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION, fromUserInteraction);
                Intent receivedPushNotificationIntent = intent.getParcelableExtra("receivedPushNotificationIntent");
                notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION, receivedPushNotificationIntent);
                LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(notificationOpenedIntent);

                if (WonderPush.isInitialized()) {
                    handleReceivedNotification(activity, notif);
                } else {
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            try {
                                handleReceivedNotification(context, notif);
                            } catch (Exception ex) {
                                Log.e(TAG, "Unexpected error on deferred handling of received notification", ex);
                            }
                            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                        }
                    };

                    IntentFilter filter = new IntentFilter(WonderPush.INTENT_INTIALIZED);
                    LocalBroadcastManager.getInstance(activity).registerReceiver(receiver, filter);
                }

                return true;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse notification JSON object", e);
            }

        }
        return false;
    }

    protected static boolean containsExplicitNotification(Intent intent) {
        return  intent != null
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && WonderPush.INTENT_NOTIFICATION_TYPE.equals(intent.getType())
                && intent.getData() != null
                && WonderPush.INTENT_NOTIFICATION_SCHEME.equals(intent.getData().getScheme())
                && WonderPush.INTENT_NOTIFICATION_AUTHORITY.equals(intent.getData().getAuthority())
                ;
    }

    protected static boolean containsWillOpenNotification(Intent intent) {
        return  intent != null
                // action may or may not be INTENT_NOTIFICATION_WILL_OPEN
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && intent.hasExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION)
                ;
    }

    protected static boolean containsWillOpenNotificationAutomaticallyOpenable(Intent intent) {
        return  containsWillOpenNotification(intent)
                && intent.hasExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN) // makes it default to false if removed
                && intent.getBooleanExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN, false)
                ;
    }

    private static void handleReceivedNotification(Context context, NotificationModel notif) {
        try {
            for (ActionModel action : notif.getActions()) {
                try {
                    if (action == null || action.getType() == null) {
                        // Skip unrecognized action types
                        continue;
                    }
                    switch (action.getType()) {
                        case CLOSE:
                            // Noop
                            break;
                        case LINK:
                            handleLinkAction(context, action);
                            break;
                        case RATING:
                            handleRatingAction(context, action);
                            break;
                        case TRACK_EVENT:
                            handleTrackEventAction(action);
                            break;
                        case UPDATE_INSTALLATION:
                            handleUpdateInstallationAction(action);
                            break;
                        case METHOD:
                            handleMethodAction(action);
                            break;
                        default:
                            Log.w(TAG, "Unhandled opened notification action \"" + action.getType() + "\"");
                            break;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while handling opened notification action " + action, ex);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling opened notification actions", ex);
        }

        try {
            switch (notif.getType()) {
                case SIMPLE:
                case DATA:
                    // Nothing to do
                    break;
                case URL:
                    handleURLNotification(context, (NotificationUrlModel) notif);
                    break;
                case TEXT:
                    handleTextNotification(context, (NotificationTextModel) notif);
                    break;
                case MAP:
                    handleMapNotification(context, (NotificationMapModel) notif);
                    break;
                case HTML:
                    handleHTMLNotification(context, (NotificationHtmlModel) notif);
                    break;
                default:
                    Log.e(TAG, "Missing built-in action for type " + notif.getType());
                    break;
            }
        } catch (ClassCastException ex) {
            Log.e(TAG, "Wrong notification class for type " + notif.getType(), ex);
        }
    }

    protected static WonderPushDialogBuilder createDialogNotificationBase(final Context context, final NotificationModel notif) {
        WonderPushDialogBuilder builder = new WonderPushDialogBuilder(context, notif, new WonderPushDialogBuilder.OnChoice() {
            @Override
            public void onChoice(WonderPushDialogBuilder dialog, ButtonModel which) {
                handleDialogButtonAction(dialog, which);
            }
        });
        builder.setupTitleAndIcon();

        return builder;
    }

    protected static void createDefaultCloseButtonIfNeeded(WonderPushDialogBuilder builder) {
        if (builder.getNotificationModel().getButtonCount() == 0) {
            ButtonModel defaultButton = new ButtonModel();
            defaultButton.label = builder.getContext().getResources().getString(R.string.wonderpush_close);
            builder.getNotificationModel().addButton(defaultButton);
        }
    }

    protected static void handleDialogButtonAction(WonderPushDialogBuilder dialog, ButtonModel buttonClicked) {
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
            for (ActionModel action : buttonClicked.actions) {
                try {
                    if (action == null || action.getType() == null) {
                        // Skip unrecognized action types
                        continue;
                    }
                    switch (action.getType()) {
                        case CLOSE:
                            // Noop
                            break;
                        case MAP_OPEN:
                            handleMapOpenAction(context, dialog.getNotificationModel(), action);
                            break;
                        case LINK:
                            handleLinkAction(context, action);
                            break;
                        case RATING:
                            handleRatingAction(context, action);
                            break;
                        case TRACK_EVENT:
                            handleTrackEventAction(action);
                            break;
                        case UPDATE_INSTALLATION:
                            handleUpdateInstallationAction(action);
                            break;
                        case METHOD:
                            handleMethodAction(action);
                            break;
                        default:
                            Log.w(TAG, "Unhandled button action \"" + action.getType() + "\"");
                            break;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while handling button action " + action, ex);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling button actions", ex);
        }
    }

    protected static void handleTextNotification(final Context context, final NotificationTextModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        if (notif.getMessage() == null) {
            Log.w(TAG, "Got no message to display for a plain notification");
        } else {
            builder.setMessage(notif.getMessage());
        }

        createDefaultCloseButtonIfNeeded(builder);
        builder.setupButtons();

        builder.show();
    }

    @SuppressLint("InflateParams")
    protected static void handleMapNotification(final Context context, final NotificationMapModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        final NotificationMapModel.Map map = notif.getMap();
        if (map == null) {
            Log.e(TAG, "Could not get the map from the notification");
            return;
        }
        final NotificationMapModel.Place place = map.getPlace();
        if (place == null) {
            Log.e(TAG, "Could not get the place from the map");
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
                        Log.e(TAG, "No location for map");
                        return null;
                    }
                    int screenWidth = InstallationManager.getScreenWidth(context);
                    int screenHeight = InstallationManager.getScreenHeight(context);
                    double ratio = screenWidth / (double)screenHeight;
                    int width = ratio >= 1 ? Math.min(640, screenWidth) : (int)Math.floor(ratio * Math.min(640, screenHeight));
                    int height = ratio <= 1 ? Math.min(640, screenHeight) : (int)Math.floor(Math.min(640, screenWidth) / ratio);
                    String size = width + "x" + height;
                    int scale = InstallationManager.getScreenDensity(context) >= 192 ? 2 : 1;
                    URL url = new URL("https://maps.google.com/maps/api/staticmap"
                            + "?center=" + loc
                            + "&zoom=" + (place.getZoom() != null ? place.getZoom() : 13)
                            + "&size=" + size
                            + "&sensors=false"
                            + "&markers=color:red%7C" + loc
                            + "&scale=" + scale
                            + "&language=" + WonderPush.getLang()
                    );
                    return BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Malformed map URL", e);
                } catch (IOException e) {
                    Log.e(TAG, "Could not load map image", e);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error while loading map image", e);
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

    protected static void handleMapOpenAction(Context context, NotificationModel notif, ActionModel action) {
        try {
            NotificationMapModel.Place place;
            try {
                place = ((NotificationMapModel) notif).getMap().getPlace();
            } catch (Exception e) {
                Log.e(TAG, "Could not get the place from the map", e);
                return;
            }
            NotificationMapModel.Point point = place.getPoint();

            Uri.Builder geo = new Uri.Builder();
            geo.scheme("geo");
            if (point != null) {
                if (place.getName() != null) {
                    geo.authority("0,0");
                    geo.appendQueryParameter("q", point.getLat() + "," + point.getLon() + "(" + place.getName() + ")");
                } else {
                    geo.authority(point.getLat() + "," + point.getLon());
                    if (place.getZoom() != null) {
                        geo.appendQueryParameter("z", place.getZoom().toString());
                    }
                }
            } else if (place.getQuery() != null) {
                geo.authority("0,0");
                geo.appendQueryParameter("q", place.getQuery());
            }
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(geo.build());
            if (open.resolveActivity(context.getPackageManager()) != null) {
                WonderPush.logDebug("Will open location " + open.getDataString());
                context.startActivity(open);
            } else {
                WonderPush.logDebug("No activity can open location " + open.getDataString());
                WonderPush.logDebug("Falling back to regular URL");
                geo = new Uri.Builder();
                geo.scheme("http");
                geo.authority("maps.google.com");
                geo.path("maps");
                if (point != null) {
                    geo.appendQueryParameter("q", point.getLat() + "," + point.getLon());
                    if (place.getZoom() != null) {
                        geo.appendQueryParameter("z", place.getZoom().toString());
                    }
                } else if (place.getQuery() != null) {
                    geo.appendQueryParameter("q", place.getQuery());
                } else if (place.getName() != null) {
                    geo.appendQueryParameter("q", place.getName());
                }
                open = new Intent(Intent.ACTION_VIEW);
                open.setData(geo.build());
                if (open.resolveActivity(context.getPackageManager()) != null) {
                    WonderPush.logDebug("Opening URL " + open.getDataString());
                    context.startActivity(open);
                } else {
                    WonderPush.logDebug("No activity can open URL " + open.getDataString());
                    Log.w(TAG, "Cannot open map!");
                    Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while opening map", e);
            Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
        }
    }

    protected static WonderPushDialogBuilder createWebNotificationBasePre(final Context context, final NotificationModel notif, WonderPushView webView) {
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

    protected static void handleHTMLNotification(final Context context, final NotificationHtmlModel notif) {
        if (notif.getMessage() == null) {
            Log.w(TAG, "No HTML content to display in the notification!");
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

    protected static void handleURLNotification(Context context, NotificationUrlModel notif) {
        if (notif.getUrl() == null) {
            Log.e(TAG, "No URL to display in the notification!");
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

    protected static void handleLinkAction(Context context, ActionModel action) {
        try {
            String url = action.getUrl();
            if (url == null) {
                Log.e(TAG, "No url in a " + ActionModel.Type.LINK + " action!");
                return;
            }
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "No service for intent " + intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.LINK + " action", e);
        }
    }

    protected static void handleRatingAction(Context context, ActionModel action) {
        try {
            Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "No service for intent " + intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.RATING + " action", e);
        }
    }

    protected static void handleTrackEventAction(ActionModel action) {
        JSONObject event = action.getEvent();
        if (event == null) {
            Log.e(TAG, "Got no event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        if (!event.has("type") || event.optString("type", null) == null) {
            Log.e(TAG, "Got no type in the event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        WonderPush.trackEvent(event.optString("type", null), event.optJSONObject("custom"));
    }

    protected static void handleUpdateInstallationAction(ActionModel action) {
        JSONObject custom = action.getCustom();
        if (custom == null) {
            Log.e(TAG, "Got no installation custom properties to update for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        if (custom.length() == 0) {
            WonderPush.logDebug("Empty installation custom properties for an update, for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        InstallationManager.putInstallationCustomProperties(custom);
    }

    protected static void handleMethodAction(ActionModel action) {
        String method = action.getMethod();
        String arg = action.getMethodArg();
        if (method == null) {
            Log.e(TAG, "Got no method to call for a " + ActionModel.Type.METHOD + " action");
            return;
        }
        Intent intent = new Intent();
        intent.setPackage(WonderPush.getApplicationContext().getPackageName());
        intent.setAction(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_ACTION);
        intent.setData(new Uri.Builder()
                .scheme(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_SCHEME)
                .authority(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_AUTHORITY)
                .appendPath(method)
                .build());
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_METHOD, method);
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_ARG, arg);
        LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(intent);
    }

}
