package com.wonderpush.sdk.push;

import java.util.Collection;

public class PushServiceResult {

    private String data;
    private String service;
    private String senderIds;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getSenderIds() {
        return senderIds;
    }

    public void setSenderIds(String senderIds) {
        this.senderIds = senderIds;
    }

}
