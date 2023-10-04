package com.wonderpush.sdk.inappmessaging.display.internal;

import android.app.Activity;
import com.squareup.picasso3.Callback;
import com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers.BindingWrapper;

public class MediaLoader {

    final private IamImageLoader imageLoader;

    public MediaLoader(
            IamImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void loadImage(
            Activity activity,
            BindingWrapper iam,
            String imageUrl,
            Runnable onSuccess,
            Consumer<Throwable> onError) {
        if (imageUrl != null)  {
            imageLoader
                    .load(imageUrl)
                    .tag(activity.getClass())
                    .into(iam.getImageView(), new Callback() {
                        @Override
                        public void onSuccess() {
                            if (onSuccess != null) onSuccess.run();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (onError != null) onError.accept(e);
                        }
                    });
        } else if (onSuccess != null) {
            onSuccess.run();
        }
    }

}
