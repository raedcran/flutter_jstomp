package com.jstomp;

import android.app.Activity;
import android.util.Log;

import com.jstomp.provider.StompConfig;
import com.jstomp.provider.StompProvider;
import com.jstomp.provider.UserMessageEntry;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * JstompPlugin
 *
 * @anthor created by jingzhanwu
 * @date 2019-06-24
 * @change
 * @describe Stomp Android plugin
 **/
public class JStompPlugin implements MethodCallHandler {
    private Activity activity;

    private MethodChannel channel;

    public JStompPlugin(Activity act, MethodChannel channel) {
        this.activity = act;
        this.channel = channel;
        handlerRxError();
    }

    /**
     * Rxjava error handling
     */
    private void handlerRxError() {
        //Unified processing of Rxjava error
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e("JStompPlugin--", throwable.getMessage());
            }
        });
    }


    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "jstomp");
        channel.setMethodCallHandler(new JStompPlugin(registrar.activity(), channel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String method = call.method;
        try {
            switch (method) {
                case FlutterCall.INIT://initialization
                    String url = call.argument("url");
                    String sendUrl = call.argument("sendUrl");
                    boolean b = init(url, sendUrl);
                    result.success(b);
                    break;
                case FlutterCall.DESTROY: //Destroy, disconnect
                    boolean d = destroy();
                    result.success(d);
                    break;
                case FlutterCall.CONNECTION://connection
                    boolean c = connection();
                    result.success(c);
                    break;
                case FlutterCall.SEND_MESSAGE: //Send a message
                    Map<String, String> header = null;
                    if (call.hasArgument("header")) {
                        header = (Map<String, String>) call.argument("header");
                    }
                    String str = sendMessage(call.argument("msg"), header);
                    result.success(str);
                    break;
                case FlutterCall.SUBSCRIBER_P2P://Subscribe to p2p
                    String[] urls = call.arguments.toString().split(",");
                    boolean s = subscriberP2P(urls);
                    result.success(s);
                    break;
                case FlutterCall.SUBSCRIBER_BROADCAST: //Subscribe to the broadcast
                    String[] burls = call.arguments.toString().split(",");
                    boolean sb = subscriberBroadcast(burls);
                    result.success(sb);
                    break;
                case FlutterCall.MESSAGE_CALLBACK: //Set message callback
                    boolean sm = setMessageCallback();
                    result.success(sm);
                    break;
                case FlutterCall.SEND_CALLBACK: //Set the send callback
                    boolean ss = setSendCallback();
                    result.success(ss);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            result.success(Boolean.FALSE);
        }
    }


    /**
     * Stomp initialization
     *
     * @param url
     * @param sendUrl
     */
    private boolean init(String url, String sendUrl) {
        try {
            return StompProvider.get().init(activity.getApplicationContext(), new StompConfig(url, sendUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Resource destruction
     *
     * @return
     */
    private boolean destroy() {
        try {
            StompProvider.get().destroy();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Open connection
     */
    private boolean connection() {
        try {
            StompProvider.get().openConnection(new StompProvider.OnStompConnectionListener() {
                @Override
                public void onConnectionOpened() {
                    //Connect open, notify flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_OPENED, Boolean.TRUE);
                }

                @Override
                public void onConnectionError(String error) {
                    //Connection error, notify flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_ERROR, error);
                }

                @Override
                public void onConnectionClosed() {
                    //Connection closed, notify flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_CLOSED, Boolean.FALSE);
                }
            });
            return true;
        } catch (Exception e) {
            //Connection error, notify flutter
            channel.invokeMethod(CallFlutter.ON_CONNECTION_ERROR, e.getMessage());
            return false;
        }
    }

    /**
     * Subscribe to p2p
     *
     * @param url
     */
    private boolean subscriberP2P(String[] url) {
        try {
            StompProvider.get().subscriber(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Subscribe to the broadcast
     *
     * @param url
     */
    private boolean subscriberBroadcast(String[] url) {
        try {
            StompProvider.get().subscriberBroadcast(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send a message
     *
     * @param message Must be a json string
     * @return
     */
    private String sendMessage(String message, Map<String, String> header) {
        try {
            StompProvider.get().sendMessage(message, header);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Message listener
     *
     * @return
     */
    private boolean setMessageCallback() {
        try {
            StompProvider.get().setOnMessageListener(new StompProvider.OnMessageListener() {
                @Override
                public void onBroadcastMessage(String stompMsg, String topicUrl) {
                    channel.invokeMethod(CallFlutter.ON_BROAD_CAST, stompMsg);
                }

                @Override
                public void onP2PMessage(String stompMsg, String topicUrl) {
                    channel.invokeMethod(CallFlutter.ON_MESSAGE, stompMsg);
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add send success, failure listener
     *
     * @return
     */
    private boolean setSendCallback() {
        try {
            StompProvider.get().setOnMessageSendListener(new StompProvider.OnMessageSendListener() {
                @Override
                public void onSendMessage(int status, String userMsg, String tipsMsg) {
                    Map<String, Object> map = new HashMap();
                    map.put("msg", userMsg);
                    map.put("status", status);
                    channel.invokeMethod(CallFlutter.ON_SEND, map);
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Define the method that flutter calls native
     */
    class FlutterCall {
        static final String INIT = "init";
        static final String CONNECTION = "connection";
        static final String SUBSCRIBER_P2P = "subscriberP2P";
        static final String SUBSCRIBER_BROADCAST = "subscriberBroadcast";
        static final String DESTROY = "destroy";
        static final String SEND_MESSAGE = "sendMessage";

        static final String MESSAGE_CALLBACK = "setMessageCallback";
        static final String SEND_CALLBACK = "setSendCallback";
    }

    /**
     * Define the method of native calling flutter
     */
    class CallFlutter {
        static final String ON_CONNECTION_OPENED = "onConnectionOpen";
        static final String ON_CONNECTION_ERROR = "onConnectionError";
        static final String ON_CONNECTION_CLOSED = "onConnectionClosed";

        static final String ON_MESSAGE = "onMessage";
        static final String ON_BROAD_CAST = "onBroadcastMessage";
        static final String ON_SEND = "onSend";
    }

    /**
     * Convert stomp messages to map
     *
     * @param userMsg
     * @return
     */
    private Map<String, Object> parserMsg(UserMessageEntry userMsg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", userMsg.getId());
        map.put("content", userMsg.getContent());
        map.put("createId", userMsg.getCreateId());
        map.put("microGroupName", userMsg.getMicroGroupName());
        map.put("createName", userMsg.getCreateName());
        map.put("createTime", userMsg.getCreateTime());
        map.put("headUrl", userMsg.getHeadUrl());
        map.put("microGroupId", userMsg.getMicroGroupId());
        map.put("path", userMsg.getPath());
        map.put("localPath", userMsg.getLocalPath());
        map.put("obj", userMsg.getObj());
        map.put("type", userMsg.getType());

        map.put("sendState", userMsg.getStatus());//send status
        map.put("direct", 1);//accept
        map.put("status", 1);//State, success
        map.put("isCrowd", 0);
        return map;
    }
}
