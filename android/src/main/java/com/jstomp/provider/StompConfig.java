package com.jstomp.provider;

import android.annotation.SuppressLint;
import android.util.ArraySet;

import java.util.Arrays;
import java.util.Set;

/**
 * @company Shanghai Daoqiao Information Technology-->
 * @anthor created by jingzhanwu
 * @date 2018/1/23
 * @change
 * @describe describe
 * Stomp configuration information
 **/
@SuppressLint("NewApi")
public class StompConfig {
    /*Successful operation*/
    public static final int STOMP_SEND_SUCCESS = 1;
    /*operation failed*/
    public static final int STOMP_SEND_FAIL = 0;
    /*Initialize the endpoint url*/
    private String url;
    /*Send message url*/
    private String sendUrl;
    /*Subscribe to the broadcast address*/
    private Set<String> topicBroadCast = new ArraySet<>();
    /*Subscribe to a peer-to-peer address*/
    private Set<String> topic = new ArraySet<>();


    public StompConfig(String url, String sendURL) {
        this.url = url;
        this.sendUrl = sendURL;
    }


    /**
     * P2p subscription address, multiple
     *
     * @param topicUrl
     * @return
     */

    public StompConfig topicUrl(String... topicUrl) {
        this.topic.addAll(Arrays.asList(topicUrl));
        return this;
    }

    /**
     * Broadcast subscription address multiple
     *
     * @param broadcastUrl
     * @return
     */
    public StompConfig broadcastUrl(String... broadcastUrl) {
        this.topicBroadCast.addAll(Arrays.asList(broadcastUrl));
        return this;
    }

    /**
     * Return send url
     *
     * @return
     */
    public String getSendUrl() {
        return this.sendUrl;
    }

    /**
     * Return to peer-to-peer subscription address
     *
     * @return
     */
    public Set<String> getTopicUrl() {
        return topic;
    }

    /**
     * Return the address of the broadcast subscription
     *
     * @return
     */
    public Set<String> getTopicBroadcastUrl() {
        return topicBroadCast;
    }


    /**
     * Returns the url for final initialization
     *
     * @return
     */
    public String connectionUrl() {
        return url;
    }
}
