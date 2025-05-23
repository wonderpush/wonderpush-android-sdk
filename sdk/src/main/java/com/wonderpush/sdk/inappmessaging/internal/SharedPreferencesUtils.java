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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.wonderpush.sdk.WonderPushCompatibilityHelper;

import javax.inject.Inject;

/** @hide */
public class SharedPreferencesUtils {

  //@VisibleForTesting
  static final String PREFERENCES_PACKAGE_NAME = "com.wonderpush.sdk.inappmessaging";

  private Application application;

  @Inject
  public SharedPreferencesUtils(Application application) {
    this.application = application;
  }

  /**
   * Helper method for setting a boolean value in the apps stored preferences.
   *
   * @param preference the preference key.
   * @param value the value to store.
   */
  public void setBooleanPreference(String preference, boolean value) {
    SharedPreferences.Editor preferencesEditor =
        application.getSharedPreferences(PREFERENCES_PACKAGE_NAME, Context.MODE_PRIVATE).edit();
    preferencesEditor.putBoolean(preference, value);
    preferencesEditor.apply();
  }

  /**
   * Helper method for getting a boolean value from the apps stored preferences.
   *
   * @param preference the preference key.
   * @param defaultValue the default value to return if the key is not found.
   * @return the value stored or the default if the stored value is not found.
   */
  public boolean getAndSetBooleanPreference(String preference, boolean defaultValue) {
    SharedPreferences preferences =
        application.getSharedPreferences(PREFERENCES_PACKAGE_NAME, Context.MODE_PRIVATE);

    // Value set at runtime overrides anything else, but default to defaultValue.
    if (preferences.contains(preference)) {
      boolean result = preferences.getBoolean(preference, defaultValue);
      return result;
    }
    // No preferences set yet - use and set defaultValue.
    setBooleanPreference(preference, defaultValue);
    return defaultValue;
  }

  /**
   * Helper method for getting a boolean value from the apps stored preferences.
   *
   * @param preference the preference key.
   * @param defaultValue the default value to return if the key is not found.
   * @return the value stored or the default if the stored value is not found.
   */
  public boolean getBooleanPreference(String preference, boolean defaultValue) {
    SharedPreferences preferences =
        application.getSharedPreferences(PREFERENCES_PACKAGE_NAME, Context.MODE_PRIVATE);

    // Value set at runtime overrides anything else, but default to defaultValue.
    if (preferences.contains(preference)) {
      boolean result = preferences.getBoolean(preference, defaultValue);
      return result;
    }
    // No preferences set yet - use  defaultValue.
    return defaultValue;
  }

  /**
   * Helper method for getting a boolean value from the apps stored preferences.
   *
   * @param preference the preference key.
   * @return whether the preference has been set or not
   */
  public boolean isPreferenceSet(String preference) {
    SharedPreferences preferences =
        application.getSharedPreferences(PREFERENCES_PACKAGE_NAME, Context.MODE_PRIVATE);

    return preferences.contains(preference);
  }

  /**
   * Helper method for getting a boolean value from the apps manifest
   *
   * @param preference the preference key.
   * @return whether the preference has been set or not
   */
  public boolean isManifestSet(String preference) {
    // Check if there's metadata in the manifest setting the auto-init state.
    try {
      PackageManager packageManager = application.getPackageManager();
      if (packageManager != null) {
        Bundle applicationInfoMetaData = WonderPushCompatibilityHelper.getApplicationInfoMetaData(application);
        return applicationInfoMetaData != null
            && applicationInfoMetaData.containsKey(preference);
      }
    } catch (PackageManager.NameNotFoundException e) {
      // This shouldn't happen since it's this app's package. However, if it does, we want to fall
      // through to the default, and avoid throwing an exception

    }
    return false;
  }

  /**
   * Helper method for getting a boolean value from the apps stored preferences. Falls back to
   * checking for a manifest preference before returning the default value.
   *
   * @param preference the manifest preference key.
   * @param defaultValue the default value to return if the key is not found.
   * @return the value stored or the default if the stored value is not found.
   */
  public boolean getBooleanManifestValue(String preference, boolean defaultValue) {
    // Check if there's metadata in the manifest setting the auto-init state.
    try {
      PackageManager packageManager = application.getPackageManager();
      if (packageManager != null) {
        Bundle applicationInfoMetaData = WonderPushCompatibilityHelper.getApplicationInfoMetaData(application);
        if (applicationInfoMetaData != null
            && applicationInfoMetaData.containsKey(preference)) {
          return applicationInfoMetaData.getBoolean(preference);
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      // This shouldn't happen since it's this app's package. However, if it does, we want to fall
      // through to the default, and avoid throwing an exception
    }

    // Return the default
    return defaultValue;
  }
}
