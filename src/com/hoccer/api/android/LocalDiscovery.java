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
 * Uses a state machine to track the state of the registering process, which is necessary, because a registration is
 * performed asynchronously by the ZeroConf implementation, and a record needs to be properly registered before it can
 * be unregistered. So, if publish is called, the object enters the REGISTERING state. If revoke is now called before
 * the registration was successful (appropriate serviceUpdated callback), the object enters WAITING_TO_UNREGISTER state
 * and will call unregister once the registration has finished. WAITING_TO_REGISTER works analogously the other way
 * round.
 * 
 * @author Arne Handt, it@handtwerk.de
 */
public class LocalDiscovery {

    // Constants ---------------------------------------------------------

    private static final String LOG_TAG = LocalDiscovery.class.getSimpleName();

    /** Key of the client's ID property of the mDNS record */
    private static final String ID_PROPERTY = "id";

    // Instance Fields ---------------------------------------------------

    /** Initial state. Furthermore, is entered if unregistration completes while in UNREGISTERING. */
    private final State UNREGISTERED = new UnregisteredState();

    /**
     * Is entered if publish is called while in in UNREGISTERED or if unregistration completes while in
     * WAITING_TO_REGISTER.
     */
    private final State REGISTERING = new RegisteringState();

    /** Is entered if registration completes while in REGISTERING. */
    private final State REGISTERED = new RegisteredState();

    /** Is entered if revoke is called while in REGISTERING */
    private final State WAITING_TO_UNREGISTER = new WaitingToUnregisterState();

    /**
     * Is entered if revoke is called while in REGISTERED or if registration completes while in WAITING_TO_UNREGISTER.
     */
    private final State UNREGISTERING = new UnregisteringState();

    /** Is entered if publish is called while in UNREGISTERING. */
    private final State WAITING_TO_REGISTER = new WaitingToRegisterState();

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

    private State mState = UNREGISTERED;

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

        Log.d(LOG_TAG, "publishAnnouncement, name='" + pName + "', clientId='" + pClientId
                + "' is waiting to be executed");
        synchronized (mZeroConf) {

            Log.d(LOG_TAG, "publishAnnouncement is executing...");

            mRecord.setProperty(ID_PROPERTY, pClientId);
            mRecord.name = pName;
            mState.onPublish();

            Log.d(LOG_TAG, "publishAnnouncement is done");
        }
    }

    /**
     * Removes the service record identifying this Hoccer client.
     */
    public void revokeAnnouncement() {

        Log.d(LOG_TAG, "revokeAnnouncement");

        synchronized (mZeroConf) {

            mState.onRevoke();
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

    private void register() {

        Log.d(LOG_TAG, "Register, name='" + mRecord.name + "'");

        synchronized (mZeroConf) {

            mZeroConf.registerService(mRecord);
        }

        Log.d(LOG_TAG, "Register is done.");
    }

    private void unregister() {

        Log.d(LOG_TAG, "Unregister, name='" + mRecord.name + "'");

        synchronized (mZeroConf) {

            mZeroConf.unregisterService(mRecord.clientKey);
        }

        Log.d(LOG_TAG, "Unregister is done.");
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

    private boolean isMyClientId(String pId) {

        return pId != null && !pId.equals(mRecord.getPropertyString(ID_PROPERTY));
    }

    // Inner Classes -----------------------------------------------------

    public interface Listener {

        public void onVisiblePeersChanged(Collection<String> pVisibleIds);
    }

    /** Receives updates from the ZeroConfClient and updates the set of visible peer IDs accordingly */
    private class ZeroConfListener implements ZeroConfClient.Listener {

        @Override
        public void serviceUpdated(ZeroConfRecord pRecord) {

            String hoccerClientId = pRecord.getPropertyString(ID_PROPERTY);

            Log.d(LOG_TAG, "serviceUpdated, name: '" + pRecord.name + "', id: '" + hoccerClientId + "'");
            // Log.d(LOG_TAG, "properties: ");
            // for (String name : pRecord.getPropertyNames()) {
            //
            // Log.d(LOG_TAG, " - " + name + ": " + pRecord.getPropertyString(name));
            // }

            if (hoccerClientId != null && !isMyClientId(hoccerClientId)) {
                // it's a hoccer client

                Log.d(LOG_TAG, "it's a hoccer client!");

                if (isMyClientId(hoccerClientId)) {

                    Log.d(LOG_TAG, "it's me!");
                    synchronized (mZeroConf) {

                        mState.onUpdated();
                    }

                } else {

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
        }

        @Override
        public void serviceRemoved(ZeroConfRecord pRecord) {

            String hoccerClientId = pRecord.getPropertyString(ID_PROPERTY);

            Log.d(LOG_TAG, "serviceRemoved, name: '" + pRecord.name + "', id: '" + hoccerClientId + "'");

            if (hoccerClientId != null) {
                // it's a hoccer client

                Log.d(LOG_TAG, "it's a hoccer client!");

                if (isMyClientId(hoccerClientId)) {

                    Log.d(LOG_TAG, "it's me!");

                    synchronized (mZeroConf) {

                        mState.onRemoved();
                    }
                } else {

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
        }

        @Override
        public void connectedToService() {

            Log.d(LOG_TAG, "connectedToService");
        }
    }

    private abstract class State {

        public void onPublish() {

            Log.d(LOG_TAG, "State " + this + " is ignoring event onPublish");
        }

        public void onRevoke() {

            Log.d(LOG_TAG, "State " + this + " is ignoring event onRevoke");
        }

        public void onUpdated() {

            Log.d(LOG_TAG, "State " + this + " is ignoring event onUpdated");
        }

        public void onRemoved() {

            Log.d(LOG_TAG, "State " + this + " is ignoring event onRemoved");
        }
    }

    private class UnregisteredState extends State {

        @Override
        public void onPublish() {

            Log.d(LOG_TAG, "State " + this + " - onPublish");

            register();
            mState = REGISTERING;
        }
    }

    private class RegisteringState extends State {

        @Override
        public void onUpdated() {

            Log.d(LOG_TAG, "State " + this + " - onUpdated");

            mState = REGISTERED;
        }

        @Override
        public void onRevoke() {

            mState = WAITING_TO_UNREGISTER;
        }
    }

    private class WaitingToUnregisterState extends State {

        @Override
        public void onUpdated() {

            Log.d(LOG_TAG, "State " + this + " - onUpdated");

            unregister();
            mState = UNREGISTERING;
        }
    }

    private class RegisteredState extends State {
        @Override
        public void onRevoke() {

            Log.d(LOG_TAG, "State " + this + " - onRevoke");

            unregister();
            mState = UNREGISTERING;
        }
    }

    private class UnregisteringState extends State {
        @Override
        public void onPublish() {

            Log.d(LOG_TAG, "State " + this + " - onPublish");

            mState = WAITING_TO_REGISTER;
        }

        @Override
        public void onRemoved() {

            Log.d(LOG_TAG, "State " + this + " - onRemoved");

            mState = UNREGISTERED;
        }
    }

    private class WaitingToRegisterState extends State {

        @Override
        public void onRemoved() {

            Log.d(LOG_TAG, "State " + this + " - onRemoved");

            register();
            mState = REGISTERING;
        }
    }
}
