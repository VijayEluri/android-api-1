package com.hoccer.api.android;

import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.hoccer.data.StreamableContent;
import com.hoccer.http.AsyncHttpGet;
import com.hoccer.http.AsyncHttpRequest;
import com.hoccer.http.HttpResponseHandler;

public class FileCacheService extends Service {

    private final HashMap<String, AsyncHttpRequest> mOngoingRequests = new HashMap<String, AsyncHttpRequest>();

    protected void stopWhenAllLoadsFinished() {

    }

    public void fetch(String url, StreamableContent sink, HttpResponseHandler responseHandler) {
        AsyncHttpGet fetchRequest = new AsyncHttpGet(url);
        fetchRequest.registerResponseHandler(responseHandler);
        fetchRequest.setStreamableContent(sink);
        fetchRequest.start();
        mOngoingRequests.put(url, fetchRequest);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
