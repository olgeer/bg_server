package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.common.Util;

import java.util.Date;

/**
 * Created by Max on 2016/12/22.
 */
public class APIResponse {
    private String token;
    private String returnCode;
    private Object returnData;
    private Date responseTime = new Date();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    public Object getReturnData() {
        return returnData;
    }

    public void setReturnData(Object returnData) {
        this.returnData = returnData;
    }

    @Override
    public String toString() {
        String quot = "\"";
        if (returnData instanceof String) quot = "";
        return "{\"returncode\":\"" + returnCode + "\",\"token\":\"" + token + "\",\"responsetime\":\"" + Util.formatDateTime(responseTime) + "\",\"returndata\":" + returnData + "}";
    }
}
