package com.hoccer.api.android;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoccer.api.BadModeException;
import com.hoccer.api.ClientActionException;
import com.hoccer.api.ClientConfig;
import com.hoccer.api.CollidingActionsException;
import com.hoccer.api.Linccer;
import com.hoccer.api.UpdateException;

public class AsyncLinccer extends Linccer {

    public static class MessageType {
        public final static int SEARCHING         = 4;
        public final static int SHARED            = 3;
        public final static int RECEIVED          = 2;
        public final static int NOTHING_SHARED    = 1;
        public final static int NOTHING_RECEIVED  = 0;
        public final static int BAD_MODE          = -1;
        public final static int BAD_CLIENT_ACTION = -2;
        public final static int COLLISION         = -3;
        public final static int UNKNOWN_EXCEPTION = -4;
    }

    public AsyncLinccer(ClientConfig config) {
        super(config);
    }

    public void asyncShare(final String mode, final JSONObject payload, final Handler handler) {
        new Thread(new Runnable() {
            public void run() {

                Message msg = handler.obtainMessage();
                try {
                    handler.handleMessage(handler.obtainMessage(MessageType.SEARCHING));
                    msg.obj = share(mode, payload);

                    if (msg.obj != null) {
                        msg.what = MessageType.SHARED;
                    } else {
                        msg.what = MessageType.NOTHING_SHARED;
                    }
                } catch (BadModeException e) {
                    msg.what = MessageType.BAD_MODE;
                    msg.obj = e;
                } catch (ClientActionException e) {
                    msg.what = MessageType.BAD_CLIENT_ACTION;
                    msg.obj = e;
                } catch (CollidingActionsException e) {
                    msg.what = MessageType.COLLISION;
                    msg.obj = e;
                } catch (Exception e) {
                    msg.what = MessageType.UNKNOWN_EXCEPTION;
                    msg.obj = e;
                }

                Log.v("Linccer", msg.what + " " + msg.obj);

                handler.handleMessage(msg);
            }
        }).start();
    }

    public void asyncReceive(final String mode, final Handler handler) {
        new Thread(new Runnable() {
            public void run() {

                Message msg = handler.obtainMessage();
                try {
                    handler.handleMessage(handler.obtainMessage(MessageType.SEARCHING));
                    msg.obj = receive(mode);

                    if (msg.obj != null) {
                        msg.what = MessageType.RECEIVED;
                    } else {
                        msg.what = MessageType.NOTHING_RECEIVED;
                    }
                } catch (BadModeException e) {
                    msg.what = MessageType.BAD_MODE;
                    msg.obj = e;
                } catch (ClientActionException e) {
                    msg.what = MessageType.BAD_CLIENT_ACTION;
                    msg.obj = e;
                } catch (CollidingActionsException e) {
                    msg.what = MessageType.COLLISION;
                    msg.obj = e;
                } catch (Exception e) {
                    msg.what = MessageType.UNKNOWN_EXCEPTION;
                    msg.obj = e;
                }

                Log.v("Linccer", msg.what + " " + msg.obj);

                handler.handleMessage(msg);
            }
        }).start();

    }

    public void onWifiScanResults(List<ScanResult> scanResults) throws UpdateException {
        if (scanResults != null) {
            List<String> bssids = new ArrayList<String>();
            for (ScanResult scan : scanResults) {
                bssids.add(scan.BSSID);
            }
            onWifiChanged(bssids);
        }
    }

    public void onNetworkChanged(Location location) throws UpdateException {
        onNetworkChanged(location.getLatitude(), location.getLongitude(), (int) location
                .getAccuracy(), location.getTime());
    }

    public void onGpsChanged(Location location) throws UpdateException {
        onGpsChanged(location.getLatitude(), location.getLongitude(), (int) location.getAccuracy(),
                location.getTime());
    }

    public static String getClientIdFromSharedPreferences(Context context, String appName) {
        SharedPreferences prefs = context.getSharedPreferences(appName, Context.MODE_PRIVATE);

        String tmpUUID = UUID.randomUUID().toString();
        String storedUUID = prefs.getString("client_uuid", tmpUUID);

        if (tmpUUID.equals(storedUUID)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("client_uuid", tmpUUID);
            editor.commit();
        }
        return storedUUID;
    }
}
