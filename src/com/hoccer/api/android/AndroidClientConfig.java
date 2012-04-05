package com.hoccer.api.android;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;

import com.hoccer.api.ClientConfig;

public class AndroidClientConfig extends ClientConfig {

	// Static Methods ----------------------------------------------------

	public static String getServerNameFromSharedPreferences(Context context) {

		SharedPreferences prefs = context.getSharedPreferences(
				"com.artcom.hoccer_preferences", Context.MODE_WORLD_READABLE
						| Context.MODE_WORLD_WRITEABLE);

		String tmpServerName = "https://linccer.hoccer.com/v3";
		String serverName = prefs.getString("hoccer_server", tmpServerName);

		if (tmpServerName.equals(serverName)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("hoccer_server", tmpServerName);
			editor.commit();
		}
		return serverName;
	}

	public static String getFileCacheServerNameFromSharedPreferences(
			Context context) {

		SharedPreferences prefs = context.getSharedPreferences(
				"com.artcom.hoccer_preferences", Context.MODE_WORLD_READABLE
						| Context.MODE_WORLD_WRITEABLE);

		String tmpServerName = "https://filecache.hoccer.com/v3";
		String serverName = prefs.getString("hoccer_filecache_server",
				tmpServerName);

		if (tmpServerName.equals(serverName)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("hoccer_filecache_server", tmpServerName);
			editor.commit();
		}
		return serverName;
	}

	public static String getHocletServerNameFromSharedPreferences(
			Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				"com.artcom.hoccer_preferences", Context.MODE_WORLD_READABLE
						| Context.MODE_WORLD_WRITEABLE);

		String tmpServerName = "https://hoclet-experimental.hoccer.com/v3";
		String serverName = prefs.getString("hoccer_hoclet_server",
				tmpServerName);

		if (tmpServerName.equals(serverName)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("hoccer_hoclet_server", tmpServerName);
			editor.commit();
		}
		return serverName;
	}

	public static void useSettingsServers(Context context) {

		useTestingServers();
		// mLinccerUri = getServerNameFromSharedPreferences(context);
		// mFileCacheUri = getFileCacheServerNameFromSharedPreferences(context);
	}

	// Constructors ------------------------------------------------------

	public AndroidClientConfig(String applicatioName, String apiKey,
			String sharedSecret, UUID clientId, String clientName,
			String publicKey) {

		super(applicatioName, apiKey, sharedSecret, clientId, clientName,
				publicKey);
	}

	public AndroidClientConfig(String applicatioName, String apiKey,
			String sharedSecret) {

		super(applicatioName, apiKey, sharedSecret);
	}

	public AndroidClientConfig(String applicatioName, UUID clientId,
			String clientName) {

		super(applicatioName, clientId, clientName);
	}

	public AndroidClientConfig(String applicatioName) {

		super(applicatioName);
	}

}
