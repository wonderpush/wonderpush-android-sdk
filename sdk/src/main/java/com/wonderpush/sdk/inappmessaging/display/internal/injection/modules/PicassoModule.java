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

package com.wonderpush.sdk.inappmessaging.display.internal.injection.modules;

import android.app.Application;
import android.net.TrafficStats;
import android.os.Process;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.wonderpush.sdk.inappmessaging.display.internal.PicassoErrorListener;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import dagger.Module;
import dagger.Provides;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/** @hide */
@Module
public class PicassoModule {
  @Provides
  @InAppMessagingScope
  Picasso providesIamController(
      Application application, PicassoErrorListener picassoErrorListener) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(
                new Interceptor() {
                  @Override
                  public Response intercept(Chain chain) throws IOException {
                    return chain.proceed(
                        chain.request().newBuilder().addHeader("Accept", "image/*").build());
                  }
                }
            )
            .eventListener(new EventListener() {
                @Override
                public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
                    TrafficStats.setThreadStatsTag(Process.myTid());
                }
            })
            .build();

    Picasso.Builder builder = new Picasso.Builder(application);
    builder.listener(picassoErrorListener).downloader(new OkHttp3Downloader(client));
    return builder.build();
  }
}
