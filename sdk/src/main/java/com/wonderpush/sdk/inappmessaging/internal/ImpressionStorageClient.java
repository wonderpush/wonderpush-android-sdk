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

package com.wonderpush.sdk.inappmessaging.internal;

import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.ImpressionStore;
import com.wonderpush.sdk.inappmessaging.model.Campaign;
import com.wonderpush.sdk.inappmessaging.model.CampaignImpression;
import com.wonderpush.sdk.inappmessaging.model.CampaignImpressionList;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Class to store and retrieve in app message impressions
 *
 * @hide
 */
@Singleton
public class ImpressionStorageClient {
  private final ProtoStorageClient storageClient;
  private Maybe<CampaignImpressionList> cachedImpressionsMaybe = Maybe.empty();

  @Inject
  ImpressionStorageClient(@ImpressionStore ProtoStorageClient storageClient) {
    this.storageClient = storageClient;
  }

  private static CampaignImpressionList appendImpression(
      CampaignImpressionList campaignImpressions, CampaignImpression impression) {
    CampaignImpressionList rtn = new CampaignImpressionList(campaignImpressions);
    rtn.addAlreadySeenCampaigns(impression);
    return rtn;
  }

  /** Stores the provided {@link CampaignImpression} to file storage */
  public Completable storeImpression(CampaignImpression impression) {
    return getAllImpressions()
        .defaultIfEmpty(new CampaignImpressionList())
        .flatMapCompletable(
            storedImpressions -> {
              CampaignImpressionList appendedImpressions =
                  appendImpression(storedImpressions, impression);
              return storageClient
                  .write(appendedImpressions)
                  .doOnComplete(() -> initInMemCache(appendedImpressions));
            });
  }

  /**
   * Returns the list of impressed campaigns
   *
   * <p>Returns {@link Maybe#empty()} if no campaigns have ever been impressed or if the storage was
   * corrupt.
   */
  public Maybe<CampaignImpressionList> getAllImpressions() {
    return cachedImpressionsMaybe
        .switchIfEmpty(
            storageClient.read(CampaignImpressionList.class).doOnSuccess(this::initInMemCache))
        .doOnError(ignored -> clearInMemCache());
  }

  private void initInMemCache(CampaignImpressionList campaignImpressions) {
    cachedImpressionsMaybe = Maybe.just(campaignImpressions);
  }

  private void clearInMemCache() {
    cachedImpressionsMaybe = Maybe.empty();
  }

  /** Returns {@code Single.just(true)} if the campaign has been impressed */
  public Single<Boolean> isImpressed(Campaign.ThickContent content) {
    String campaignId = content.getPayload().getCampaignId();
    return getAllImpressions()
        .map(CampaignImpressionList::getAlreadySeenCampaignsList)
        .flatMapObservable(Observable::fromIterable)
        .map(CampaignImpression::getCampaignId)
        .contains(campaignId);
  }
}
