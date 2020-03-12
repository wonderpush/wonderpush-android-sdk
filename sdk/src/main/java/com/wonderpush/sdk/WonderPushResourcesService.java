package com.wonderpush.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.app.WonderPushJobIntentService;
import android.util.Log;

/**
 * Utility class to fetch images and update notifications accordingly.
 */
public class WonderPushResourcesService extends WonderPushJobIntentService {

    private static final String TAG = WonderPush.TAG;

    static final int JOB_ID = 0x64C2EAE9; // CRC32("WonderPushNotificationResourceFetcherAndDisplayerJobIntentService")
    static final long TIMEOUT_MS = 30 * 1000;

    public static void enqueueWork(Context context, Work work) {
        Intent intent = new Intent();
        intent.putExtra("work", work);
        enqueueWork(context, WonderPushResourcesService.class, JOB_ID, intent);
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
        private final String tag;
        private final int localNotificationId;
        private final Intent pushIntent;

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

        Work(NotificationModel notif, String tag, int localNotificationId, Intent pushIntent) {
            this.notif = notif;
            this.tag = tag;
            this.localNotificationId = localNotificationId;
            this.pushIntent = pushIntent;
        }

        protected Work(Parcel in) {
            notif = in.readParcelable(getClass().getClassLoader());
            tag = in.readString();
            localNotificationId = in.readInt();
            pushIntent = in.readParcelable(getClass().getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(notif, 0);
            parcel.writeString(tag);
            parcel.writeInt(localNotificationId);
            parcel.writeParcelable(pushIntent, 0);
        }

        public NotificationModel getNotif() {
            return notif;
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

        public NotificationManager.PendingIntentBuilder getPendingIntentBuilder(Context context) {
            return new NotificationManager.PendingIntentBuilder(notif, localNotificationId, pushIntent, context);
        }

    }

}
