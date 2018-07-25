package com.phicomm.smarthome.statusmgr.controller;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.phicomm.smarthome.consts.PhihomeConst.ResponseStatus;
import com.phicomm.smarthome.phihome.model.PhiHomeBaseResponse;
import com.phicomm.smarthome.statusmgr.biz.MqttDeviceOnlineManager;

/**
 * 
 * package: com.phicomm.smarthome.statusmgr.controller
 * class: MqttDeviceOnlineController.java
 * date: 2018年6月28日 上午11:16:15
 * author: wen.xia
 * description:
 */
@RestController
public class MqttDeviceOnlineController extends BaseController {

    @Autowired
    private MqttDeviceOnlineManager deviceOnlineManager;

    @RequestMapping(value = "/device/status/clearBroker", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> clearBroker(String broker) {
        try {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call clearBroker, broker=%s", broker));
            }
            deviceOnlineManager.clearBroker(broker);
            return successResponse(new PhiHomeBaseResponse<>());
        }catch(Exception e) {
            LOGGER.error("clearBroker error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/isOnline", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> isOnline(String deviceId) {
        try {
            long start = System.currentTimeMillis();
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            pbr.setResult(deviceOnlineManager.isOnlineWithReadCache(deviceId));
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call isOnline[%s ms], deviceId=%s, isOnline=%s"
                        , System.currentTimeMillis() - start ,deviceId, pbr.getResult()));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("isOnline error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/isOnlines", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> isOnlines(String[] deviceIds) {
        try {
            long start = System.currentTimeMillis();
            if(deviceIds == null || deviceIds.length == 0 || deviceIds.length > 100 ) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            pbr.setResult(deviceOnlineManager.isOnlines(Arrays.asList(deviceIds)));
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call isOnlines[%s ms], deviceIds=%s, isOnlines=%s"
                        , System.currentTimeMillis() - start, Arrays.toString(deviceIds), pbr.getResult()));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("isOnlines error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/onlineAll", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> onlineAll() {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            pbr.setResult(deviceOnlineManager.getOnlineData());
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call onlineAll, onlineAll=%s", pbr.getResult()));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("onlineAll error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/readCache", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> readCache() {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            pbr.setResult(deviceOnlineManager.getOnlineReadCache());
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call readCache, readCache=%s", pbr.getResult()));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("readCache error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/brokers", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> brokers() {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            pbr.setResult(deviceOnlineManager.getBrokers());
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call brokers, brokers result=%s", pbr.getResult()));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("brokers error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    
    @RequestMapping(value = "/device/status/putCacheHash", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> putCacheHash(String key, String subKey, Long val) {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.put(key, subKey, val);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call putCacheHash, key=%s, subKey=%s, val=%s", key, subKey, val));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("putCacheHash error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/addBroker", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> addBroker(String broker, Long version) {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.addBroker(broker, version);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call addBroker, broker=%s, version=%s", broker, version));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("addBroker error ", e);
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/testOnlineDevice", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> testOnlineDevice(String broker, String deviceId) {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.deviceOnline(broker, deviceId);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call testOnlineDevice, broker=%s, deviceId=%s", broker, deviceId));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("addDevice error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/testOfflineDevice", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> testOfflineDevice(String broker, String deviceId) {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.deviceOffline(broker, deviceId);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("call testOfflineDevice, broker=%s, deviceId=%s", broker, deviceId));
            }
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("addDevice error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/testReset", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> testReset() {
        try {
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.reset();
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("testReset error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }
    
    @RequestMapping(value = "/device/status/resetPrefix", method = RequestMethod.GET, produces = { "application/json" })
    public PhiHomeBaseResponse<Object> resetPrefix(String prefix) {
        try {
            if(StringUtils.isBlank(prefix)) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
            PhiHomeBaseResponse<Boolean> pbr = new PhiHomeBaseResponse<>();
            deviceOnlineManager.reset(prefix);
            return successResponse(pbr);
        }catch(Exception e) {
            LOGGER.error("testReset error ", e);
            if(e instanceof IllegalArgumentException) {
                return errorResponse(ResponseStatus.STATUS_INVAID_PARA);
            }
        }
        return errorResponse(ResponseStatus.STATUS_COMMON_FAILED);
    }

}
