package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;

/**
 * Created by Max on 2017/2/16.
 */

public class Receiver {
    private String[] userids;
    private String[] roles;

    public String[] getUserids() {
        return userids;
    }

    public void setUserids(String[] userids) {
        this.userids = userids;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public String toString() {
        return JSON.toJSONString(this);
    }
}