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
import java.util.List;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;

import prom.android.zeroconf.client.ZeroConfClient;
import prom.android.zeroconf.model.ZeroConfRecord;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.hoccer.api.UpdateException;

public class LinccLocationManager implements LocationListener, ZeroConfClient.Listener {

    // Constructors ------------------------------------------------------

    private static final String   UNKNOWN_LOCATION_TEXT = "You can not hoc without a location";

    private static final String LOG_TAG = LinccLocationManager.class.getSimpleName();

    // Instance Fields ---------------------------------------------------

    private final LocationManager mLocationManager;
    private final WifiManager     mWifiManager;

    private final Context         mContext;

    private final AsyncLinccer    mLinccer;
    private final Updateable      mUpdater;

    private final ZeroConfClient mZeroConf;
    private final ZeroConfRecord mMdnsRecord;

    // TODO this is a temporary workaround - normally we shouldn't reference the network provider direclty
    private final boolean mNetworkProviderAvailable;

    // Constructors ------------------------------------------------------

    public LinccLocationManager(Context pContext, AsyncLinccer linccer, Updateable updater) {
        mContext = pContext;

        mLinccer = linccer;
        mUpdater = updater;

        mLocationManager = (LocationManager) pContext.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) pContext.getSystemService(Context.WIFI_SERVICE);

        mNetworkProviderAvailable = mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER);
        
        mMdnsRecord = new ZeroConfRecord();
        mMdnsRecord.port = 8033;

        mMdnsRecord.domain = "local";
        mMdnsRecord.protocol = "tcp";
        mMdnsRecord.application = "hoccer";
        mMdnsRecord.type = "_" + mMdnsRecord.application + "._" + mMdnsRecord.protocol + "." + mMdnsRecord.domain + ".";
        mMdnsRecord.clientKey = UUID.randomUUID().toString();

        mZeroConf = new ZeroConfClient(mContext);
        mZeroConf.registerListener(this);
    }

    // Public Instance Methods -------------------------------------------

    public Context getContext() {
        return mContext;
    }

    public AsyncLinccer getLinccer() {
        return mLinccer;
    }

    public void refreshLocation() throws UpdateException, ClientProtocolException, IOException {
        mLinccer.autoSubmitEnvironmentChanges(false);

        mLinccer.onWifiScanResults(mWifiManager.getScanResults());
        Location location;
        if (mNetworkProviderAvailable) {
            location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null)
                mLinccer.onNetworkChanged(location);
        }
        location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null)
            mLinccer.onGpsChanged(location);

        mLinccer.submitEnvironment();
    }

    public void deactivate() {

        Log.d(LOG_TAG, "Deactivating");

        mLocationManager.removeUpdates(this);
        mZeroConf.unregisterService(mMdnsRecord.clientKey);
        mZeroConf.disconnectFromService();
    }

    public void activate() {

        Log.d(LOG_TAG, "Activating");

        mZeroConf.connectToService();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

        if (mNetworkProviderAvailable) {

            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("LinccLocationManager", location.toString());
        if (mUpdater != null) {
            mUpdater.updateNow();
        }
        // try {
        // refreshLocation();
        // } catch (ClientProtocolException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (UpdateException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public Address getAddress(Location location) throws IOException {
        if (location == null) {
            return new Address(null);
        }

        Geocoder gc = new Geocoder(mContext);

        Address address = null;
        List<Address> addresses = gc.getFromLocation(location.getLatitude(),
                location.getLongitude(), 1);
        if (addresses.size() > 0) {
            address = addresses.get(0);
        }
        return address;
    }

    public String getDisplayableAddress(Location location) {

        try {
            Address address = getAddress(location);

            String addressLine = null;
            String info = " (~" + location.getAccuracy() + "m)";
            if (location.getAccuracy() < 500) {
                addressLine = address.getAddressLine(0);
            } else {
                addressLine = address.getAddressLine(1);
            }

            addressLine = trimAddress(addressLine);

            return addressLine + info;

        } catch (Exception e) {
            return UNKNOWN_LOCATION_TEXT + " ~" + location.getAccuracy() + "m";
        }
    }

    @Override
    public void serviceRemoved(ZeroConfRecord pRecord) {
    }

    @Override
    public void serviceUpdated(ZeroConfRecord pRecord) {
    }

    @Override
    public void connectedToService() {

        Log.d(LOG_TAG, "Connected to zero conf service");
        mMdnsRecord.name = "Hoccer Client " + mLinccer.getClientName();
        mZeroConf.registerService(mMdnsRecord);
    }

    // Private Instance Methods ------------------------------------------

    private String trimAddress(String pAddressLine) {
        if (pAddressLine.length() < 27)
            return pAddressLine;

        String newAddress = pAddressLine.substring(0, 18) + "..."
                + pAddressLine.substring(pAddressLine.length() - 5);

        return newAddress;
    }
}
