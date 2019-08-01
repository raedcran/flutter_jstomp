package com.jstomp.provider;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

/**
 * @company 上海道枢信息科技-->
 * @anthor created by jingzhanwu
 * @date 2018/1/23
 * @change
 * @describe describe
 * Stomp消息处理
 **/
@SuppressLint("NewApi")
public class StompProvider {

    private static final String TAG = "StompProvider";
    private StompClient mStompClient;
    private static StompProvider instance;
    private Context mContext;
    /*Unified message listening interface*/

    private OnMessageListener messageListener;
    /*Global send listener*/
    private OnMessageSendListener globalSendStatusListener;
    /*Connection monitoring*/
    private OnStompConnectionListener connectionListener;

    /*Stomp connection information configuration class*/
    private StompConfig mConfig;
    /*Whether the markup service has started*/
    public boolean stopService = false;

    public StompClient getStompClient() {
        return mStompClient;
    }

    private CompositeDisposable compositeDisposable;

    /**
     * Stomp connection close listening interface
     */
    public interface OnStompConnectionListener {
        void onConnectionOpened();//Link open

        void onConnectionError(String error);//Link error

        void onConnectionClosed();//Link closed
    }

    /**
     * Total message listening interface
     */
    public interface OnMessageListener {
        void onBroadcastMessage(String stompMsg, String topicUrl);

        void onP2PMessage(String stompMsg, String topicUrl);
    }

    /**
     * Stomp send interface
     */
    public interface OnMessageSendListener {
        void onSendMessage(int status, String userMsg, String tipsMsg);
    }


    private StompProvider() {
    }

