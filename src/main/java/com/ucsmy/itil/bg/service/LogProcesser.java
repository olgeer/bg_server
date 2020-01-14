package com.ucsmy.itil.bg.service;

import com.alibaba.fastjson.JSONObject;
import com.sword.nio.Proecsser;
import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Max on 2017/10/24.
 */
public class LogProcesser implements Proecsser {
    private static boolean LOG_ON_DB = true;
    private static String LOG_LIST_NAME = "system_log";
    private static Connection conn = null;

    @Override
    public boolean init() {
        conn = ItilDataSource.newInstance().getConn();
        return true;
    }

    @Override
    public void quit() {
        Util.safeClose(conn);
    }

    @Override
    public boolean process(Object log) {
        JSONObject logO = (JSONObject) log;
        //String now = Util.now();
        boolean retValue = true;
        if (LOG_ON_DB) {
            try {
                PreparedStatement ps = conn.prepareStatement("insert into glob_sql_log (account,tablename,action,sqlstring,value,logtime,errormsg) values(?,?,?,?,?,?,?)");
                ps.setObject(1, logO.getString("account"));
                ps.setObject(2, logO.getString("tablename"));
                ps.setObject(3, logO.getString("action"));
                ps.setObject(4, logO.getString("sql"));
                ps.setObject(5, logO.getString("values"));
                ps.setObject(6, logO.getString("logtime"));
                ps.setObject(7, logO.getString("errormsg"));
                ps.execute();
            } catch (SQLException se) {
                //logger.error(se.getMessage());
                System.out.println("LogProcesser ERROR:" + se.getMessage());
                se.printStackTrace();
                Util.safeClose(conn);
                conn = ItilDataSource.newInstance().getConn();
                retValue = false;
            } finally {
                //Util.safeClose(conn);
            }
        }
        return retValue;
    }
}
