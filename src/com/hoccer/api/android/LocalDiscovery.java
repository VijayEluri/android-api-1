package com.hoccer.api.android;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import prom.android.zeroconf.client.ZeroConfClient;
import prom.android.zeroconf.model.ZeroConfRecord;
import android.content.Context;
import android.util.Log;

/**
 * Uses mDNS/DNS-SD to announce this instance and receive announcements from peers in the same net.
 * 
 * @author Arne Handt, it@handtwerk.de
 */
public class LocalDiscovery {

    // Constants ---------------------------------------------------------

    private static final String LOG_TAG = LocalDiscovery.class.getSimpleName();

    /** Key of the client's ID property of the mDNS record */
    private static final String ID_PROPERTY = "id";

    // Instance Fields ---------------------------------------------------

    /** Listeners attached to this object */
    private final HashSet<Listener> mListeners;
    
    /** Contains the peer IDs seen by this client. Is updated through ZeroConfClient's callbacks. */
    private final HashSet<String> mVisibleIds;

    /** Provides mDNS functionality */
    private final ZeroConfClient mZeroConf;

    /** Receives callbacks from the ZeroConfClient */
    private final ZeroConfListener mZeroConfListener;
    
    /** mDNS record for announcing this client */
    private final ZeroConfRecord mRecord;

    // Constructors ------------------------------------------------------

    /**
     * Creates a new instance that is not connected to an mDNS service and doesn't yet announce a record.
     * 
     * @param pContext
     */
    public LocalDiscovery(Context pContext) {

        mVisibleIds = new HashSet<String>();

        mListeners = new HashSet<Listener>();
        mZeroConf = new ZeroConfClient(pContext);

        mZeroConfListener = new ZeroConfListener();
        mZeroConf.registerListener(mZeroConfListener);

        // prepare record for later use
        mRecord = new ZeroConfRecord();
        mRecord.port = 0;
        mRecord.domain = "local";
        mRecord.protocol = "tcp";
        mRecord.application = "hoccer";
        mRecord.type = "_" + mRecord.application + "._" + mRecord.protocol + "." + mRecord.domain + ".";
        mRecord.clientKey = UUID.randomUUID().toString();
    }

    // Public Instance Methods -------------------------------------------

    /**
     * Publishes an announcement for this client. Later announcements override earlier ones, i.e. you always announce
     * only one service.
     * 
     * @param pName
     *            the human readable name
     * @param pClientId
     *            the peer ID of this Hoccer client
     */
    public void publishAnnouncement(final String pName, final String pClientId) {

        Log.d(LOG_TAG, "publishAnnoucnement('" + pName + "', '" + pClientId + "')");

        new Thread(new Runnable() {

            public void run() {

                synchronized (mZeroConf) {

                    mRecord.name = pName;
                    mRecord.setProperty(ID_PROPERTY, pClientId);
                    mZeroConf.registerService(mRecord);
                }
            }
        }).start();
    }

    /**
     * Removes the service record identifying this Hoccer client.
     */
    public void revokeAnnouncement() {

        Log.d(LOG_TAG, "revokeAnnouncement");

        synchronized (mZeroConf) {
            
            mZeroConf.unregisterService(mRecord.clientKey);
        }
    }

    /**
     * Connects to the mDNS service. This is a prerequisite for announcing this client and receiving updates abount
     * visible peers
     */
    public void connect() {

        Log.d(LOG_TAG, "connect");

        synchronized (mZeroConf) {

            mZeroConf.connectToService();
        }
    }

    /**
     * Shuts down the connection to the mDNS service.
     */
    public void disconnect() {

        Log.d(LOG_TAG, "disconnect");

        synchronized (mZeroConf) {

            mZeroConf.disconnectFromService();
        }
    }

    /** Returns the set of peers IDs that are currently visible via mDNS */
    public Collection<String> getVisibleIds() {

        synchronized (mVisibleIds) {

            return Collections.unmodifiableCollection(mVisibleIds);
        }
    }

