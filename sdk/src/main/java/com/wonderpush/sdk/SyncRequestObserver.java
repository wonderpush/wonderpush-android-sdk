package com.wonderpush.sdk;

import org.json.JSONObject;

import java.util.Map;

/**
 * The request-layer surface installed into {@link BaseApiClient}: inject outgoing params, consume
 * incoming responses. Ported from wonderpush-ios-sdk {@code WPSyncRequestObserver}.
 */
interface SyncRequestObserver {

    /** Params to merge onto an outgoing request (empty unless opportunistic + injection enabled). */
    Map<String, Object> prepareOutgoingParams(String path, String method);

    /** Process an API response: classify, run the processor per source, execute decisions. */
    void consumeIncomingResponse(String path, String method, JSONObject response);
}
