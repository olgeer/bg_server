package com.ucsmy.itil.bg.service;

import com.ucsmy.itil.bg.api.APIRequest;
import com.ucsmy.itil.bg.api.APIResponse;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Max on 2017/4/25.
 */
public class Report {
    private Connection connection;
    private int port;
    private static Logger logger = LogManager.getLogger(Report.class.getName());

    public Report(Connection connection, int port) {
        this.connection = connection;
        this.port = port;
    }

    public String report(APIRequest params) {
        String retString = "";
        String action = params.getMethod();
        String token = params.getToken();
        APIResponse apiResponse = new APIResponse();
        apiResponse.setToken(token);
        String username = params.getUsername();
        String ip = params.getIp();
        Properties paramsProperties = params.getParameters();
        String startDate = paramsProperties.getProperty("_start");
        String endDate = paramsProperties.getProperty("_end");
        String pagesize = paramsProperties.getProperty("_pagesize");
        String pageindex = paramsProperties.getProperty("_pageindex");
        String callback = paramsProperties.getProperty("_callback");
        String sortCol = paramsProperties.getProperty("_sortcol");
        String sortDir = paramsProperties.getProperty("_sortdir");
        String flowname = paramsProperties.getProperty("_flowname");
        String taskname = paramsProperties.getProperty("_taskname");
        String joincols = paramsProperties.getProperty("_joincols");
        paramsProperties = Util.removeCommonProperties(paramsProperties);

        if (startDate == null || startDate.length() == 0) {
            startDate = Util.getFirstDayOfWeek();
        }
        if (endDate == null || endDate.length() == 0) {
            endDate = Util.getLastDayOfWeek();
        }

        CommonDAO commonDAO = null;
        ResultSet rs = null;
        String sql = "";

        switch (action) {
            case "overview":    //流程总览
                commonDAO = new CommonDAO("report_flow_overview", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                rs = commonDAO.selectBySql("select (" +
                        "select count(1) from view_activiti_process_hi " +
                        "where " +
                        "process_name is not NULL and " +
                        "start_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                        ") as total_start," +
                        "(select count(1) from view_activiti_process_hi " +
                        "where " +
                        "process_name is not NULL and " +
                        "end_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                        ") as total_end," +
                        "(select count(1) from view_activiti_process_hi " +
                        "where " +
                        "process_name is not NULL and " +
                        "flow_status = 0" +
                        ") as total_running,'" + startDate + "','" + endDate + "'");
                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), 1, null));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            case "flowlist": {    //流程列表
                commonDAO = new CommonDAO("view_activiti_process_hi", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                CommonExample commonExample = new CommonExample();
                CommonExample.Criteria criteria = commonExample.createCriteria();
                criteria.andColumnIsNotNull("process_name");
                criteria.andColumnBetween("start_time", startDate, endDate);

                Iterator<Map.Entry<Object, Object>> it = paramsProperties.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Object, Object> entry = it.next();
                    String key = (String) entry.getKey();
                    //Object value = entry.getValue();
                    if (key != null) {
                        if (paramsProperties.getProperty(key).trim().length() > 0) {
                            String keyType = commonDAO.getTableInfo().getColumnType(key);
                            if (keyType != null && Util.isDigitalType(keyType)) {
                                criteria.andColumnEqualTo(key, paramsProperties.getProperty(key));
                            } else {
                                criteria.andColumnLike(key, paramsProperties.getProperty(key));
                            }
                        }
                    }
                }
                commonExample.setOrderByClause("start_time");
                int count = commonDAO.countByExample(commonExample);

                if (pagesize == null) {
                    pagesize = "10";
                }
                commonExample.setPageSize(Integer.parseInt(pagesize));
                if (pageindex == null) {
                    pageindex = "1";
                }
                commonExample.setPageIndex(Integer.parseInt(pageindex));
                if (sortCol != null && sortDir != null) {
                    commonExample.setOrderByClause(sortCol + " " + sortDir);
                }

                rs = commonDAO.selectByExample(commonExample);

                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), count, commonExample));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            }
            case "userflow": {
                //用户相关流程列表，默认排序方式为按flow_no,start_time顺序排序，默认分页大小为50
                //必填参数为_start、_end、username
                //_start为查询开始时间
                //_end为查询结束时间
                //username为查找的用户名
                commonDAO = new CommonDAO("view_activiti_task_flow", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                CommonExample commonExample = new CommonExample();
                CommonExample.Criteria criteria = commonExample.createCriteria();
                criteria.andColumnBetween("start_time", startDate, endDate);

                Iterator<Map.Entry<Object, Object>> it = paramsProperties.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Object, Object> entry = it.next();
                    String key = (String) entry.getKey();
                    //Object value = entry.getValue();
                    if (key != null) {
                        if (paramsProperties.getProperty(key).trim().length() > 0) {
                            String keyType = commonDAO.getTableInfo().getColumnType(key);
                            if (keyType != null && Util.isDigitalType(keyType)) {
                                criteria.andColumnEqualTo(key, paramsProperties.getProperty(key));
                            } else {
                                criteria.andColumnLike(key, paramsProperties.getProperty(key));
                            }
                        }
                    }
                }

                commonExample.setOrderByClause("flow_no,start_time");

                int count = commonDAO.countByExample(commonExample);

                if (pagesize == null) {
                    pagesize = "100";
                }
                commonExample.setPageSize(Integer.parseInt(pagesize));
                if (pageindex == null) {
                    pageindex = "1";
                }
                commonExample.setPageIndex(Integer.parseInt(pageindex));
                if (sortCol != null && sortDir != null) {
                    commonExample.setOrderByClause(sortCol + " " + sortDir);
                }

                rs = commonDAO.selectByExample(commonExample);

                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), count, commonExample));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            }
            case "flowstatistics":    //流程统计
                commonDAO = new CommonDAO("report_flow_statistics", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                if (flowname != null) {
                    sql = "select process_name,null as task_count,min(cost_time) as min_time,avg(cost_time) as avg_time," +
                            "max(cost_time) as max_time,count(1) as counter,process_definition_id from view_activiti_process_hi" +
                            " where" +
                            " start_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                            " and process_name='" + flowname + "'" +
                            " GROUP BY process_name order by process_name;";
                } else {
                    sql = "select process_name,null as task_count,min(cost_time) as min_time,avg(cost_time) as avg_time," +
                            "max(cost_time) as max_time,count(1) as counter,process_definition_id from view_activiti_process_hi" +
                            " where" +
                            " start_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                            " GROUP BY process_name order by process_name;";
                }
                System.out.println("report sql:" + sql);
                rs = commonDAO.selectBySql(sql);
                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), 1, null));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            case "taskstatistics":    //步骤分析
                commonDAO = new CommonDAO("report_task_statistics", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                if (flowname != null) {
                    sql = "select process_name,process_definition_id,task_name,task_def_key," +
                            "case when task_role_name is not null then task_role_name else task_usba_name end as task_role_name," +
                            "min(cost_time) as min_time,avg(cost_time) as avg_time,max(cost_time) as max_time from view_activiti_task_hi" +
                            " where process_name = '" + flowname + "'" +
                            " and start_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                            " GROUP BY task_def_key order by task_name";
                } else {
                    apiResponse.setReturnCode("501");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"没找到必要的流程名称！\"}");
                    break;
                }
                rs = commonDAO.selectBySql(sql);
                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), 1, null));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            case "taskprocesslist":    //步骤流程列表
                commonDAO = new CommonDAO("report_task_process_list", this.connection);
                commonDAO.setPort(this.port);
                commonDAO.setTraceid(params.getTraceid());

                if (flowname != null) {
                    sql = "select t.process_title as process_title,t.process_instance_id as process_instance_id,t.process_definition_id as process_definition_id,b.bpnu_id as flowno," +
                            "t.task_name as task_name,t.task_def_key as task_def_key," +
                            "case when t.task_usba_name is not null then t.task_usba_name else t.task_role_name end as task_role_name," +
                            "t.start_time as start_time,t.end_time as end_time,t.cost_time as cost_time from view_activiti_task_hi t" +
                            " join bpm_number b on t.process_instance_id = b.PROC_INST_ID_" +
                            " where t.process_name = '" + flowname + "'" +
                            " and t.start_time BETWEEN '" + startDate + "' and '" + endDate + "'" +
                            " and t.task_name='" + taskname + "' order by t.cost_time desc";
                } else {
                    apiResponse.setReturnCode("501");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"没找到必要的流程名称！\"}");
                    break;
                }
                rs = commonDAO.selectBySql(sql);
                if (rs != null) {
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(commonDAO.resultSet2Json(rs, null, commonDAO.getTableInfo(), 1, null));
                } else {
                    apiResponse.setReturnCode("500");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"表单查询错误！\"}");
                }
                break;
            default:
                apiResponse.setReturnCode("505");
                apiResponse.setReturnData("{\"result\":0,\"msg\":\"未知操作类型！\"}");
        }

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("[" + this.port + "]ERROR:" + e.getMessage());
            }
        }
        if (callback != null) {
            retString = callback + "(" + apiResponse.toString() + ");";
        } else {
            retString = apiResponse.toString();
        }
        return retString;
    }

}