    /** Attaches the given listener to this object */
    public void addListener(Listener pListener) {

        synchronized (mListeners) {

            mListeners.add(pListener);
        }
    }

    /** Removes the given listener from this object */
    public void removeListener(Listener pListener) {

        synchronized (mListeners) {

            mListeners.remove(pListener);
        }
    }

    // Protected Instance Methods ----------------------------------------

    @Override
    protected void finalize() throws Throwable {

        Log.d(LOG_TAG, "finalize");

        mZeroConf.unregisterService(mRecord.clientKey);
        mZeroConf.disconnectFromService();
    }

    // Private Instance Methods ------------------------------------------

    /**
     * Extracts the Hoccer client ID from a service record. May return null.
     * 
     * @param pRecord
     * @return the Hoccer client ID, if present as a property and not my own ID, or null otherwise.
     */
    private String extractId(ZeroConfRecord pRecord) {

        String id = pRecord.getPropertyString(ID_PROPERTY);
        if (id == null) {

            // not a hoccer client
            return null;
        }

        // i get updates about myself, too, ...
        String myId = mRecord.getPropertyString(ID_PROPERTY);
        if (id.equals(myId)) {

            // ...but i'm not interested in them
            return null;
        }

        return id;
    }

    /**
     * Updates the listeners that the set of visible peers has changed.
     */
    private void onVisiblePeersChanged() {

        HashSet<String> peerIds;
        synchronized (mVisibleIds) { // copy to keep synch'd block short
            peerIds = new HashSet<String>(mVisibleIds);
        }

        HashSet<Listener> listeners;
        synchronized (mListeners) { // copy to keep synch'd block short
            listeners = new HashSet<LocalDiscovery.Listener>(mListeners);
        }

        for (Listener listener : listeners) {
            try {

                listener.onVisiblePeersChanged(peerIds);

            } catch (Throwable t) {

                Log.e(LOG_TAG, "error while executing listener", t);
            }
        }
    }

    // Inner Classes -----------------------------------------------------

    public interface Listener {

        public void onVisiblePeersChanged(Collection<String> pVisibleIds);
    }

    /** Receives updates from the ZeroConfClient and updates the set of visible peer IDs accordingly */
    private class ZeroConfListener implements ZeroConfClient.Listener {

        @Override
        public void serviceUpdated(ZeroConfRecord pRecord) {

            String hoccerClientId = extractId(pRecord);

            Log.d(LOG_TAG, "serviceUpdated, name: '" + pRecord.name + "', id: '" + hoccerClientId + "'");
            Log.d(LOG_TAG, "properties: ");
            for (String name : pRecord.getPropertyNames()) {

                Log.d(LOG_TAG, " - " + name + ": " + pRecord.getPropertyString(name));
            }

            if (hoccerClientId != null) {
                // it's a hoccer client

                boolean peersChanged = false;
                synchronized (mVisibleIds) { // synchronize only as long as necessary

                    if (!mVisibleIds.contains(hoccerClientId)) {

                        mVisibleIds.add(hoccerClientId);
                        peersChanged = true;
                    }
                }

                if (peersChanged) {
                    // update listeners
                    onVisiblePeersChanged();
                }
            }
        }

        @Override
        public void serviceRemoved(ZeroConfRecord pRecord) {

            String hoccerClientId = extractId(pRecord);

            Log.d(LOG_TAG, "serviceRemoved, name: '" + pRecord.name + "', id: '" + hoccerClientId + "'");

            if (hoccerClientId != null) {
                // it's a hoccer client

                boolean peersChanged = false;
                synchronized (mVisibleIds) { // synchronize only as long as necessary

                    if (mVisibleIds.contains(hoccerClientId)) {

                        mVisibleIds.remove(hoccerClientId);
                        peersChanged = true;
                    }
                }

                if (peersChanged) {
                    // update listeners
                    onVisiblePeersChanged();
                }
            }
        }

        @Override
        public void connectedToService() {

            Log.d(LOG_TAG, "connectedToService");
        }
    }

}
