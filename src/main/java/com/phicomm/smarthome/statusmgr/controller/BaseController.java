package com.phicomm.smarthome.statusmgr.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.phicomm.smarthome.consts.PhihomeConst.ResponseStatus;
import com.phicomm.smarthome.phihome.model.PhiHomeBaseResponse;
import com.phicomm.smarthome.util.MyResponseutils;
import com.phicomm.smarthome.util.StringUtil;

/**
 * 
 * package: com.phicomm.smarthome.statusmgr.controller
 * class: BaseController.java
 * date: 2018年6月28日 上午11:15:58
 * author: wen.xia
 * description:
 */
public abstract class BaseController {
    
    protected final Logger LOGGER = LogManager.getLogger(this.getClass());

    public static PhiHomeBaseResponse<Object> geResponse(Object result) {
        PhiHomeBaseResponse<Object> smartHomeResponseT = new PhiHomeBaseResponse<Object>();
        smartHomeResponseT.setResult(result);
        return smartHomeResponseT;
    }

    protected PhiHomeBaseResponse<Object> errorResponse(int errCode) {
        String errMsg = MyResponseutils.parseMsg(errCode);
        return errorResponse(errCode, errMsg);
    }

    protected PhiHomeBaseResponse<Object> errorResponse(int errCode, String errMsg) {
        PhiHomeBaseResponse<Object> response = geResponse(null);
        response.setCode(errCode);
        if (StringUtil.isNullOrEmpty(errMsg)) {
            response.setMessage(MyResponseutils.parseMsg(errCode));
        } else {
            response.setMessage(errMsg);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    protected PhiHomeBaseResponse<Object> successResponse(Object obj) {
        PhiHomeBaseResponse<Object> response = (PhiHomeBaseResponse<Object>) obj;
        response.setCode(ResponseStatus.STATUS_OK);
        response.setMessage(MyResponseutils.parseMsg(ResponseStatus.STATUS_OK));
        return response;
    }
    
    protected String getIpAdrress(HttpServletRequest request) {
        String Xip = request.getHeader("X-Real-IP");
        String XFor = request.getHeader("X-Forwarded-For");
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if(index != -1){
                return XFor.substring(0,index);
            }else{
                return XFor;
            }
        }
        XFor = Xip;
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getRemoteAddr();
        }
        return XFor;
    }
    
    
}
