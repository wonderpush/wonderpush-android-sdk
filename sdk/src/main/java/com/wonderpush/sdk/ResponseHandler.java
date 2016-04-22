package com.wonderpush.sdk;

/**
 * HTTP response handler.
 */
abstract class ResponseHandler {

    /**
     * Called when the request failed. Default implementation is empty.
     */
    public abstract void onFailure(Throwable e, Response errorResponse);

    /**
     * Called on request success. Default implementation is empty.
     */
    public abstract void onSuccess(Response response);

    /**
     * Called on request success. Default implementation calls {@link #onSuccess(Response)}.
     */
    public void onSuccess(int statusCode, Response response) {
        onSuccess(response);
    }

}
