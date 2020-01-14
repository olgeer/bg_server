package com.ucsmy.itil.bg.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.api.APIRequest;
import com.ucsmy.itil.bg.api.APIResponse;
import com.ucsmy.itil.bg.common.*;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Max on 2016/12/30.
 */
public class CommonObject {
    protected String objectTable;
    private Connection connection;
    private CommonDAO commonDAO;
    private int port;
    private UcsmyLog logger = new UcsmyLog(CommonObject.class.getName());

    public CommonObject(String objectTableName, Connection conn, int port) {
        this.objectTable = objectTableName;
        this.connection = conn;
        setPort(port);
        commonDAO = new CommonDAO(objectTable, conn);
        commonDAO.setPort(port);
    }

    public CommonObject(Connection conn, int port) {
        this.connection = conn;
        setPort(port);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        logger.setTrace(String.valueOf(this.port));
    }

    public String httpRequest(APIRequest params) {
        String retString = "";
        String action = params.getMethod();
        String token = params.getToken();
        String username = params.getUsername();
        String ip = params.getIp();
        Properties paramsProperties = params.getParameters();
        String tableName = paramsProperties.getProperty("_tablename");
        String selectCol = paramsProperties.getProperty("_selectcol");
        String pagesize = paramsProperties.getProperty("_pagesize");
        String pageindex = paramsProperties.getProperty("_pageindex");
        String callback = paramsProperties.getProperty("_callback");
        String jsvalue = paramsProperties.getProperty("_jsvalue");
        String sortCol = paramsProperties.getProperty("_sortcol");
        String sortDir = paramsProperties.getProperty("_sortdir");
        String groupCol = paramsProperties.getProperty("_groupcol");
        String joincols = paramsProperties.getProperty("_joincols");
        boolean tableinfo = paramsProperties.getProperty("_tableinfo")!=null?Boolean.valueOf(paramsProperties.getProperty("_tableinfo")):false;

        paramsProperties = Util.removeCommonProperties(paramsProperties);

        APIResponse apiResponse = new APIResponse();
        apiResponse.setToken(token);

        if (tableName == null || action == null) {
            apiResponse.setReturnCode("500");
            apiResponse.setReturnData("No necessary parameter like tablename or action !");
            retString = apiResponse.toString();
        } else {
            action = action.toLowerCase();
            CommonExample commonExample = null;
            CommonExample.Criteria criteria = null;
            Iterator<Map.Entry<Object, Object>> it = null;
            int result = 0;
            //Authentication authentication = null;
            try {
                switch (action) {
                    case "tableinfo": {
                        commonDAO = new CommonDAO(tableName, connection);
                        commonDAO.setPort(this.port);
                        commonDAO.setTraceid(params.getTraceid());

                        apiResponse.setReturnCode("200");
                        apiResponse.setReturnData(commonDAO.getTableInfo().toString());

                        break;
                    }
                    case "get": {
                        commonDAO = new CommonDAO(tableName, connection);
                        commonDAO.setPort(this.port);
                        commonDAO.setTraceid(params.getTraceid());

                        commonExample = new CommonExample();
                        criteria = commonExample.createCriteria();

                        it = paramsProperties.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Object, Object> entry = it.next();
                            String key = (String) entry.getKey();
                            //Object value = entry.getValue();
                            try {
                                if (key != null) {
                                    if (paramsProperties.getProperty(key).trim().length() > 0) {
                                        String keyType = commonDAO.getTableInfo().getColumnType(key);
                                        String keyValue = paramsProperties.getProperty(key);
                                        if (keyValue.contains("(") && keyValue.endsWith(")")) {
                                            keyValue = keyValue.substring(0, keyValue.length() - 1);
                                            String funcName = keyValue.substring(0, keyValue.indexOf('('));
                                            keyValue = keyValue.substring(keyValue.indexOf('(') + 1, keyValue.length());
                                            switch (funcName.toLowerCase()) {
                                                case "in": {
                                                    List<Object> strList = new ArrayList<Object>();
                                                    keyValue = Util.splitEncode(keyValue);
                                                    for (String inValue : keyValue.split(",")) {
                                                        if (Util.isDigitalType(keyType)) {
                                                            strList.add(Integer.parseInt(inValue));
                                                        } else {
                                                            strList.add(Util.splitDecode(inValue));
                                                        }
                                                    }
                                                    criteria.andColumnIn(key, strList);
                                                }
                                                break;
                                                case "notin": {
                                                    List<Object> strList = new ArrayList<Object>();
                                                    keyValue = Util.splitEncode(keyValue);
                                                    for (String inValue : keyValue.split(",")) {
                                                        if (Util.isDigitalType(keyType)) {
                                                            strList.add(Integer.parseInt(inValue));
                                                        } else {
                                                            strList.add(Util.splitDecode(inValue));
                                                        }
                                                    }
                                                    criteria.andColumnNotIn(key, strList);
                                                }
                                                break;
                                                case "between":
                                                    keyValue = Util.splitEncode(keyValue);
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnBetween(key, Integer.parseInt(keyValue.split(",")[0]), Integer.parseInt(keyValue.split(",")[1]));
                                                    } else {
                                                        criteria.andColumnBetween(key, Util.splitDecode(keyValue.split(",")[0]), Util.splitDecode(keyValue.split(",")[1]));
                                                    }
                                                    break;
                                                case "notbetween":
                                                    keyValue = Util.splitEncode(keyValue);
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnNotBetween(key, Integer.parseInt(keyValue.split(",")[0]), Integer.parseInt(keyValue.split(",")[1]));
                                                    } else {
                                                        criteria.andColumnNotBetween(key, Util.splitDecode(keyValue.split(",")[0]), Util.splitDecode(keyValue.split(",")[1]));
                                                    }
                                                    break;
                                                case "like":
                                                    criteria.andColumnLike(key, Util.splitDecode(keyValue));
                                                    break;
                                                case "notlike":
                                                    criteria.andColumnNotLike(key, Util.splitDecode(keyValue));
                                                    break;
                                                case "equal":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnEqualTo(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnEqualTo(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "notequal":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnNotEqualTo(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnNotEqualTo(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "less":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnLessThan(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnLessThan(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "greater":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnGreaterThan(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnGreaterThan(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "lessequal":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnLessThanOrEqualTo(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnLessThanOrEqualTo(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "greaterequal":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnGreaterThanOrEqualTo(key, Integer.parseInt(keyValue));
                                                    } else {
                                                        criteria.andColumnGreaterThanOrEqualTo(key, Util.splitDecode(keyValue));
                                                    }
                                                    break;
                                                case "regexp":
                                                    if (Util.isDigitalType(keyType)) {
                                                        criteria.andColumnEqualTo(key,keyValue);
                                                    } else {
                                                        criteria.andColumnRegexp(key,keyValue);
                                                    }
                                                    break;
                                                case "isnull":
                                                    criteria.andColumnIsNull(key);
                                                    break;
                                                case "isnotnull":
                                                    criteria.andColumnIsNotNull(key);
                                                    break;
                                                default:
                                            }
                                        }
                                        else
                                            {
                                            if (keyType != null && Util.isDigitalType(keyType)) {
                                                criteria.andColumnEqualTo(key, keyValue);
                                            } else {
                                                //criteria.andColumnEqualTo(key, keyValue);
                                                //criteria.andColumnLike(key, paramsProperties.getProperty(key));
                                                criteria.andColumnRegexp(key, Util.regexpFix(keyValue));
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("[" + this.port + "]httpRequest:get ERROR:" + e.getMessage());
                            }
                        }

                        if (joincols != null) {
                            commonExample.setJoinCols(joincols);
                        }
                        if (groupCol != null) {
                            commonExample.setGroupByClause(groupCol);
                        }

                        int totalRecord = commonDAO.countByExample(commonExample);
                        if (totalRecord > -1) {
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

                            ResultSet rs = commonDAO.selectByExample(selectCol, commonExample);

                            if (rs != null) {
                                apiResponse.setReturnCode("200");
                                apiResponse.setReturnData(commonDAO.resultSet2Json(rs, selectCol, commonDAO.getTableInfo(), totalRecord, commonExample,tableinfo));
                                rs.close();
                            } else {
                                apiResponse.setReturnCode("504");
                                JSONObject tmp = new JSONObject();
                                tmp.put("result", 0);
                                tmp.put("msg", "参数值错误！");
                                apiResponse.setReturnData(tmp);
                            }
                        } else {
                            apiResponse.setReturnCode("504");
                            JSONObject tmp = new JSONObject();
                            tmp.put("result", 0);
                            tmp.put("msg", "参数值错误！");
                            apiResponse.setReturnData(tmp);
                        }
                        break;
                    }
                    case "add": {
                        //authentication = new Authentication(connection);
                        //if (authentication.tokenAvailable(username, token, ip) || true) {
                        commonDAO = new CommonDAO(tableName, connection, username);
                        commonDAO.setPort(this.port);
                        commonDAO.setTraceid(params.getTraceid());

                        commonExample = new CommonExample();
                        criteria = commonExample.createValueCriteria();

                        it = paramsProperties.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Object, Object> entry = it.next();
                            String key = (String) entry.getKey();
                            String value = (String) entry.getValue();
                            if (key != null) {
//                                    if (paramsProperties.getProperty(key).trim().length() > 0)
//                                        criteria.addKeyValue(key, paramsProperties.getProperty(key));
                                if (value.trim().length() > 0) {
                                    criteria.addKeyValue(key, value);
                                } else {
                                    Column column = commonDAO.getTableInfo().getColumnByName(key);
                                    try {
                                        if (!column.isMaynull()) {
                                            switch (column.getType().toLowerCase()) {
                                                case "int":
                                                case "tinyint":
                                                case "long":
                                                    criteria.addKeyValue(key, 0);
                                                    break;
                                                case "datetime":
                                                case "timestamp":
                                                    criteria.addKeyValue(key, Util.now());
                                                    break;
                                                default:
                                                    criteria.addKeyValue(key, "");
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("[" + this.port + "]httpRequest:Add ERROR:Key[" + key + "] not found in table[" + tableName + "]!");
                                    }
                                }
                            }
                        }

                        result = commonDAO.insertByExample(commonExample);
                        if (result > 0) {
                            apiResponse.setReturnCode("200");
                            apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据插入成功！\"}");
                        } else {
                            apiResponse.setReturnCode("500");
                            apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据插入失败！\"}");
                        }
                        break;
                    }
                    case "update": {
                        //authentication = new Authentication(connection);
                        //if (authentication.tokenAvailable(username, token, ip) || true) {
                        commonDAO = new CommonDAO(tableName, connection, username);
                        commonDAO.setPort(this.port);
                        commonDAO.setTraceid(params.getTraceid());
                        //String keyColumn = commonDAO.getTableInfo().getKey();
                        commonExample = new CommonExample();
                        criteria = commonExample.createCriteria();
                        String keys = paramsProperties.getProperty(commonDAO.getTableInfo().getKey());
                        if (keys != null) {
                            if (!keys.contains(",")) {   //单个key
                                criteria.andColumnEqualTo(commonDAO.getTableInfo().getKey(), keys);
                            } else {
                                ArrayList<Object> keyList = new ArrayList<Object>();
                                for (String key : keys.split(",")) {
                                    if (commonDAO.getTableInfo().getColumnByName(commonDAO.getTableInfo().getKey()).getType().compareTo("varchar") == 0) {
                                        keyList.add(key);
                                    } else {
                                        keyList.add(Integer.parseInt(key));
                                    }
                                }
                                criteria.andColumnIn(commonDAO.getTableInfo().getKey(), keyList);
                            }
                            paramsProperties.remove(commonDAO.getTableInfo().getKey());

                            criteria = commonExample.createValueCriteria();
                            it = paramsProperties.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<Object, Object> entry = it.next();
                                String key = (String) entry.getKey();
                                String value = (String) entry.getValue();
                                if (key != null) {
                                    if (value.trim().length() == 0) {
                                        Column column = commonDAO.getTableInfo().getColumnByName(key);
                                        if (column != null) {
                                            if (column.isMaynull()) {
                                                criteria.setKeyValue2Null(key);
                                            }
                                        } else {
                                            logger.error("[" + this.port + "]httpRequest:update ERROR:Key[" + key + "] is not exist !");
                                        }
                                    } else {
                                        criteria.setKeyValue(key, value);
                                    }
                                }
                            }

                            result = commonDAO.updateByExample(commonExample);
                            if (result > 0) {
                                apiResponse.setReturnCode("200");
                                apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据更新成功！\"}");
                            } else {
                                apiResponse.setReturnCode("500");
                                apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据更新失败！\"}");

                            }
                        } else {
                            apiResponse.setReturnCode("507");
                            apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"Key[" + commonDAO.getTableInfo().getKey() + "]不可为空！\"}");
                        }
                        break;
                    }
                    case "drop": {
                        //authentication = new Authentication(connection);
                        //if (authentication.tokenAvailable(username, token, ip) || true) {
                        commonDAO = new CommonDAO(tableName, connection, username);
                        commonDAO.setPort(this.port);
                        commonDAO.setTraceid(params.getTraceid());

                        commonExample = new CommonExample();
                        criteria = commonExample.createCriteria();
                        String keys = paramsProperties.getProperty(commonDAO.getTableInfo().getKey());
                        if (keys != null) {
                            if (!keys.contains(",")) {   //单个key
                                criteria.andColumnEqualTo(commonDAO.getTableInfo().getKey(), keys);
                            } else {
                                ArrayList<Object> keyList = new ArrayList<Object>();
                                for (String key : keys.split(",")) {
                                    if (commonDAO.getTableInfo().getColumnByName(commonDAO.getTableInfo().getKey()).getType().compareTo("varchar") == 0) {
                                        keyList.add(key);
                                    } else {
                                        keyList.add(Integer.parseInt(key));
                                    }
                                }
                                criteria.andColumnIn(commonDAO.getTableInfo().getKey(), keyList);
                            }
                            paramsProperties.remove(commonDAO.getTableInfo().getKey());
                            result = commonDAO.deleteByExample(commonExample);
                            if (result > 0) {
                                apiResponse.setReturnCode("200");
                                apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据删除成功！\"}");
                            } else {
                                apiResponse.setReturnCode("500");
                                apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"数据删除失败！\"}");
                            }
                        } else {
                            apiResponse.setReturnCode("507");
                            apiResponse.setReturnData("{\"result\":" + result + ",\"msg\":\"Key[" + commonDAO.getTableInfo().getKey() + "]不可为空！\"}");
                        }
                        break;
                    }
                    default:
                }
            } catch (Exception e) {
                apiResponse.setReturnCode("503");
                apiResponse.setReturnData(e.getMessage());
                logger.error("[" + this.port + "]httpRequest ERROR:" + e.getMessage());
                retString = apiResponse.toString();
            }
        }
        if (callback != null) {
            retString = callback + "(" + apiResponse.toString() + ");";
        } else if (jsvalue != null) {
            retString = jsvalue + "=" + apiResponse.toString() + ";";
        } else {
            retString = apiResponse.toString();
        }
        return retString;
    }

    public String interfaceRequest(APIRequest params) {
        String retString = null;
        String action = params.getMethod();
        String token = params.getToken();
        APIResponse apiResponse = new APIResponse();
        apiResponse.setToken(token);
        String username = params.getUsername();
        String ip = params.getIp();
        Properties paramsProperties = params.getParameters();
        String callback = paramsProperties.getProperty("_callback");

        switch (action) {
            case "updateservice": {
                String dataJson = params.getParameters().getProperty("data");
                if (dataJson != null) {
                    CommonDAO commonDAO = new CommonDAO("cmdb_application_new", this.connection);
                    commonDAO.setSqlLog(false);

                    TableInfo cmdb_application_new = commonDAO.getTableInfo();
                    TableInfo cmdb_service_new = new TableInfo("cmdb_service_new", this.connection);

                    JSONObject data = JSONObject.parseObject(dataJson);
                    String hostname = data.getString("hostname");
                    JSONArray services = data.getJSONArray("services");
                    if (hostname != null && services.size() > 0) {
                        commonDAO.executeBySql("delete csn from cmdb_service_new csn " +
                                "INNER JOIN cmdb_service_host_new ON cmdb_service_host_new.belong_to_service_id = csn.id " +
                                "INNER JOIN cmdb_host ON cmdb_host.id = cmdb_service_host_new.belong_to_host_id " +
                                "where cmdb_host.host_name='" + Util.mysqlEscape(hostname) + "';");
                        commonDAO.executeBySql("delete cshn from cmdb_service_host_new cshn " +
                                "INNER JOIN cmdb_host ON cmdb_host.id = cshn.belong_to_host_id " +
                                "where cmdb_host.host_name='" + Util.mysqlEscape(hostname) + "';");
                    }
                    //String[] servicesList = new String[services.size()];
                    for (int i = 0; i < services.size(); i++) {
                        try {
                            JSONObject tmp = services.getJSONObject(i);
                            String servicename = tmp.getString("servicename");
                            String deployload = tmp.getString("deployload");
                            String certificate = tmp.getString("certificate");
                            String port = tmp.getString("port");
                            String applicationname = tmp.getString("applicationname");
                            String application_id = null;

                            commonDAO.setTableInfo(cmdb_application_new);
                            CommonExample commonExample = new CommonExample();
                            CommonExample.Criteria criteria = commonExample.createCriteria();
                            CommonExample.Criteria value = null;

                            criteria.andColumnEqualTo("application_name", applicationname);
                            ResultSet rs = commonDAO.selectByExample(commonExample);
                            if (rs != null) {
                                if (!rs.next()) { //如果应用不存在，则添加应用
                                    value = commonExample.createValueCriteria();
                                    value.addKeyValue("application_name", applicationname);
                                    application_id = String.valueOf(commonDAO.insertByExample(commonExample));
                                    rs.close();
                                } else {
                                    application_id = rs.getString("id");
                                }
                                rs.close();
                            }

                            commonExample.clear();
                            value = commonExample.createValueCriteria();
                            value.addKeyValue("belong_to_application_id", application_id);
                            value.addKeyValue("service_name", servicename);
                            value.addKeyValue("monitor_port", port);
                            value.addKeyValue("service_deploy_load", deployload);
                            commonDAO.setTableInfo(cmdb_service_new);
                            String service_id = String.valueOf(commonDAO.insertByExample(commonExample));

                            commonExample.clear();
                            commonDAO.executeBySql("insert into cmdb_service_host_new (service_deploy_load,monitor_port,certificate,service_status,belong_to_host_id,belong_to_service_id) " +
                                    "select '" + Util.mysqlEscape(deployload) + "','" + Util.mysqlEscape(port) + "','" + Util.mysqlEscape(certificate) + "',1,h.id," + service_id + " from cmdb_host h where h.host_name='" + hostname + "'");

                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                            apiResponse.setReturnCode("508");
                            apiResponse.setReturnData("{\"result\":0,\"msg\":\"程序异常，更新失败！\"}");
                            break;
                        }
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData("{\"result\":" + services.size() + ",\"hostname\":\"" + hostname + "\"}");
                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"操作请求获取失败！\"}");
                }
                break;
            }
            case "updatehost": {
                String dataJson = params.getParameters().getProperty("data");
                if (dataJson != null) {
                    CommonDAO commonDAO = new CommonDAO("cmdb_host", this.connection);
                    commonDAO.setSqlLog(false);

                    TableInfo cmdb_host = commonDAO.getTableInfo();
                    TableInfo cmdb_host_disk = new TableInfo("cmdb_host_disk", this.connection);

                    JSONObject data = JSONObject.parseObject(dataJson);
                    String hostname = data.getString("hostname");
                    JSONArray disks = data.getJSONArray("disk");


                    CommonExample commonExample = new CommonExample();
                    CommonExample.Criteria criteria = commonExample.createCriteria();
                    CommonExample.Criteria value = null;

                    criteria.andColumnEqualTo("host_name", hostname);
                    ResultSet rs = commonDAO.selectByExample(commonExample);
                    if (rs != null) {
                        try {
                            if (rs.next()) { //如果主机存在，则添加
                                String hostid = rs.getString("id");
                                rs.close();

                                if (hostname != null && disks.size() > 0) {
                                    commonDAO.executeBySql("delete from cmdb_host_disk " +
                                            "where belong_to_host_id=" + hostid);
                                }
                                int diskSize = 0;
                                for (int i = 0; i < disks.size(); i++) {
                                    JSONObject tmp = disks.getJSONObject(i);

                                    commonDAO.setTableInfo(cmdb_host_disk);
                                    commonExample.clear();
                                    criteria = commonExample.createCriteria();
                                    value = null;
                                    value = commonExample.createValueCriteria();
                                    value.addKeyValue("disk_label", Integer.parseInt(disks.getJSONObject(i).getString("label")));
                                    value.addKeyValue("disk_capacity", disks.getJSONObject(i).getString("capacity"));
                                    value.addKeyValue("data_storage", disks.getJSONObject(i).getString("storage"));
                                    value.addKeyValue("disk_account", disks.getJSONObject(i).getString("account"));
                                    value.addKeyValue("belong_to_host_id", Integer.parseInt(hostid));
                                    value.addKeyValue("host_name", hostname);
                                    value.addKeyValue("remark", Util.now());
                                    value.addKeyValue("in_use", 1);
                                    String disk_id = String.valueOf(commonDAO.insertByExample(commonExample));
                                    diskSize += Integer.parseInt(disks.getJSONObject(i).getString("capacity"));
                                }
                                commonDAO.setTableInfo(cmdb_host);
                                commonDAO.executeBySql("update cmdb_host h set h.cpu='" + data.getString("cpu") + "',h.memory='" + data.getString("mem") + "',h.disk='" + diskSize + "' where h.id=");

                                apiResponse.setReturnCode("200");
                                apiResponse.setReturnData("{\"result\":" + disks.size() + ",\"hostname\":\"" + hostname + "\"}");
                            } else {
                                apiResponse.setReturnCode("507");
                                apiResponse.setReturnData("{\"result\":0,\"msg\":\"主机不存在，更新失败！\"}");
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            apiResponse.setReturnCode("508");
                            apiResponse.setReturnData("{\"result\":0,\"msg\":\"程序异常，更新失败！\"}");
                            break;
                        }
                    } else {
                        apiResponse.setReturnCode("507");
                        apiResponse.setReturnData("{\"result\":0,\"msg\":\"主机不存在，更新失败！\"}");
                    }

                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"操作请求获取失败！\"}");
                }
                break;
            }
            default:
                apiResponse.setReturnCode("506");
                apiResponse.setReturnData("{\"result\":0,\"msg\":\"未知请求操作！\"}");
        }
        if (callback != null) {
            retString = callback + "(" + apiResponse.toString() + ");";
        } else {
            retString = apiResponse.toString();
        }

        return retString;
    }
}
