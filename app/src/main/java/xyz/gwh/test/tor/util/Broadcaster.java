/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package xyz.gwh.test.tor.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import xyz.gwh.test.tor.service.TorStatus;

/**
 * Broadcasts Tor status, traffic information and log messages via a local Intent.
 * Follows the singleton pattern as it's used across several classes.
 */
public class Broadcaster {

    public interface OnStatusChangedListener {
        public void onStatusChanged(TorStatus status);
    }

    private static Broadcaster INSTANCE;

    private static final String ACTION_LOG = "ACTION_LOG";
    private static final String ACTION_STATUS = "ACTION_STATUS";
    private static final String ACTION_TRAFFIC = "ACTION_TRAFFIC";

    private static final String ARG_MESSAGE = "ARG_MESSAGE";
    private static final String ARG_STATUS = "ARG_STATUS";
    private static final String ARG_UPLOAD = "ARG_UPLOAD";
    private static final String ARG_DOWNLOAD = "ARG_DOWNLOAD";
    private static final String ARG_WRITTEN = "ARG_WRITTEN";
    private static final String ARG_READ = "ARG_READ";

    private final LocalBroadcastManager localBroadcastManager;
    private OnStatusChangedListener onStatusChangedListener;
    private Context context;

    private Broadcaster(Context context) {
        this.context = context;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public static synchronized void initialize(Context context) {
        if (INSTANCE != null) {
            INSTANCE = new Broadcaster(context);
        }
    }

    public static synchronized Broadcaster getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("You must call initialize() before getInstance()");
        }
        return INSTANCE;
    }

    public void traffic(long upload, long download, long written, long read) {
        final Intent intent = new Intent(ACTION_TRAFFIC);
        intent.putExtra(ARG_UPLOAD, upload);
        intent.putExtra(ARG_DOWNLOAD, download);
        intent.putExtra(ARG_WRITTEN, written);
        intent.putExtra(ARG_READ, read);

        localBroadcastManager.sendBroadcast(intent);
    }

    public void status(TorStatus status) {
        final Intent intent = new Intent(ACTION_STATUS);
        intent.putExtra(ARG_STATUS, status);

        localBroadcastManager.sendBroadcast(intent);

        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(status);
        }
    }

    public void log(String message, Exception exception) {
        log(message + '\n' + exception.getMessage());
    }

    public void log(@StringRes int resId) {
        log(context.getString(resId));
    }

    public void logFormat(@StringRes int resId, String... args) {
        log(String.format(context.getString(resId), args));
    }

    public void log(String message) {
        final Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(ARG_MESSAGE, message);

        localBroadcastManager.sendBroadcast(intent);
    }

    public void registerOnStatusChangedListener(OnStatusChangedListener listener) {
        onStatusChangedListener = listener;
    }
}