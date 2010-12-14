package com.hoccer.api.android;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.hoccer.api.ClientConfig;
import com.hoccer.api.FileCache;
import com.hoccer.data.StreamableContent;
import com.hoccer.http.AsyncHttpGet;
import com.hoccer.http.HttpResponseHandler;

public class FileCacheService extends Service {

    private FileCache mFileCache;

    public void setConfig(ClientConfig config) {
        mFileCache = new FileCache(config);
    }

    protected void stopWhenAllLoadsFinished() {
        if (mFileCache.getOngoingRequests().size() == 0) {
            stopSelf();
        }
    }

    public void fetch(String uri, StreamableContent sink, HttpResponseHandler responseHandler) {
        AsyncHttpGet fetchRequest = new AsyncHttpGet(uri);
        fetchRequest.registerResponseHandler(responseHandler);
        fetchRequest.setStreamableContent(sink);
        fetchRequest.start();
    }

    public String store(StreamableContent source, int secondsUntilExipred,
            HttpResponseHandler responseHandler) throws IOException {
        return mFileCache.asyncStore(source, secondsUntilExipred, responseHandler);
    }

    public void cancel(String uri) {
        mFileCache.cancel(uri);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
