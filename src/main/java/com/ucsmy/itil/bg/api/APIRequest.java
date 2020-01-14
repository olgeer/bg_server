package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;

import java.util.Properties;

/**
 * Created by Max on 2016/12/22.
 */
public class APIRequest {
    private String resoure;
    private String method;
    private Properties parameters;
    private String token;
    private String username;
    private String ip;
    private String traceid;
    private int port;

    public APIRequest() {
    }

    public String getTraceid() {
        return traceid;
    }

    public void setTraceid(String traceid) {
        this.traceid = traceid;
    }

    public String getResoure() {
        return resoure;
    }

    public void setResoure(String resoure) {
        this.resoure = resoure;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Properties getParameters() {
        return parameters;
    }

    public void setParameters(Properties parameters) {
        if (parameters != null) {
            if (parameters.getProperty("_username") != null) {
                this.username = parameters.getProperty("_username");
                parameters.remove("_username");
            }
            if (parameters.getProperty("_action") != null) {
                this.method = parameters.getProperty("_action");
                parameters.remove("_action");
            }
            if (parameters.getProperty("_token") != null) {
                this.token = parameters.getProperty("_token");
                parameters.remove("_token");
            }
            this.parameters = parameters;
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
