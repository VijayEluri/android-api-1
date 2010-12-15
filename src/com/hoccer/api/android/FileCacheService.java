package com.hoccer.api.android;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.hoccer.api.ClientConfig;
import com.hoccer.api.FileCache;
import com.hoccer.data.StreamableContent;
import com.hoccer.http.HttpResponseHandler;

public class FileCacheService extends Service {

    private FileCache mFileCache;

    public void init(ClientConfig config) {
        if (mFileCache == null) {
            mFileCache = new FileCache(config);
        }
    }

    protected void stopWhenAllLoadsFinished() {
        if (mFileCache.getOngoingRequests().size() == 0) {
            stopSelf();
        }
    }

    public void fetch(String uri, StreamableContent sink, HttpResponseHandler responseHandler) {
        mFileCache.asyncFetch(uri, sink, responseHandler);
    }

    public String store(StreamableContent source, int secondsUntilExipred,
            HttpResponseHandler responseHandler) throws IOException {
        String uri = mFileCache.asyncStore(source, secondsUntilExipred, responseHandler);
        return uri;
    }

    public void cancel(String uri) {
        mFileCache.cancel(uri);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
