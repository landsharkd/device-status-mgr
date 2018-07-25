package com.phicomm.smarthome.statusmgr.model;

/**
 * mqtt服务器传过来的消息
 * package: com.phicomm.smarthome.statusmgr.model
 * class: MqttMsg.java
 * date: 2018年6月28日 上午11:17:38
 * author: wen.xia
 * description:
 */
public class MqttMsg {
    private String topic;

    private String body;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
