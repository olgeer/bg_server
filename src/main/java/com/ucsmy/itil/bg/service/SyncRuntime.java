package com.ucsmy.itil.bg.service;

import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.api.RedisUtil;
import com.ucsmy.itil.bg.common.Configure;
import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;

public class SyncRuntime {
    private String url = "http://10.1.20.253/api_jsonrpc.php";
    private ZabbixApi zabbixApi;
    private String account;
    private String password;
    private boolean login = false;
    private int cachesize = 100;
    private static Logger logger = LogManager.getLogger(SyncRuntime.class.getName());

    public SyncRuntime() {
        Properties zabbixProperty = Configure.getConfig();
        url = zabbixProperty.getProperty("zabbix.url");
        account = zabbixProperty.getProperty("zabbix.account");
        password = zabbixProperty.getProperty("zabbix.password");
        cachesize = Util.str2int(zabbixProperty.getProperty("zabbix.cachesize"), 100);
        init();
    }

    public void init() {
        zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();
        login = zabbixApi.login(account, password);
    }

    public void close() {
        if (login) {
            zabbixApi.destroy();
        }
    }

    public String[] getHostList() {
        return getHostList(null);
    }

    public String[] getHostList(String[] hosts) {
        String[] hostList = null;
        Request getRequest = RequestBuilder.newBuilder().method("host.get")
                //.paramEntry("output", "extend")
                .paramEntry("output", new String[]{"host", "status"})
                .build();
        JSONObject filter = new JSONObject();
        filter.put("status", "0");
        if (hosts != null) {
            if (hosts.length > 0) {
                filter.put("host", hosts);
            }
        }
        getRequest.putParam("filter", filter);
        JSONObject getResponse = zabbixApi.call(getRequest);
        //System.err.println(getResponse);
        hostList = new String[getResponse.getJSONArray("result").size()];
        for (int i = 0; i < getResponse.getJSONArray("result").size(); i++) {
            hostList[i] = getResponse.getJSONArray("result").getJSONObject(i).getString("host");
        }
        return hostList;
    }

    public JSONObject getHostItems(String host) {
        JSONObject retString = null;

        JSONObject filter = new JSONObject();

        filter.put("host", host);
        filter.put("state", "0");
        filter.put("status", "0");

        Request getRequest = RequestBuilder.newBuilder()
                .method("item.get")
                .paramEntry("filter", filter)
                .paramEntry("output", new String[]{"name", "key_", "lastclock", "lastvalue"})
                //.paramEntry("output", "extend")
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);
        //System.err.println(getResponse);
        if (getResponse.getJSONArray("result").size() > 0) {
            retString = getResponse;
            //System.err.println(retString);
        }
        return retString;
    }

    public void writeAllHostItem2Redis() {
        logger.debug(Util.now() + " Begin to sync runtime states !");
        Connection connection = ItilDataSource.newInstance().getConn();
        CommonDAO commonDAO = new CommonDAO("cmdb_host", connection);
        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo("host_status", 0);
        ResultSet resultSet = commonDAO.selectByExample("host_name", commonExample);
        try {
            String hostname = null;
            JSONObject monitor = null;
            ArrayList<JSONObject> hostList = new ArrayList<JSONObject>(this.cachesize);
            int count = 1;
            int total = 0;
            int cmdbHost = 0;

            while (resultSet.next()) {
                hostname = resultSet.getString("host_name");
                cmdbHost++;
                monitor = getHostItems(hostname);
                if (monitor != null) {
                    monitor.put("hostname", hostname);
                    monitor.put("updatetime", Util.now());
                    hostList.add(monitor);
                    count++;
                    if (count >= cachesize) {
                        total += RedisUtil.batchSet("hostname", hostList);
                        logger.debug("Write " + total + " record to redis !");
                        hostList.clear();
                        count = 0;
                    }
                }
            }

            if (count > 1) total += RedisUtil.batchSet("hostname", hostList);
            logger.debug("Found " + cmdbHost + " host and write " + total + " monitor record to redis !");
            resultSet.close();
            Util.safeClose(connection);
            logger.debug("SyncRuntime states done !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SyncRuntime syncRuntime = new SyncRuntime();
        syncRuntime.writeAllHostItem2Redis();
        syncRuntime.close();
    }
}
