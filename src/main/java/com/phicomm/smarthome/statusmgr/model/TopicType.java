package com.phicomm.smarthome.statusmgr.model;


public enum TopicType {
    TYPE_CLIENT_BIND,//客户端发起绑定
    TYPE_CLIENT_UNBIND, //客户端发起解绑
    TYPE_BROKER_CONNECTED, //服务器通知设备上线
    TYPE_BROKER_DISCONNECTED, //服务器通知设备掉线
    TYPE_CLIENT_TIMESYNC, //设备请求对时
    TYPE_CLIENT_UPGRADED, //设备反馈升级结果
    TYPE_CLIENT_NOTIFY, //设备发送消息通知
    TYPE_CLIENT_UPDATE_SHADOW, // 更新shadow的topic $phihome/shadow/+/%s/+/update
    TYPE_CLIENT_GET_SHADOW, // 获取shadow状态 $phihome/shadow/+/%s/+/get
    TYPE_CLIENT_POWER_UPLOAD, //device/<DEVICE_ID>/power/comsumption/upload
    TYPE_CLIENT_FEEDBACK,
    TYPE_DEFAULT;
}