    public static StompProvider get() {
        if (instance == null) {
            synchronized (StompProvider.class) {
                if (instance == null) {
                    instance = new StompProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Reset and disconnect subscribers
     */
    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    /**
     * Get current configuration
     *
     * @return
     */
    public StompConfig getConfig() {
        return mConfig;
    }

    /**
     * Initialization operation,
     *
     * @param config Custom configuration information
     */
    public boolean init(Context context, StompConfig config) {
        try {
            resetSubscriptions();
            this.mContext = context;
            this.mConfig = config;
            String url = config.connectionUrl();
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url);
            Log.d(TAG, "Stomp initialization--url:" + url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Start the stomp message service
     */
    @TargetApi(Build.VERSION_CODES.O)
    public StompProvider openConnection(OnStompConnectionListener listener) {
        if (mContext == null) {
            return this;
        }
        try {
            connectionListener = listener;
            // If the StompService has been started and the service is not destroyed, then do not restart the service.
            //just need to re-register Stomp listener
            if (!stopService && StompService.GET() != null) {
                StompService.GET().registerStompConnectionListener();
                return this;
            }

            Intent intent = new Intent(mContext, StompService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // android8.0 or above start service through startForegroundService
                mContext.startForegroundService(intent);
            } else {
                mContext.startService(intent);
            }
            stopService = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Reconnect stomp
     *
     * @return
     */
    public void reConnection() {
        if (mContext == null || mConfig == null) {
            return;
        }
        //Disconnect first
        // disconnect();
        //Reset
        boolean b = init(mContext, mConfig);
        if (b) {
            Log.i(TAG, "Stomp reconnection is in progress");
            openConnection(connectionListener);
        }
    }

    /**
     * Subscribe to p2p, get it from the configuration information when no url is specified
     *
     * @return
     */
    public StompProvider subscriber() {
        if (mConfig != null && mConfig.getTopicUrl() != null) {
            String[] urls = (String[]) mConfig.getTopicUrl().toArray();
            return subscriber(urls);
        }
        return this;
    }

    /**
     * Subscribe to p2p
     *
     * @param topicUrl
     * @return
     */
    public StompProvider subscriber(String... topicUrl) {
        if (null == topicUrl || topicUrl.length == 0) {
            Log.i(TAG, "P2p subscription address is empty --");
            return this;
        }
        mConfig.topicUrl(topicUrl);
        for (String url : topicUrl) {
            Log.i(TAG, "P2P subscription:" + url);
            Disposable dispCast = mStompClient.topic(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(topicMessage -> {
                        Log.i(TAG, topicMessage.getPayload());
                        if (null != messageListener) {
                            messageListener.onP2PMessage(topicMessage.getPayload(), url);
                        }
                    });
            compositeDisposable.add(dispCast);
        }
        return this;
    }

    public StompProvider subscriberBroadcast() {
        if (mConfig != null && mConfig.getTopicBroadcastUrl() != null) {
            String[] urls = (String[]) mConfig.getTopicBroadcastUrl().toArray();
            return subscriberBroadcast(urls);
        }
        return this;
    }

    /**
     * Subscribe to the broadcast
     *
     * @param broadCast
     * @return
     */
    public StompProvider subscriberBroadcast(String... broadCast) {
        if (null == broadCast || broadCast.length == 0) {
            Log.i(TAG, "Broadcast subscription address is empty--");
            return this;
        }
        mConfig.broadcastUrl(broadCast);
        for (String url : broadCast) {
            Disposable dispTopic = mStompClient.topic(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(broadcastTopicMessage -> {
                        Log.d(TAG, "broadcastMessage: " + broadcastTopicMessage.getPayload());
                        if (messageListener != null) {
                            messageListener.onBroadcastMessage(broadcastTopicMessage.getPayload(), url);
                        }
                    });
            compositeDisposable.add(dispTopic);
        }
        return this;
    }

    /**
     * Stop stomp service
     */
    private void stopStompService() {
        if (mContext == null) {
            return;
        }
        try {
            Intent intent = new Intent(mContext, StompService.class);
            mContext.stopService(intent);
            stopService = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Connect to the stomp service
     *
     * @param connectionListener
     */
    protected void connect(OnStompConnectionListener connectionListener) {
        connect(connectionListener, null);
    }

    /**
     * Open link
     */
    protected void connect(OnStompConnectionListener callback, List<StompHeader> headers) {
        if (mStompClient == null) {
            return;
        }
        if (headers != null && headers.size() > 0) {
            mStompClient.connect(headers);
        } else {
            mStompClient.connect();
        }

        if (mStompClient.isConnected()) {
            Log.i(TAG, "Stomp link is open, no need to reconnect");
            return;
        }
        try {
            Disposable dispLifecycle = mStompClient.lifecycle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lifecycleEvent -> {
                        switch (lifecycleEvent.getType()) {
                            case OPENED:
                                Log.i(TAG, "Stomp link opens");
                                callback.onConnectionOpened();
                                if (connectionListener != null) {
                                    connectionListener.onConnectionOpened();
                                }
                                break;
                            case ERROR:
                                Log.e(TAG, "Stomp connection error" + lifecycleEvent.getException());
                                String error = "Stomp error " + (lifecycleEvent.getException() == null ? "" : lifecycleEvent.getException().toString());
                                callback.onConnectionError(error);
                                if (connectionListener != null) {
                                    connectionListener.onConnectionError(error);
                                }
                                break;
                            case CLOSED:
                                Log.e(TAG, "Stomp connection is closed");
                                callback.onConnectionClosed();
                                if (connectionListener != null) {
                                    connectionListener.onConnectionClosed();
                                }
                                break;
                        }
                    });

            compositeDisposable.add(dispLifecycle);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onConnectionError(e.getMessage());
            if (connectionListener != null) {
                connectionListener.onConnectionError(e.getMessage());
            }
        }
    }


    /**
     * Disconnect link
     */
    private void disconnect() {
        if (mStompClient != null) {
            mStompClient.disconnect();
            stopStompService();
            mStompClient = null;
        }
    }

    public boolean isConnecting() {
        if (mStompClient == null) {
            return false;
        }
        return mStompClient.isConnected();
    }

    /**
     * Destroy related resources
     */
    public void destroy() {
        disconnect();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        messageListener = null;
        globalSendStatusListener = null;
        connectionListener = null;
        mConfig = null;
    }

    /**
     * Unified message listener registration entry
     *
     * @param listener
     */
    public StompProvider setOnMessageListener(OnMessageListener listener) {
        if (listener == null) {
            Log.d(TAG, "OnMessageListener is null");
            return this;
        }
        messageListener = listener;
        return this;
    }

    /**
     * Register global send listener
     *
     * @param listener
     */
    public StompProvider setOnMessageSendListener(OnMessageSendListener listener) {
        if (listener == null) {
            Log.d(TAG, "registerStompGlobalSenderListener: listener is null");
            return this;
        }
        globalSendStatusListener = listener;
        return this;
    }


    /**
     * Send a message
     *
     * @param jsonMsg 消息文本
     */
    public void sendMessage(String jsonMsg) {
        sendMessage(jsonMsg, null);
    }

    /**
     * Send a message with a custom header
     *
     * @param jsonMsg
     * @param header
     */
    public void sendMessage(String jsonMsg, Map<String, String> header) {
        List<StompHeader> stompHeaders = new ArrayList<>();
        StompHeader defaultHeader = new StompHeader(StompHeader.DESTINATION, mConfig.getSendUrl());
        stompHeaders.add(defaultHeader);

        //If there are custom headers, add them one by one.
        if (header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                stompHeaders.add(new StompHeader(entry.getKey(), entry.getValue()));
            }
        }
        //Construct a stomp message body
        StompMessage message = new StompMessage(StompCommand.SEND, stompHeaders, jsonMsg);

        sendMessage(message);
    }

    /**
     * Send a message and callback listener
     *
     * @param sender
     */
    private void sendMessage(StompMessage sender) {
        compositeDisposable.add(mStompClient.send(sender)
                .compose(applySchedulers())
                .subscribe(() -> {
                    Log.d(TAG, "Stomp消息发送成功" + sender.getPayload());
                    handleSendResultMessage(StompConfig.STOMP_SEND_SUCCESS, sender);
                }, throwable -> {
                    Log.e(TAG, "Stomp message failed to be sent", throwable);
                    handleSendResultMessage(StompConfig.STOMP_SEND_FAIL, sender);
                }));
    }


    /**
     * Handling sent message callback results
     *
     * @param status 发送消息状态
     * @param sender 发送的消息
     */
    private void handleSendResultMessage(int status, StompMessage sender) {
        //Global peer-to-peer send listener
        if (null != globalSendStatusListener) {
            globalSendStatusListener.onSendMessage(status, sender.getPayload(), status == StompConfig.STOMP_SEND_SUCCESS ? "发送成功" : "发送失败");
        }
    }

    /**
     * Get the content of the message from stompMessage
     *
     * @param message
     * @return
     */
    public UserMessageEntry parseStompMessage(StompMessage message) {
        if (message == null) {
            return null;
        }

        String content = message.getPayload();
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        UserMessageEntry msg = new Gson().fromJson(content, UserMessageEntry.class);
        String createTime = msg.getCreateTime();
        if (createTime != null) {
            //Determine if it is a pure number
            boolean isNumber = createTime.matches("^\\d+$");
            if (isNumber) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                msg.setCreateTime(sdf.format(new Date(Long.parseLong(createTime))));
            }
        }

        return msg;
    }

    private CompletableTransformer applySchedulers() {
        return upstream -> upstream
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
