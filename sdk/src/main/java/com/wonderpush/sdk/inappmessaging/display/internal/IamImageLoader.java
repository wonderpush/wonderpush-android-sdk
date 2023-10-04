// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.wonderpush.sdk.inappmessaging.display.internal;

import androidx.annotation.Nullable;
import android.widget.ImageView;

import com.squareup.picasso3.Callback;
import com.squareup.picasso3.Picasso;
import com.squareup.picasso3.RequestCreator;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;

import javax.inject.Inject;

import dagger.Lazy;

/**
 * Image loader abstraction around the Picasso singleton to facilitate testing and injection
 *
 * @hide
 */
@InAppMessagingScope
public class IamImageLoader {
  private final Lazy<Picasso> picasso;

  @Inject
  IamImageLoader(Lazy<Picasso> picasso) {
    this.picasso = picasso;
  }

  public IamImageRequestCreator load(@Nullable String imageUrl) {
    return new IamImageRequestCreator(picasso.get().load(imageUrl));
  }

  public void cancelTag(Class c) {
    picasso.get().cancelTag(c);
  }

  public static class IamImageRequestCreator {
    private final RequestCreator mRequestCreator;

    public IamImageRequestCreator(RequestCreator requestCreator) {
      mRequestCreator = requestCreator;
    }

    public IamImageRequestCreator placeholder(int placeholderResId) {
      mRequestCreator.placeholder(placeholderResId);
      return this;
    }

    public IamImageRequestCreator tag(Class c) {
      mRequestCreator.tag(c);
      return this;
    }

    public void into(ImageView imageView, Callback callback) {
      mRequestCreator.into(imageView, callback);
    }
  }
}
