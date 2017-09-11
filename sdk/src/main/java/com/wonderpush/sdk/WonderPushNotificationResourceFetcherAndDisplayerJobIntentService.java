package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

public class WonderPushNotificationResourceFetcherAndDisplayerJobIntentService extends JobIntentService {

    private static final String TAG = WonderPush.TAG;

    static final int JOB_ID = 0x64C2EAE9; // CRC32("WonderPushNotificationResourceFetcherAndDisplayerJobIntentService")
    static final long TIMEOUT_MS = 30 * 1000;

    public static void enqueueWork(Context context, Work work) {
        Intent intent = new Intent();
        intent.putExtra("work", work);
        enqueueWork(context, WonderPushNotificationResourceFetcherAndDisplayerJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            Work work = intent.getParcelableExtra("work");
            NotificationManager.fetchResourcesAndDisplay(this, work, TIMEOUT_MS);
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling intent " + intent, ex);
        }
    }

    @Override
    public boolean onStopCurrentWork() {
        return false; // do not reschedule, a degraded notification has been shown
    }

    static class Work implements Parcelable {

        private final NotificationModel notif;
        private final int iconResource;
        private final String tag;
        private final int localNotificationId;
        private final Intent pushIntent;
        private final Class<? extends Activity> activity;

        public static final Creator<Work> CREATOR = new Creator<Work>() {
            @Override
            public Work createFromParcel(Parcel in) {
                return new Work(in);
            }

            @Override
            public Work[] newArray(int size) {
                return new Work[size];
            }
        };

        Work(NotificationModel notif, int iconResource, String tag, int localNotificationId, Intent pushIntent, Class<? extends Activity> activity) {
            this.notif = notif;
            this.iconResource = iconResource;
            this.tag = tag;
            this.localNotificationId = localNotificationId;
            this.pushIntent = pushIntent;
            this.activity = activity;
        }

        protected Work(Parcel in) {
            notif = in.readParcelable(getClass().getClassLoader());
            iconResource = in.readInt();
            tag = in.readString();
            localNotificationId = in.readInt();
            pushIntent = in.readParcelable(getClass().getClassLoader());

            String activityClassName = in.readString();
            Class<? extends Activity> activity = null;
            try {
                activity = getClass().getClassLoader().loadClass(activityClassName).asSubclass(Activity.class);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not unparcel activity class from " + activityClassName, e);
            }
            this.activity = activity;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(notif, 0);
            parcel.writeInt(iconResource);
            parcel.writeString(tag);
            parcel.writeInt(localNotificationId);
            parcel.writeParcelable(pushIntent, 0);
            parcel.writeString(activity.getCanonicalName());
        }

        public NotificationModel getNotif() {
            return notif;
        }

        public int getIconResource() {
            return iconResource;
        }

        public String getTag() {
            return tag;
        }

        public int getLocalNotificationId() {
            return localNotificationId;
        }

        public Intent getPushIntent() {
            return pushIntent;
        }

        public Class<? extends Activity> getActivity() {
            return activity;
        }

        public NotificationManager.PendingIntentBuilder getPendingIntentBuilder(Context context) {
            return new NotificationManager.PendingIntentBuilder(notif, localNotificationId, pushIntent, context, activity);
        }

    }

}
