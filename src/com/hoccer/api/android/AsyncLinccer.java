/**
 * Copyright (C) 2010, Hoccer GmbH Berlin, Germany <www.hoccer.com> These coded instructions,
 * statements, and computer programs contain proprietary information of Hoccer GmbH Berlin, and are
 * copy protected by law. They may be used, modified and redistributed under the terms of GNU
 * General Public License referenced below. Alternative licensing without the obligations of the GPL
 * is available upon request. GPL v3 Licensing: This file is part of the "Linccer Android-API".
 * Linccer Android-API is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version. Linccer Android-API is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with Linccer
 * Android-API. If not, see <http://www.gnu.org/licenses/>.
 */
package com.hoccer.api.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoccer.api.BadModeException;
import com.hoccer.api.ClientActionException;
import com.hoccer.api.ClientConfig;
import com.hoccer.api.CollidingActionsException;
import com.hoccer.api.Linccer;
import com.hoccer.api.UpdateException;
import com.hoccer.data.Base64;
import com.hoccer.data.CryptoHelper;

public class AsyncLinccer extends Linccer {

    public static final String  PREFERENCES                    = "com.artcom.hoccer_preferences";

    public static final String  PREF_RENEW_CLIENT_ID           = "renew_client_id_on_start";
    public static final String  PREF_RENEW_KEYPAIR             = "renew_keypair_on_startup";
    public static final String  PREF_USE_ENCRYPTION            = "use_encryption";
    public static final String  PREF_DISTRIBUTE_PUBKEY         = "public_key_distribution";
    public static final String  PREF_AUTO_PASSWORD             = "auto_key_change";

    public static final boolean PREF_DEFAULT_RENEW_CLIENT_ID   = false;
    public static final boolean PREF_DEFAULT_RENEW_KEYPAIR     = false;
    public static final boolean PREF_DEFAULT_USE_ENCRYPTION    = true;
    public static final boolean PREF_DEFAULT_DISTRIBUTE_PUBKEY = true;
    public static final boolean PREF_DEFAULT_AUTO_PASSWORD     = true;

    public static final String  PREF_SHARED_KEY                = "encryption_key";
    public static final String  PREF_PUBLIC_KEY                = "public_key";
    public static final String  PREF_PRIVATE_KEY               = "private_key";

    public static class MessageType {
        public final static int PEEKED            = 6;
        public final static int PEEKING           = 5;
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

    // public void asyncPeek(String groupID, final Handler handler) {
    // new Thread(new Runnable() {
    // public void run() {
    //
    // Message msg = handler.obtainMessage();
    // try {
    // handler.handleMessage(handler.obtainMessage(MessageType.PEEKING));
    // msg.obj = peek(groupID);
    //
    // if (msg.obj != null) {
    // msg.what = MessageType.PEEKED;
    // } else {
    // msg.what = MessageType.NOTHING_RECEIVED;
    // }
    // } catch (ClientActionException e) {
    // msg.what = MessageType.BAD_CLIENT_ACTION;
    // msg.obj = e;
    // } catch (Exception e) {
    // msg.what = MessageType.UNKNOWN_EXCEPTION;
    // msg.obj = e;
    // }
    //
    // Log.v("Linccer", msg.what + " " + msg.obj);
    //
    // handler.handleMessage(msg);
    // }
    // }).start();
    // }

    public void onWifiScanResults(List<ScanResult> scanResults) throws UpdateException,
            ClientProtocolException, IOException {
        if (scanResults != null) {
            List<String> bssids = new ArrayList<String>();
            for (ScanResult scan : scanResults) {
                bssids.add(scan.BSSID);
            }
            onWifiChanged(bssids);
        }
    }

    public void onNetworkChanged(Location location) throws UpdateException,
            ClientProtocolException, IOException {
        onNetworkChanged(location.getLatitude(), location.getLongitude(),
                (int) location.getAccuracy(), location.getTime());
    }

    public void onGpsChanged(Location location) throws UpdateException, ClientProtocolException,
            IOException {
        onGpsChanged(location.getLatitude(), location.getLongitude(), (int) location.getAccuracy(),
                location.getTime());
    }

    public static void renewClientIdInSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String tmpUUID = UUID.randomUUID().toString();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("client_uuid", tmpUUID);
        editor.commit();
    }

    public static String getClientIdFromSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        String tmpUUID = UUID.randomUUID().toString();
        String storedUUID = prefs.getString("client_uuid", tmpUUID);

        if (tmpUUID.equals(storedUUID)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("client_uuid", tmpUUID);
            editor.commit();
        }
        return storedUUID;
    }

    public static boolean getFlagFromSharedPreferences(Context context, String prefid,
            boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        boolean flag = prefs.getBoolean(prefid, defaultValue);
        return flag;
    }

    public static void setFlagInSharedPreferences(Context context, String key, boolean flag) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, flag);
        editor.commit();
    }

    public static String newEncryptionKey() {
        return "^" + Base64.encodeBytes(CryptoHelper.makeRandomBytes(16)) + "$";
    }

    public static boolean isAutoEncryptionKey(String key) {
        if (key.length() >= 2) {
            return key.charAt(0) == '^' && key.charAt(key.length()) == '$';
        }
        return false;
    }

    public static void setInSharedPreferences(Context context, String key, String content) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, content);
        editor.commit();
    }

    public static String getEncryptionKeyFromSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        String defaultValue = newEncryptionKey();
        String storedValue = prefs.getString(PREF_SHARED_KEY, defaultValue);

        if (defaultValue.equals(storedValue)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SHARED_KEY, defaultValue);
            editor.commit();
        }
        return storedValue;
    }

    public static void setEncryptionKeyInSharedPreferences(Context context, String key) {
        setInSharedPreferences(context, PREF_SHARED_KEY, key);
    }

    public static String getUserNameFromSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        String defaultValue = "<" + Build.MODEL + ">";
        String clientName = prefs.getString("client_name", defaultValue);

        if (defaultValue.equals(clientName)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("client_name", defaultValue);
            editor.commit();
        }
        return clientName;
    }

}
