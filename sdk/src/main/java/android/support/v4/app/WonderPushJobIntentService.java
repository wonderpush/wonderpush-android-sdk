package android.support.v4.app;

import android.util.Log;

// See: https://issuetracker.google.com/issues/63622293
public abstract class WonderPushJobIntentService extends JobIntentService {

    private static final String TAG = "WPJobIntentService";

    @Override
    GenericWorkItem dequeueWork() {
        try {
            return super.dequeueWork();
        } catch (Exception ex) {
            // Log and mask any error (a SecurityException most probably)
            Log.e(TAG, "Unexpected error while in dequeueWork", ex);
        }
        return null;
    }

}
