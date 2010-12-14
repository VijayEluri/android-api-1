package com.hoccer.api.android;

import java.io.IOException;
import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.hoccer.api.ClientConfig;
import com.hoccer.data.StreamableContent;
import com.hoccer.http.AsyncHttpGet;
import com.hoccer.http.AsyncHttpPut;
import com.hoccer.http.AsyncHttpRequest;
import com.hoccer.http.HttpResponseHandler;

public class FileCacheService extends Service {

    private final HashMap<String, AsyncHttpRequest> mOngoingRequests = new HashMap<String, AsyncHttpRequest>();

    protected void stopWhenAllLoadsFinished() {
        if (mOngoingRequests.size() == 0) {
            stopSelf();
        }
    }

    public void fetch(String uri, StreamableContent sink, HttpResponseHandler responseHandler) {
        AsyncHttpGet fetchRequest = new AsyncHttpGet(uri);
        fetchRequest.registerResponseHandler(responseHandler);
        fetchRequest.setStreamableContent(sink);
        fetchRequest.start();
        mOngoingRequests.put(uri, fetchRequest);
    }

    public String store(StreamableContent source, int secondsUntilExipred,
            HttpResponseHandler responseHandler) throws IOException {
        String uri = ClientConfig.getFileCacheBaseUri() + "/" + source.getFilename()
                + "?expires_in=" + secondsUntilExipred;

        AsyncHttpPut storeRequest = new AsyncHttpPut(uri);
        storeRequest.registerResponseHandler(responseHandler);
        storeRequest.setBody(source);
        storeRequest.start();
        mOngoingRequests.put(uri, storeRequest);

        return uri;
    }

    public void cancel(String uri) {
        AsyncHttpRequest request = mOngoingRequests.remove(uri);
        request.interrupt();
        Log.v("FileCacheService", "aborting " + uri + " as " + request + " interr: "
                + request.isInterrupted());
        ;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
