package com.phicomm.smarthome.statusmgr.msghandler;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.phicomm.smarthome.statusmgr.biz.MqttDeviceOnlineManager;
import com.phicomm.smarthome.statusmgr.util.StringUtil;



/**
 * package: com.phicomm.smarthome.statusmgr.msghandler
 * class: MqttBrokerMsgHandler.java
 * date: 2018年6月28日 上午11:18:08
 * author: wen.xia
 * description:
 */
@Component
public class MqttBrokerMsgHandler {
    private static Logger logger = LogManager.getLogger(MqttBrokerMsgHandler.class);

    /**
     * 实体变量，初始化时使用.
     */
    public static MqttBrokerMsgHandler instance;

    @Autowired
    private MqttDeviceOnlineManager deviceOnlineManager;

    /**
     * 初始化方法，自动调用.
     */
    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * called when init.
     */
    public void onStart() {
    }

    /**
     * @param topic 消息主题
     * @param body 消息内容
     */
    public void onConnectTopic(String topic, String body) {
        logger.debug("onPublish callback topic:[{}] body:[{}]", topic, body);
        String deviceId = parseConnectDeviceId(topic);
        if (StringUtil.isNullOrEmpty(deviceId)) {
            logger.info("onDisconnect topic[{}] body is:[{}]", topic, body);
            return;
        }
        
        deviceOnlineManager.deviceOnline(body, deviceId);

    }

    /**
     * 设备断连时收到的消息(设备主动掉线和被动掉线).
     * @param topic 消息主题
     * @param body 消息内容
     */
    public void onDisConnectTopic(String topic, String body) {
        logger.debug("onPublish callback topic:[{}] body:[{}]", topic, body);
        // 设备状态,更新表状态
        String deviceId = parseConnectDeviceId(topic);
        if (StringUtil.isNullOrEmpty(deviceId)) {
            logger.info("onDisconnect topic[{}] body is:[{}]", topic, body);
            return;
        }
        
        deviceOnlineManager.deviceOffline(body, deviceId);
    }

    private String parseConnectDeviceId(String topic) {
        if (StringUtil.isNullOrEmpty(topic)) {
            return null;
        }
        String[] items = topic.split("/");
        if (items == null || items.length < 3) {
            return null;
        }
        return items[2];
    }
}
