package eu.hgross.blaubot.android.views;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.messaging.BlaubotChannelManager;
import eu.hgross.blaubot.messaging.BlaubotChannelManagerInfo;
import eu.hgross.blaubot.messaging.ChannelInfo;
import eu.hgross.blaubot.ui.IBlaubotDebugView;

/**
 * Android view to display informations about the StateMachine's history of states.
 * Add this view to a blaubot instance like this: stateView.registerBlaubotInstance(blaubot);
 * 
 * @author Henning Gross <mail.to@henning-gross.de>
 * 
 */
public class ChannelManagerView extends LinearLayout implements IBlaubotDebugView {
    private static final String LOG_TAG = "ChannelManagerView";
    private Handler mUiHandler;
    private Blaubot mBlaubot;

    public static final long UPDATE_INTERVAL = 1000;
    private ScheduledExecutorService scheduledThreadPoolExecutor;
    private boolean initialized = false;
    private ConcurrentHashMap<Short, ChannelView> mChannelViews;

    public ChannelManagerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ChannelManagerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    /**
     * Hacky way to update the state of the ServerConnector.
     */
    private Runnable updateUiTask = new Runnable() {
        @Override
        public void run() {
            final BlaubotChannelManagerInfo info;
            if (initialized && mBlaubot != null) {
                final BlaubotChannelManager channelManager = mBlaubot.getChannelManager();
                info = channelManager.createChannelManagerInfo();
            } else {
                info = null;
            }
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (info != null) {
                        updateUI(info);
                    }
                }
            });
        }
    };

    private void initView() {
        this.mUiHandler = new Handler(Looper.getMainLooper());
        this.mChannelViews = new ConcurrentHashMap<>();
        this.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {

            @Override
            public void onViewAttachedToWindow(View v) {
                // start updates
                scheduledThreadPoolExecutor = Executors.newSingleThreadScheduledExecutor();
                scheduledThreadPoolExecutor.scheduleAtFixedRate(updateUiTask, (long) 0, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                // stop updates
                if (scheduledThreadPoolExecutor != null) {
                    scheduledThreadPoolExecutor.shutdown();
                    scheduledThreadPoolExecutor = null;
                }
            }
        });
        this.initialized = true;
    }


    private void updateUI(final BlaubotChannelManagerInfo channelManagerInfo) {
        // append the views
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Set<Short> channels = new HashSet<Short>();
                for (ChannelInfo channelInfo : channelManagerInfo.getChannels()) {
                    final short channelId = channelInfo.getChannelConfig().getChannelId();
                    channels.add(channelId);
                    ChannelView channelView = new ChannelView(getContext(), null);
                    boolean added = mChannelViews.putIfAbsent(channelId, channelView) == null;
                    channelView = mChannelViews.get(channelId);
                    if (added) {
                        addView(channelView);
                    }
                    channelView.setChannelInfo(channelInfo);

                }

                // remove channels that are not in the list anymore
                for (Map.Entry<Short, ChannelView> entry : mChannelViews.entrySet()) {
                    if (!channels.contains(entry.getKey())) {
                        removeView(entry.getValue());
                    }
                }

                invalidate();
            }
        });
    }

    /**
     * Register this view with the given blaubot instance
     *
     * @param blaubot the blaubot instance to connect with
     */
    public void registerBlaubotInstance(Blaubot blaubot) {
        if (mBlaubot != null) {
            unregisterBlaubotInstance();
        }
        this.mBlaubot = blaubot;
    }

    @Override
    public void unregisterBlaubotInstance() {
        if (mBlaubot != null) {
            // unregister listeners

            this.mBlaubot = null;
        }
    }
}