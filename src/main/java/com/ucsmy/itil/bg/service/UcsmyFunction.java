package com.ucsmy.itil.bg.service;

import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.TableInfo;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Max on 2017/5/24.
 */
public class UcsmyFunction {
    private static Logger logger = LogManager.getLogger(UcsmyFunction.class.getName());

    public static String Func(String funcDef, TableInfo tableInfo) {
        String retString = null;
        if (funcDef.startsWith("func:")) {
            //logger.info("Table function [" + funcDef + "]");
            String func = funcDef.substring(5);
            String funcName = func.substring(0, func.indexOf('('));
            String paramStr = func.substring(func.indexOf('(') + 1, func.indexOf(')'));
            //logger.info("funcName:" + funcName + "(\"" + tableInfo.getColumnByName(paramStr).getTempValue() + "\")");

            switch (funcName) {
                case "flow_task_count":
                    retString = flow_task_count(paramStr, tableInfo);
                    break;
                case "work_date_diff":
                    retString = work_date_diff(paramStr, tableInfo);
                    break;
            }
        }
        return retString;
    }

    private static String flow_task_count(String paramStr, TableInfo tableInfo) {
        String retString = "0";
        if (paramStr != null) {
            Connection conn = ItilDataSource.newInstance().getConn();
            CommonDAO commonDAO = new CommonDAO("view_activiti_task_hi", conn);
            ResultSet rs = null;
            rs = commonDAO.selectBySql("select count(1) as task_count from (" +
                    "select * from view_activiti_task_hi " +
                    "where process_definition_id='" + tableInfo.getColumnByName(paramStr).getTempValue() + "' " +
                    "GROUP BY task_def_key) as tasks");
            try {
                if (rs.next()) retString = rs.getString(1);
                rs.close();
            } catch (SQLException e) {
                //e.printStackTrace();
            } finally {
                Util.safeClose(conn);
            }
        }
        return retString;
    }

    private static String work_date_diff(String paramStr, TableInfo tableInfo) {
        String retString = "0";
        String[] params = paramStr.split(",");
        if (params.length > 1) {
            retString = String.valueOf(Util.workDateDiff((String) tableInfo.getColumnByName(params[0]).getTempValue(), (String) tableInfo.getColumnByName(params[1]).getTempValue()));
        }
        return retString;
    }

    public static void main(String[] args) {
        System.out.println(Util.workDateDiff("2017-05-24 17:05:48", "2017-05-24 19:45:08"));
        System.out.println("timestamp:" + Calendar.getInstance().getTimeInMillis());
    }
}
