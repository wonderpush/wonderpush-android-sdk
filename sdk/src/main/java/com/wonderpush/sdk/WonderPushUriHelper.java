package com.wonderpush.sdk;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A collection of static helpers that manipulate Uri's and resources.
 */
class WonderPushUriHelper {

    static Uri sBaseUri;

    /**
     * Extracts the resource path from a Uri.
     *
     * @param uri
     * @return The resource path for that Uri, starting with a '/' after the API
     *         version number. null if the provided Uri is not a WonderPush uri
     *         (isAPIUri returns false).
     */
    protected static String getResource(Uri uri) {
        if (!isAPIUri(uri)) {
            return null;
        }

        String scheme = uri.getScheme();
        String apiScheme = getBaseUri().getScheme();

        // Strip out the protocol and store the result in the "remainder" variable
        String remainder = uri.toString().substring(scheme.length());

        // Strip out the protocol from the base URI
        String apiRemainder = getBaseUri().toString().substring(apiScheme.length());

        // Check that the remainder starts with the apiRemainder
        if (!remainder.startsWith(apiRemainder)) {
            return null;
        }

        // Return the path, stripped out of the base uri's path
        return uri.getPath().substring(getBaseUri().getPath().length());
    }

    private static Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(Uri.decode(name));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }

    /**
     * Extracts the query parameters as {@link RequestParams}
     *
     * @param uri
     */
    protected static RequestParams getParams(Uri uri) {
        RequestParams params = new RequestParams();
        Set<String> keys = getQueryParameterNames(uri);
        for (String key : keys) {
            String value = uri.getQueryParameter(key);
            if (TextUtils.isEmpty(value)) {
                value = null;
            }
            params.put(key, value);
        }
        return params;
    }

    /**
     * Checks that the provided URI points to the WonderPush REST server
     *
     * @param uri
     */
    protected static boolean isAPIUri(Uri uri) {
        if (uri == null) {
            return false;
        }

        return getBaseUri().getHost().equals(uri.getHost());
    }

    /**
     * @return The WonderPush base URL as a {@link android.net.Uri}
     */
    protected static Uri getBaseUri() {
        if (sBaseUri == null && WonderPush.getBaseURL() != null) {
            sBaseUri = Uri.parse(WonderPush.getBaseURL());
        }
        return sBaseUri;
    }

    /**
     * Returns the absolute URL for the given resource
     *
     * @param resource
     *            The resource path, which may or may not start with
     *            "/"+WonderPush.API_VERSION
     */
    protected static String getAbsoluteUrl(String resource) {
        if (resource.startsWith("/" + WonderPush.API_VERSION)) {
            resource = resource.substring(1 + WonderPush.API_VERSION.length());
        }
        return WonderPush.getBaseURL() + resource;
    }

    /**
     * Returns the non secure absolute url for the given resource
     *
     * @param resource
     *            The resource path, which may or may not start with
     *            "/"+WonderPush.API_VERSION
     */
    protected static String getNonSecureAbsoluteUrl(String resource) {
        if (resource.startsWith("/" + WonderPush.API_VERSION)) {
            resource = resource.substring(1 + WonderPush.API_VERSION.length());
        }
        return WonderPush.getNonSecureBaseURL() + resource;
    }

}
