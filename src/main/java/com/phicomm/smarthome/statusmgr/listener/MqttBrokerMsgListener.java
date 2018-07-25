package com.phicomm.smarthome.statusmgr.listener;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.phicomm.smarthome.cache.Cache;
import com.phicomm.smarthome.statusmgr.StatusMgrMain;
import com.phicomm.smarthome.statusmgr.model.MqttMsg;
import com.phicomm.smarthome.statusmgr.model.RocketmqConfig;
import com.phicomm.smarthome.statusmgr.model.Topic;
import com.phicomm.smarthome.statusmgr.model.TopicType;
import com.phicomm.smarthome.statusmgr.msghandler.MqttBrokerMsgHandler;
import com.phicomm.smarthome.statusmgr.util.StringUtil;

/**
 * 
 * package: com.phicomm.smarthome.statusmgr.listener
 * class: MqttBrokerMsgListener.java
 * date: 2018年6月28日 上午11:16:23
 * author: wen.xia
 * description:
 */
@Component
public class MqttBrokerMsgListener {
    private static Logger logger = LogManager.getLogger(MqttBrokerMsgListener.class);

    //消息处理最大延迟时间
    private static final long MESSAGE_MAX_COMSUME_TIME_DELAY = 3 * 60 * 1000;

    /** 实体对象. */
    public static MqttBrokerMsgListener instance;

    @Autowired
    private Cache cache;

    @Autowired
    public RedisTemplate<String, String> redisTemplate;

    private static final String KEY_ROCKETMQ_MESSAGEIDS = "key_from_mqtt_mq_messageids";
    private static final int MAX_MESSAGES = 2000;

    private static final Topic TOPIC_BROKER_CONNECTED = new Topic("$events/broker/+/connected");
    private static final Topic TOPIC_BROKER_DISCONNECTED = new Topic("$events/broker/+/disconnected");
    //mqtt请求id字段名称
    public static final String REQUEST_ID = "client_token";

    /**
     * 自动调用的初始化方法.
     */
    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 初始化入口.
     *
     * @see StatusMgrMain
     */
    public void start() {
        logger.debug("Base mqtt listener init starting...");
        MqttBrokerMsgHandler.instance.onStart();
        new Thread(this::startOrderReadMqttRedisChannel).start();
    }

    private boolean handleOnpublishMessage(String topic, String body, long storeTime) {
        logger.debug("base publish callback topic [{}]", topic);
        if (StringUtil.isNullOrEmpty(topic)) {
            logger.info("base callback topic is null");
            return true;
        }

        TopicType topicType = parseTopicType(topic);
        logger.info("parsed topicType [{}], topic [{}] body [{}]", topicType, topic, body);

        switch (topicType) {
            case TYPE_BROKER_CONNECTED:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                MqttBrokerMsgHandler.instance.onConnectTopic(topic, body);
                break;
            case TYPE_BROKER_DISCONNECTED:
                if (filterMsgBeforeDispatch(storeTime)) {
                    return true;
                }
                MqttBrokerMsgHandler.instance.onDisConnectTopic(topic, body);
                break;
            default:
                break;
        }
        return true;
    }

    private boolean filterMsgBeforeDispatch(long storeTimeStamp) {
        if (System.currentTimeMillis() - storeTimeStamp <= MESSAGE_MAX_COMSUME_TIME_DELAY) {
            return false;
        }
        logger.info("Drop message storeTimeStamp [{}]", storeTimeStamp);
        return true;
    }

    /**
     * 接收从mqtt服务器插件来有序的mqtt消息 { "topic":"", "body":"" }.
     */
    private void startOrderReadMqttRedisChannel() {
        /*
         * Instantiate with specified consumer group name.
         */
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("group_order_dispatch_from_mqtt");
        consumer.setNamesrvAddr(RocketmqConfig.getNameser());

        /*
         * Specify where to start in case the specified consumer group is a
         * brand new one.
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);

        /*
         * Subscribe one more more topics to consume.
         */
        try {
            consumer.subscribe("dispatch_order_mqtt_message", "TagA");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        /*
         * Register callback to execute on arrival of messages fetched from
         * brokers.
         */
        consumer.registerMessageListener(new MessageListenerOrderly() {

            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                if (msgs == null || msgs.isEmpty()) {
                    return ConsumeOrderlyStatus.SUCCESS;
                }

                for (MessageExt m : msgs) {
                    if (m == null || m.getBody() == null) {
                        continue;
                    }

                    logger.debug("Ordered message id [{}] ", m.getMsgId());
                    try {
                        if (isDupMessage(m.getMsgId())) {
                            logger.info("dup message id [{}]", m);
                            continue;
                        }

                        MqttMsg model = JSON.parseObject(m.getBody(), MqttMsg.class);
                    if(handleOnpublishMessage(model.getTopic(), model.getBody(), m.getBornTimestamp())) {
                        logger.debug("consume ok msg topic [{}], body [{}]", model.getTopic(), model.getBody());
                        putMsgIdIntoCache(m.getMsgId());
                    } else {
                        logger.info("consume failed msg topic [{}], body [{}]", model.getTopic(), model.getBody());
                    }
                    } catch (Exception e) {
                        logger.error("Order message [{}] dispatch failed", m, e);
                    }
                }

                return ConsumeOrderlyStatus.SUCCESS;
            }
        });

        /*
         * Launch the consumer instance.
         */
        try {
            consumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        logger.debug("command rocketmq reader Started");
    }

    private boolean isDupMessage(String messageId) {
        try {
            if (StringUtil.isNullOrEmpty(messageId)) {
                return false;
            }

            if (cache.isSetContains(KEY_ROCKETMQ_MESSAGEIDS, messageId)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void putMsgIdIntoCache(String messageId) {
        long size = cache.getSetSize(KEY_ROCKETMQ_MESSAGEIDS);
        if (size >= MAX_MESSAGES) {
            cache.popSetValue(KEY_ROCKETMQ_MESSAGEIDS);
        }

        cache.putSetValue(KEY_ROCKETMQ_MESSAGEIDS, messageId);
    }

    /**
     * 解析topic，根据topic字符串解析出对应的类型，以便于调用不同的处理类来处理.
     *
     * @param topic
     *            收到的消息主题
     * @return topic类型
     */
    private static TopicType parseTopicType(String topic) {
        Topic comingTopic = new Topic(topic);

        if (comingTopic.match(TOPIC_BROKER_CONNECTED)) {
            return TopicType.TYPE_BROKER_CONNECTED;
        } else if (comingTopic.match(TOPIC_BROKER_DISCONNECTED)) {
            return TopicType.TYPE_BROKER_DISCONNECTED;
        } 
        return TopicType.TYPE_DEFAULT;
    }
}
