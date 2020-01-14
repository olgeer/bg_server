package com.ucsmy.itil.bg.service;

import com.alibaba.fastjson.JSONObject;
import com.sword.nio.Config;
import com.sword.nio.NIOProcess;
import com.ucsmy.itil.bg.common.Configure;
import com.ucsmy.itil.bg.common.Util;

import java.sql.Connection;

/**
 * @author Max
 * Created by Max on 2017/2/27.
 */
public class SystemLog {
    private static boolean LOG_ON_DB = false;
    private static String LOG_LIST_NAME = "system_log";
    private static Connection conn = null;

    static {
        Config cfg = new Config();

        cfg.setWriteToFile(Configure.getConfig().getProperty("nio.writeToFile") == null || Boolean.parseBoolean(Configure.getConfig().getProperty("nio.writeToFile")));
        //cfg.setCommandQueue("Cmd_Queue");
        if (Configure.getConfig().getProperty("nio.cacheDataPath") != null) {
            cfg.setCacheDataPath(Configure.getConfig().getProperty("nio.cacheDataPath"));
        }
        if (Configure.getConfig().getProperty("nio.interval") != null) {
            cfg.setInterval(Integer.parseInt(Configure.getConfig().getProperty("nio.interval")));
        }
        if (Configure.getConfig().getProperty("nio.writeInterval") != null) {
            cfg.setWriteInterval(Integer.parseInt(Configure.getConfig().getProperty("nio.writeInterval")));
        }
        if (Configure.getConfig().getProperty("nio.sleepInterval") != null) {
            cfg.setSleepInterval(Integer.parseInt(Configure.getConfig().getProperty("nio.sleepInterval")));
        }
        NIOProcess.init(cfg);
        NIOProcess.registerMessage(LOG_LIST_NAME, LogProcesser.class);

    }

    public static void setLogOnDb(boolean logOnDb) {
        LOG_ON_DB = logOnDb;
    }

    public static void log(String account, String action, String tableName, String sql, String values){
        log(account,action,tableName,sql,values,"");
    }

    public static void log(String account, String action, String tableName, String sql, String values,String errorMsg) {
        JSONObject msg = new JSONObject();
        msg.put("account", account);
        msg.put("action", action);
        msg.put("tablename", tableName);
        msg.put("sql", sql);
        msg.put("values", values);
        msg.put("logtime", Util.now());
        msg.put("errormsg", errorMsg);
        NIOProcess.putMessage(LOG_LIST_NAME, msg);
    }
}
