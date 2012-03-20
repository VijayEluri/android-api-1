package com.hoccer.api.android;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.util.Log;

import com.hoccer.util.HoccerLoggers;

/**
 * Logging handler that forwards incoming log records to Android's logcat so
 * that log output from Android-agnostic code will appear in the Android log.
 * Call {@link engage} once your app starts to enable log forwarding and in your
 * Android-agnostic code get your logger instance from {@link HoccerLoggers} to
 * use this feature conveniently.
 * 
 * In Android-dependant code you should never use this, but use Android's Log
 * class instead.
 * 
 * @author Arne Handt, it@handtwerk.de
 * 
 */
public class AndroidLogHandler extends Handler {

	// Constants ---------------------------------------------------------

	private static final String LOG_TAG = AndroidLogHandler.class
			.getSimpleName();

	private static final int ALL = Level.ALL.intValue();
	private static final int FINEST = Level.FINEST.intValue();
	private static final int FINER = Level.FINER.intValue();
	private static final int FINE = Level.FINE.intValue();
	private static final int CONFIG = Level.CONFIG.intValue();
	private static final int WARNING = Level.WARNING.intValue();

	private static final AndroidLogHandler INSTANCE = new AndroidLogHandler();

	// not used:
	// private static final int INFO = Level.INFO.intValue();
	// private static final int SEVERE = Level.SEVERE.intValue();
	// private static final int OFF = Level.OFF.intValue();

	// Static Methods ----------------------------------------------------

	public static void engage() {

		Log.d(LOG_TAG, "engaging log forwarding");
		HoccerLoggers.addHandler(INSTANCE);
	}

	// Public Instance Methods -------------------------------------------

	@Override
	public void publish(LogRecord record) {

		final String msg = record.getMessage();
		final String tag = record.getLoggerName();
		final int level = record.getLevel().intValue();

		// switch doesn't work because log level int values can't be determined
		// at compile time
		if (ALL == level || FINEST == level || FINER == level) {

			Log.v(tag, msg);

		} else if (FINE == level) {

			Log.d(tag, msg);

		} else if (CONFIG == level) {

			Log.i(tag, msg);

		} else if (WARNING == level) {

			Log.w(tag, msg);
		}

		// INFO, SEVERE: handled by the system
		// OFF: ignored
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}
}