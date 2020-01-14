package com.ucsmy.itil.bg.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.api.APIRequest;
import com.ucsmy.itil.bg.api.APIResponse;
import com.ucsmy.itil.bg.api.LdapApi;
import com.ucsmy.itil.bg.api.VmwareApi;
import com.ucsmy.itil.bg.common.*;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Max on 2017/4/23.
 */
public class Automate {
    private Connection connection;
    private int port;
    private String traceid;
    private static UcsmyLog logger = new UcsmyLog(Automate.class.getName());

    public Automate(Connection connection) {
        this.connection = connection;
        setPort(0);
    }

    public Automate(Connection connection, int port) {
        this.connection = connection;
        setPort(port);
    }

    public Automate(Connection connection, int port, String traceid) {
        this.connection = connection;
        setPort(port);
        setTraceid(traceid);
    }

    public String getTraceid() {
        return traceid;
    }

    public void setTraceid(String traceid) {
        this.traceid = traceid;
        logger.setTrace(traceid);
    }

    public void setPort(int port) {
        this.port = port;

    }

    public int initHostname() {
        int succ = 0;
        int buf = 0;
        try {
            //this.connection.setAutoCommit(false);
            CommonDAO commonDAO = new CommonDAO("cmdb_hostname", this.connection);
            commonDAO.setSqlLog(false);
            commonDAO.setPort(this.port);
            /*for (int i = 1; i < 100000; i++) {
                buf++;
                commonDAO.executeBySql("insert into cmdb_hostname (id) values(" + i + ")");
                succ++;
            }*/
            commonDAO.executeBySql("update cmdb_hostname hn,cmdb_host h set hn.hostname=h.host_name,hn.`status`=h.host_status" +
                    " where h.host_name REGEXP '^(P|V|S|U|O)(A|B|C|D)(0|1)(L|W)(W|A|S|C|M|G|D|J)[0-9]{5}'" +
                    " and hn.id=CONVERT(SUBSTR(h.host_name,6,5),UNSIGNED)" +
                    " and hn.id != 476");
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return succ;
    }

    //刷新服务器名信息，可重复执行
    public static void freeHostname() {
        int freeHostnameDay = 3;
        String freeHostnameDaySetting = Configure.getConfig().getProperty("auto.freehostnameday");

        try {
            if (freeHostnameDaySetting != null) {
                freeHostnameDay = Integer.parseInt(freeHostnameDaySetting);
            }
        } catch (NumberFormatException e) {
        }

        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_hostname", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        ResultSet rs = commonDAO.selectBySql("select h.id as id,h.modifytime as modifytime,h1.host_status as host_status from cmdb_hostname h" +
                " LEFT JOIN cmdb_host h1 on h.hostname=h1.host_name" +
                " where h.`status`=8");
        try {
            while (rs.next()) {
                int id = rs.getInt("id");

                //Date modifytime = rs.getDate("modifytime");
                //if (Util.datePlus(modifytime, Calendar.DAY_OF_YEAR, freeHostnameDay).compareTo(new Date()) < 0) {
                String modifytime = rs.getString("modifytime");
                String hostStatus = rs.getString("host_status");
                if (Util.workDateDiff(modifytime, Util.now()) > (freeHostnameDay * Util.WORKSECOND)) {
                    if (hostStatus == null) {
                        commonDAO.executeBySql("update cmdb_hostname set status=9 where id=" + id);
                    } else {
                        commonDAO.executeBySql("update cmdb_hostname set status=" + hostStatus + " where id=" + id);
                    }
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Util.safeClose(conn);
    }

    private String toShadowIp(String ip) {
        String newip = "";
        if (ip != null) {
            int f = ip.indexOf(".");
            if (f > 0) {
                int s = ip.indexOf(".", f + 1);
                if (s > f) {
                    int ip2 = Integer.parseInt(ip.substring(f + 1, s));
                    newip = ip.substring(0, f + 1) + (ip2 + 10) + ip.substring(s);
                }
            }
        }
        return newip;
    }

    public String allocate(APIRequest params) {
        String retString = null;
        String action = params.getMethod();
        String token = params.getToken();
        APIResponse apiResponse = new APIResponse();
        apiResponse.setToken(token);
        String username = params.getUsername();
        String ip = params.getIp();
        Properties paramsProperties = params.getParameters();
        String callback = paramsProperties.getProperty("_callback");
        String company = paramsProperties.getProperty("_company");//公司名/company。网金
        String room = paramsProperties.getProperty("_room");//机房名/room
        String resourcearea = paramsProperties.getProperty("_resourcearea");//平台类型/资源区域/resource_area，sit/uat  /字典cmdb_network_ip_area
        String os = paramsProperties.getProperty("_os");//操作系统/os,linux
        String apptype = paramsProperties.getProperty("_apptype");//应用/服务器类型/server_type，web，db，jump/字典cmdb_network_ip_type
        String networktype = paramsProperties.getProperty("_networktype");//网络类型/内网,外网
        //String hosttype = paramsProperties.getProperty("_hosttype");//设备类型。主机/堡垒机
        //String opname = paramsProperties.getProperty("_opname");//业务，web，db，jump
        //this.port=params.getPort();
        switch (action) {
            case "allinone": {
                boolean NEW_IP_WHEN_EXIST = true;       //当对应IP存在值时是否仍坚持分配新IP
                String jsonRequest = paramsProperties.getProperty("data");
                if (jsonRequest != null) {
                    JSONObject request = JSONObject.parseObject(jsonRequest);
                    company = request.getString("company");
                    resourcearea = request.getString("resourcearea");
                    String platform_name = request.getString("platform");
                    //os=request.getString("os");
                    //apptype=request.getString("apptype");
                    JSONArray hosts = request.getJSONArray("hosts");
                    String req = request.getString("request");

                    for (int i = 0; i < hosts.size(); i++) {
                        JSONObject host = hosts.getJSONObject(i);
                        os = host.getString("os");
                        apptype = host.getString("apptype");
                        room = host.getString("room");
                        if (req.contains("hostname")) {
                            if (host.getString("hostname") == null) {       //不重复分配主机名
                                String hostname = makeHostName(company, room, resourcearea, os, apptype);
                                if (hostname != null) {
                                    hostname = allocHostName(hostname);
                                    if (hostname != null) {
                                        host.put("hostname", hostname);
                                    } else {
                                        host.put("host_error", "主机名自动分配失败！");
                                    }
                                } else {
                                    host.put("host_error", "参数缺失或无效！");
                                }
                            }
                        }
                    }
                    for (int i = 0; i < hosts.size(); i++) {        //独立处理ip分配问题，先处理主服务器
                        JSONObject host = hosts.getJSONObject(i);
                        if (host.getString("master") == null) {  //如果是容灾服务器则跳过，不单独分配
                            os = host.getString("os");
                            apptype = host.getString("apptype");
                            if (req.contains("ip")) {
                                String hostname = host.getString("hostname");
                                Boolean hasShadow = host.getBoolean("hasshadow");
                                if (hasShadow == null) {
                                    hasShadow = false;
                                }

                                String iptype = host.getString("iptype");
                                if (iptype == null) {
                                    iptype = "1,2,3";
                                }

                                if (hostname != null && platform_name != null) {
                                    if (iptype.contains("1")) {                                        //业务ip
                                        if ((List<JSONObject>) host.get("iptype1") == null || ((List<JSONObject>) host.get("iptype1")).size() == 0 || NEW_IP_WHEN_EXIST) {
                                            String iptype1 = allocIp(hostname, platform_name, 1);
                                            if (iptype1 != null) {
                                                host.put("iptype1", JSONObject.parse(iptype1));
                                            }
                                        }
                                    }
                                    if (resourcearea.contains("生产") && iptype.contains("2")) {      //备份ip
                                        if ((List<JSONObject>) host.get("iptype2") == null || ((List<JSONObject>) host.get("iptype2")).size() == 0 || NEW_IP_WHEN_EXIST) {
                                            String iptype2 = allocIp(hostname, platform_name, 2);
                                            if (iptype2 != null) {
                                                host.put("iptype2", JSONObject.parse(iptype2));
                                            }
                                        }
                                    }
                                    if (apptype.toLowerCase().contains("db") && os.toLowerCase().contains("windows") && iptype.contains("3")) { //心跳ip
                                        if ((List<JSONObject>) host.get("iptype3") == null || ((List<JSONObject>) host.get("iptype3")).size() == 0 || NEW_IP_WHEN_EXIST) {
                                            String iptype3 = allocIp(hostname, platform_name, 3);
                                            if (iptype3 != null) {
                                                host.put("iptype3", JSONObject.parse(iptype3));
                                            }
                                        }
                                    }

                                } else {
                                    host.put("ip_error", "参数缺失或无效！");
                                }
                            }
                        }
                    }
                    for (int i = 0; i < hosts.size(); i++) {        //继续处理容灾服务器
                        JSONObject host = hosts.getJSONObject(i);
                        if (host.getString("master") != null) {  //如果是容灾服务器则处理
                            os = host.getString("os");
                            apptype = host.getString("apptype");
                            if (req.contains("ip")) {
                                String hostname = host.getString("hostname");
                                String masterHost = host.getString("master");
                                JSONObject masterHostObject = null;
                                String iptype = host.getString("iptype");
                                if (iptype == null) {
                                    iptype = "1,2,3";
                                }

                                if (masterHost != null) {
                                    int find = Util.findInJSONArray(hosts, "alias", masterHost);
                                    if (find > -1) {
                                        masterHostObject = hosts.getJSONObject(find);
                                    }
                                }
                                if (masterHostObject != null) {
                                    if (hostname != null && platform_name != null) {
                                        if (iptype.contains("1")) {
                                            List<JSONObject> iptypeObj = (List<JSONObject>) masterHostObject.get("iptype1");
                                            if (iptypeObj != null && iptypeObj.size() > 0) {
                                                String iptype1 = iptypeObj.get(0).getString("ip");

                                                List<JSONObject> shadowIpList = new ArrayList<>();
                                                if (iptype1 != null) {
                                                    JSONObject preIpObject = preAllocIp(toShadowIp(iptype1), "预分配容灾业务IP给" + hostname);
                                                    if (preIpObject != null) {
                                                        shadowIpList.add(preIpObject);
                                                    } else {
                                                        host.put("ip_error", "iptype1预分配容灾业务IP失败，原因为无可用IP！");
                                                    }
                                                }
                                                host.put("iptype1", shadowIpList);
                                            } else {
                                                host.put("ip_error", "iptype1预分配容灾业务IP失败，原因为没找到亚太主机相应IP！");
                                            }
                                        }
                                    }
                                    if (resourcearea.contains("生产") && iptype.contains("2")) {
                                        List<JSONObject> iptypeObj = (List<JSONObject>) masterHostObject.get("iptype2");
                                        if (iptypeObj != null && iptypeObj.size() > 0) {
                                            String iptype2 = iptypeObj.get(0).getString("ip");

                                            List<JSONObject> shadowIpList = new ArrayList<>();
                                            if (iptype2 != null) {
                                                JSONObject preIpObject = preAllocIp(toShadowIp(iptype2), "预分配容灾备份IP给" + hostname);
                                                if (preIpObject != null) {
                                                    shadowIpList.add(preIpObject);
                                                } else {
                                                    host.put("ip_error", "iptype2预分配容灾备份IP失败，原因为无可用IP！");
                                                }
                                            }
                                            host.put("iptype2", shadowIpList);
                                        } else {
                                            host.put("ip_error", "iptype2预分配容灾备份IP失败，原因为没找到亚太主机相应IP！");
                                        }
                                    }
                                    if (apptype.toLowerCase().contains("db") && os.toLowerCase().contains("windows") && iptype.contains("3")) {
                                        List<JSONObject> iptypeObj = (List<JSONObject>) masterHostObject.get("iptype3");
                                        if (iptypeObj != null && iptypeObj.size() > 0) {
                                            String iptype3 = iptypeObj.get(0).getString("ip");

                                            List<JSONObject> shadowIpList = new ArrayList<>();
                                            if (iptype3 != null) {
                                                JSONObject preIpObject = preAllocIp(toShadowIp(iptype3), "预分配容灾心跳IP给" + hostname);
                                                if (preIpObject != null) {
                                                    shadowIpList.add(preIpObject);
                                                } else {
                                                    host.put("ip_error", "iptype3预分配容灾心跳IP失败，原因为无可用IP！");
                                                }
                                            }
                                            host.put("iptype3", shadowIpList);
                                        } else {
                                            host.put("ip_error", "iptype3预分配容灾心跳IP失败，原因为没找到亚太主机相应IP！");
                                        }
                                    }
                                }
                            } else {
                                host.put("ip_error", "参数缺失或无效！");
                            }
                        }
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(request.toJSONString());
                }
                break;
            }
            case "gethostname":
                String hostname = "XX0XX";

                String freeid = "";  //顺序号
                //平台--company,platform
                //平台归属--room,resource_area
                //服务器类型--server_type
                //操作系统--os

                if (company == null || room == null || resourcearea == null || os == null || apptype == null) {
                    apiResponse.setReturnCode("502");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"参数缺失或无效！\"}");
                } else {
                    hostname = makeHostName(company, room, resourcearea, os, apptype);
                    if (hostname != null) {
                        hostname = allocHostName(hostname);

                        if (hostname != null) {
                            apiResponse.setReturnCode("200");
                            apiResponse.setReturnData("{\"hostname\":\"" + hostname + "\"}");
                        } else {
                            apiResponse.setReturnCode("502");
                            apiResponse.setReturnData("{\"result\":0,\"msg\":\"参数缺失或无效！\"}");
                        }
                    } else {
                        apiResponse.setReturnCode("502");
                        apiResponse.setReturnData("{\"result\":0,\"msg\":\"参数缺失或无效！\"}");
                    }
                }
                break;
            case "allocip":
                String host_name = paramsProperties.getProperty("_hostname");    //主机名-- hostname
                String platform_name = paramsProperties.getProperty("_platform_name");//平台id-- platformid
                String ip_type = paramsProperties.getProperty("_iptype");        //IP类型-- iptype

                if (ip_type == null) {
                    apiResponse.setReturnCode("501");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"缺少必要参数_iptype！\"}");
                } else {
                    if (host_name == null) {
                        host_name = makeHostName(company, room, resourcearea, os, apptype);
                    }
                    switch (ip_type.trim()) {
                        case "1":
                        case "2":
                        case "3":
                        case "6":
                            if (host_name == null || platform_name == null) {
                                apiResponse.setReturnCode("501");
                                apiResponse.setReturnData("{\"result\":0,\"msg\":\"缺少必要参数！\"}");
                            } else {
                                String retIpList = allocIp(host_name, platform_name, Integer.valueOf(ip_type));
                                if (retIpList != null) {
                                    apiResponse.setReturnCode("200");
                                    apiResponse.setReturnData("{\"hostname\":\"" + host_name + "\",\"iplist\":" + retIpList + "}");
                                } else {
                                    apiResponse.setReturnCode("503");
                                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"没有可分配的IP！\"}");
                                }
                            }
                            break;
                        case "4":
                            if (platform_name == null || host_name == null) {
                                apiResponse.setReturnCode("501");
                                apiResponse.setReturnData("{\"result\":0,\"msg\":\"缺少必要参数！\"}");
                            } else {
                                String retIpList = allocIp(host_name, platform_name, Integer.valueOf(ip_type));
                                if (retIpList != null) {
                                    apiResponse.setReturnCode("200");
                                    apiResponse.setReturnData("{\"iplist\":" + retIpList + "}");
                                } else {
                                    apiResponse.setReturnCode("503");
                                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"没有可分配的IP！\"}");
                                }
                            }
                            break;
                        case "5":
                            if (networktype == null || host_name == null) {
                                apiResponse.setReturnCode("501");
                                apiResponse.setReturnData("{\"result\":0,\"msg\":\"缺少必要参数！\"}");
                            } else {
                                String retIpList = allocIp(host_name, networktype, Integer.valueOf(ip_type));
                                if (retIpList != null) {
                                    apiResponse.setReturnCode("200");
                                    apiResponse.setReturnData("{\"iplist\":" + retIpList + "}");
                                } else {
                                    apiResponse.setReturnCode("503");
                                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"没有可分配的IP！\"}");
                                }
                            }
                            break;
                        default:
                            apiResponse.setReturnCode("505");
                            apiResponse.setReturnData("{\"result\":0,\"msg\":\"_iptype参数值非法！\"}");
                    }
                }
                break;
            case "inithostname":
                apiResponse.setReturnCode("200");
                apiResponse.setReturnData("{\"result\":" + initHostname() + ",\"msg\":\"已完成主机名表初始化！\"}");
                break;
            default:
                apiResponse.setReturnCode("506");
                apiResponse.setReturnData("{\"result\":0,\"msg\":\"未知请求操作！\"}");
        }
        if (callback != null)

        {
            retString = callback + "(" + apiResponse.toString() + ");";
        } else

        {
            retString = apiResponse.toString();
        }

        return retString;
    }

    private String allocHostName(String prehostname) {
        String hostname = null;
        if (!prehostname.contains("X")) {
            CommonDAO commonDAO = new CommonDAO("cmdb_hostname", connection);
            commonDAO.setSqlLog(false);
            commonDAO.setPort(this.port);
            try {
                commonDAO.setAutoCommit(false);
                ResultSet rs = commonDAO.selectBySql("select id from cmdb_hostname where status=9 order by id limit 0,1 for update");
                if (rs != null) {
                    rs.next();
                    int hostid = rs.getInt("id");
                    if (hostid != 0) {
                        hostname = prehostname + String.format("%05d", hostid);
                        commonDAO.executeBySql("update cmdb_hostname set status=8,hostname='" + hostname + "' where id=" + hostid);
                    }
                    commonDAO.commit();
                    rs.close();
                }
            } catch (SQLException e) {
                logger.debug("[" + this.port + "]allocHostName:" + e.getMessage());
            } finally {
                commonDAO.setAutoCommit(true);
            }
        }
        return hostname;
    }

    public String makeHostName(String company, String room, String resourcearea, String os, String apptype) {
        String hostname = "XXXXX";
        char[] hostname_chr = hostname.toCharArray();
        if (resourcearea != null) {
            if (resourcearea.toLowerCase().contains("prod") || resourcearea.toLowerCase().contains("bd") || resourcearea.toLowerCase().contains("生产")) {
                hostname_chr[0] = 'P';
            }
            if (resourcearea.toLowerCase().contains("uat")) {
                hostname_chr[0] = 'U';
            }
            if (resourcearea.toLowerCase().contains("sit") || resourcearea.toLowerCase().contains("测试")) {
                hostname_chr[0] = 'S';
            }
            if (resourcearea.toLowerCase().contains("ops") || resourcearea.toLowerCase().contains("运维")) {
                hostname_chr[0] = 'V';
            }
            if (resourcearea.toLowerCase().contains("dmz") || resourcearea.toLowerCase().contains("ex")) {
                hostname_chr[0] = 'D';
            }
        }
        if (company != null) {
            if (company.contains("网金")) {
                hostname_chr[1] = 'A';
            }
            if (company.contains("钱端")) {
                hostname_chr[1] = 'B';
            }
            if (company.contains("钱途")) {
                hostname_chr[1] = 'C';
            }
            if (company.contains("数据")) {
                hostname_chr[1] = 'D';
            }
            if (company.contains("计算")) {
                hostname_chr[1] = 'E';      //云计算公司
            }
        }
        if (room != null) {
            if (room.contains("亚太")) {
                hostname_chr[2] = '0';
            }
            if (room.contains("化龙")) {
                hostname_chr[2] = '1';
            }
        }
        if (os != null) {
            if (os.toLowerCase().contains("vsphere")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("coreos")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("suse")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("rhel")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("centos")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("linux")) {
                hostname_chr[3] = 'L';
            }
            if (os.toLowerCase().contains("windows")) {
                hostname_chr[3] = 'W';
            }
        }
        if (apptype != null) {
            if (apptype.toLowerCase().contains("web")) {
                hostname_chr[4] = 'W';
            }
            if (apptype.toLowerCase().contains("app")) {
                hostname_chr[4] = 'A';
            }
            if (apptype.toLowerCase().contains("ser")) {
                hostname_chr[4] = 'S';
            }
            if (apptype.toLowerCase().contains("cache")) {
                hostname_chr[4] = 'C';
            }
            if (apptype.toLowerCase().contains("mag")) {
                hostname_chr[4] = 'M';
            }
            if (apptype.toLowerCase().contains("db") || apptype.toLowerCase().contains("库")) {
                hostname_chr[4] = 'D';
            }
            if (apptype.toLowerCase().contains("jump") || apptype.toLowerCase().contains("跳板")) {
                hostname_chr[4] = 'J';
            }
            if (apptype.toLowerCase().contains("msg")) {
                hostname_chr[4] = 'G';
            }
        }
        hostname = String.valueOf(hostname_chr);
        //if (hostname.contains("X")) hostname = null;
        return hostname;
    }

    public String allocIp(String hostname, String platform_name, int ipType) {
        return allocIp(hostname, platform_name, ipType, false);
    }

    public String allocIp(String hostname, String platform_name, int ipType, boolean needShadow) {
        JSONObject allocIpStr = null;
        List<JSONObject> ipList = new ArrayList<>();
        if (hostname != null) {
            try {
                String platformName = null;
                String platformId = null;
                CommonDAO commonDAO = new CommonDAO("cmdb_platform", connection);
                commonDAO.setPort(this.port);
                CommonExample commonExample = new CommonExample();
                CommonExample.Criteria criteria = commonExample.createCriteria();
                ResultSet tmpRs = null;

                tmpRs = commonDAO.selectBySql("select * from cmdb_platform where platform_name='" + platform_name + "'");
                if (tmpRs.next()) {
                    platformName = tmpRs.getString("platform_name");
                    platformId = tmpRs.getString("id");
                    tmpRs.close();
                }
                char[] hostname_chr = hostname.toCharArray();
                String roomid = String.valueOf(hostname_chr[2]);
                roomid = String.valueOf(Integer.valueOf(roomid) + 1);

                switch (ipType) {
                    case 1://业务IP
                        if (platformId != null) {
                            if (hostname_chr[0] == 'V' && platformName.contains("跳板机")) {   //跳板机,业务,根据机房->根据平台确定IP资源池，状态未用，增序分配；
                                logger.debug("[" + this.port + "]业务IP，跳板机，业务，根据机房->根据平台确定IP资源池，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, platformId, null, null, null, 1, "预分配业务IP给" + hostname, needShadow);
                            } else if (hostname_chr[0] == 'V' && platformName.contains("运维")) {  //运维服务器,根据机房->从承载业务"运维服务器-普通"，状态未用，增序分配
                                logger.debug("[" + this.port + "]业务IP，运维服务器，根据机房->从承载业务\"运维服务器-普通\"，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, platformId, null, null, "运维服务器-普通", 1, "预分配业务IP给" + hostname, needShadow);
                            } else if (hostname_chr[4] == 'D') {     //DB,DB/公共DB
                                if (hostname_chr[0] == 'S') {    //公共DB,区域为测试：根据机房->忽略平台名称，承载业务为"SIT-DB"，状态未用，增序分配
                                    logger.debug("[" + this.port + "]业务IP，DB/公共DB，区域为测试：根据机房->忽略平台名称，承载业务为\"SIT-DB\"，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, null, "4", "7,9", "SIT-DB", 1, "预分配业务IP给" + hostname, needShadow);
                                }
                                if (hostname_chr[0] == 'P') {                          //DB,区域为生产/UAT:根据机房->平台名称确定IP资源池，状态未用，逆序分配
                                    logger.debug("[" + this.port + "]业务IP，DB/公共DB，区域为生产：根据机房->平台名称确定IP资源池，状态未用，逆序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "2", "7,9", null, 0, "预分配业务IP给" + hostname, needShadow);
                                }
                                if (hostname_chr[0] == 'U') {                          //DB,区域为生产/UAT:根据机房->平台名称确定IP资源池，状态未用，逆序分配
                                    logger.debug("[" + this.port + "]业务IP，DB/公共DB，区域为UAT：根据机房->平台名称确定IP资源池，状态未用，逆序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "3", "7,9", null, 0, "预分配业务IP给" + hostname, needShadow);
                                }
                            } else {                              //非DB,WEB/公共WEB/业务
                                if (hostname_chr[0] == 'S') {    //业务,区域为测试：忽略平台名称，从类型为"业务"的资源池中，状态未用，增序分配
                                    logger.debug("[" + this.port + "]业务IP，非DB,WEB/公共WEB/业务，区域为测试：忽略平台名称，从类型为\"业务\"的资源池中，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, null, "4", "6,8,17", null, 1, "预分配业务IP给" + hostname, needShadow);
                                }
                                if (hostname_chr[0] == 'P') {   //WEB/公共WEB,区域为生产/UAT：根据机房->平台名称来确定IP资源池，状态增序分配（后期平台分组做出来后再增加一项组的判断来确定WEB与公共WEB）
                                    logger.debug("[" + this.port + "]业务IP，非DB,WEB/公共WEB/业务，区域为生产：根据机房->平台名称来确定IP资源池，状态增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "2", "6,8,17", null, 1, "预分配业务IP给" + hostname, needShadow);
                                }
                                if (hostname_chr[0] == 'U') {   //WEB/公共WEB,区域为生产/UAT：根据机房->平台名称来确定IP资源池，状态增序分配（后期平台分组做出来后再增加一项组的判断来确定WEB与公共WEB）
                                    logger.debug("[" + this.port + "]业务IP，非DB,WEB/公共WEB/业务，区域为UAT：根据机房->平台名称来确定IP资源池，状态增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "3", "6,8,17", null, 1, "预分配业务IP给" + hostname, needShadow);
                                }
                            }
                        }
                        if (allocIpStr != null) {
                            ipList.add(allocIpStr);
                        }
                        break;
                    case 2://备份IP
                        if (platformId != null) {
                            if (hostname_chr[0] == 'P') {
                                if (hostname_chr[4] == 'D') {   //服务器类型为DB,区域为生产,根据机房->筛选IP资源池类型为"备份-DB"，关联平台名称，状态未用，增序分配
                                    logger.debug("[" + this.port + "]备份IP，服务器类型为DB,区域为生产,根据机房->筛选IP资源池类型为\"备份-DB\"，关联平台名称，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, null, "11", null, 1, "预分配备份IP给" + hostname, needShadow);
                                } else {                        //服务器类型非DB,区域为生产,根据机房->筛选IP资源池类型为"备份-WEB",关联平台名称，状态未用，增序分配
                                    logger.debug("[" + this.port + "]备份IP，服务器类型非DB,区域为生产,根据机房->筛选IP资源池类型为\"备份-WEB\",关联平台名称，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, null, "18", null, 1, "预分配备份IP给" + hostname, needShadow);
                                }
                            } else {
                                logger.debug("[" + this.port + "]备份IP，区域不为生产,暂不分配");
                            }
                        }
                        if (allocIpStr != null) {
                            ipList.add(allocIpStr);
                        }
                        break;
                    case 3://DB心跳IP
                        if (platformId != null) {
                            if (hostname_chr[4] == 'D') {
                                if (hostname_chr[3] == 'W') {   //服务器类型为DB，且操作系统为Windows,DB心跳,
                                    if (hostname_chr[0] == 'P') {    //区域为生产/UAT:根据机房->平台名称来确定IP资源池，状态未用，逆序分配
                                        logger.debug("[" + this.port + "]DB心跳IP，服务器类型为DB，且操作系统为Windows，DB心跳，区域为生产：根据机房->平台名称来确定IP资源池，状态未用，逆序分配");
                                        allocIpStr = preAllocIp(roomid, platformId, "2", "5", null, 0, "预分配DB心跳IP给" + hostname, needShadow);
                                    }
                                    if (hostname_chr[0] == 'U') {    //区域为生产/UAT:根据机房->平台名称来确定IP资源池，状态未用，逆序分配
                                        logger.debug("[" + this.port + "]DB心跳IP，服务器类型为DB，且操作系统为Windows，DB心跳，区域为UAT：根据机房->平台名称来确定IP资源池，状态未用，逆序分配");
                                        allocIpStr = preAllocIp(roomid, platformId, "3", "5", null, 0, "预分配DB心跳IP给" + hostname, needShadow);
                                    }
                                    if (hostname_chr[0] == 'S') {     //区域为测试：根据机房->忽略平台名称，状态未用，逆序分配
                                        logger.debug("[" + this.port + "]DB心跳IP，服务器类型为DB，且操作系统为Windows，DB心跳，区域为测试：根据机房->平台名称来确定IP资源池，状态未用，逆序分配");
                                        allocIpStr = preAllocIp(roomid, null, "4", "5", null, 0, "预分配DB心跳IP给" + hostname, needShadow);
                                    }
                                } else if (hostname_chr[3] == 'L') {   //服务器类型为DB，且操作系统为Linux,DB心跳,
                                    //allocIpStr = preAllocIp(roomid, null, null, "18", null, 1,"预分配给"+hostname);
                                    logger.debug("[" + this.port + "]DB心跳IP，服务器类型为DB，且操作系统为Linux，DB心跳，暂不分配");
                                }
                            } else {
                                logger.debug("[" + this.port + "]DB心跳IP，服务器类型不为DB，暂不分配");
                            }
                        }
                        if (allocIpStr != null) {
                            ipList.add(allocIpStr);
                        }
                        break;
                    case 4://集群VIP
                        if (platformId != null) {
                            if (hostname_chr[4] == 'D') {   //服务器类型为DB
                                if (hostname_chr[0] == 'P') { //DB/公共DB,区域为生产：根据机房->平台名称确定资源池，状态未用，增序分配
                                    logger.debug("[" + this.port + "]集群VIP，DB/公共DB,区域为生产：根据机房->平台名称确定资源池，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "2", "7,9", null, 1, "预分配集群VIP给" + hostname);
                                    if (allocIpStr != null) {
                                        ipList.add(allocIpStr);
                                    }
                                    if (hostname_chr[3] == 'W') {   //一个Windows集群分配2个，一个Linux集群分配一个
                                        logger.debug("[" + this.port + "]集群VIP，DB/公共DB,区域为生产：根据机房->平台名称确定资源池，状态未用，增序分配，windows系统，再分配一个");
                                        allocIpStr = preAllocIp(roomid, platformId, "2", "7,9", null, 1, "预分配集群VIP给" + hostname);
                                        if (allocIpStr != null) {
                                            ipList.add(allocIpStr);
                                        }
                                    }
                                }
                                if (hostname_chr[0] == 'U') { //DB/公共DB,区域为UAT：根据机房->平台名称确定资源池，状态未用，增序分配
                                    logger.debug("[" + this.port + "]集群VIP，DB/公共DB,区域为UAT：根据机房->平台名称确定资源池，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, platformId, "3", "7,9", null, 1, "预分配集群VIP给" + hostname);
                                    if (allocIpStr != null) {
                                        ipList.add(allocIpStr);
                                    }
                                    if (hostname_chr[3] == 'W') {   //一个Windows集群分配2个，一个Linux集群分配一个
                                        logger.debug("[" + this.port + "]集群VIP，DB/公共DB,区域为UAT：根据机房->平台名称确定资源池，状态未用，增序分配，windows系统，再分配一个");
                                        allocIpStr = preAllocIp(roomid, platformId, "3", "7,9", null, 1, "预分配集群VIP给" + hostname);
                                        if (allocIpStr != null) {
                                            ipList.add(allocIpStr);
                                        }
                                    }
                                }
                                if (hostname_chr[0] == 'S') {   //DB,区域为测试：根据机房->忽略平台名称，承载业务为"SIT-DBVIP"，状态未用，增序分配
                                    logger.debug("[" + this.port + "]集群VIP，DB,区域为SIT：根据机房->忽略平台名称，承载业务为\"SIT-DBVIP\"，状态未用，增序分配");
                                    allocIpStr = preAllocIp(roomid, null, "4", "7", "SIT-DBVIP", 1, "预分配集群VIP给" + hostname);
                                    if (allocIpStr != null) {
                                        ipList.add(allocIpStr);
                                    }
                                    if (hostname_chr[3] == 'W') {   //一个Windows集群分配2个，一个Linux集群分配一个
                                        logger.debug("[" + this.port + "]集群VIP，DB,区域为SIT：根据机房->忽略平台名称，承载业务为\"SIT-DBVIP\"，状态未用，增序分配，windows系统，再分配一个");
                                        allocIpStr = preAllocIp(roomid, null, "4", "7", "SIT-DBVIP", 1, "预分配集群VIP给" + hostname);
                                        if (allocIpStr != null) {
                                            ipList.add(allocIpStr);
                                        }
                                    }
                                }
                            } else {
                                logger.debug("[" + this.port + "]集群VIP，服务器类型不为DB，暂不分配");
                            }
                        }
                        break;
                    case 5://负载VIP,不适用于测试区域
                        //platform_id参数用于传递 外网/内网 参数
                        if (platform_name.contains("外网")) { //负载均衡,根据机房->筛选相应区域，承载业务含有"纵向"，不拿有DMZ，状态未用，增序分配
                            if (hostname_chr[0] == 'P') {
                                logger.debug("[" + this.port + "]负载VIP，外网，生产环境，根据机房->筛选相应区域，承载业务含有\"纵向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "2", "24", "纵向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (hostname_chr[0] == 'U') {
                                logger.debug("[" + this.port + "]负载VIP，外网，UAT环境，根据机房->筛选相应区域，承载业务含有\"纵向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "3", "24", "纵向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (hostname_chr[0] == 'S') {
                                logger.debug("[" + this.port + "]负载VIP，外网，SIT环境，根据机房->筛选相应区域，承载业务含有\"纵向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "3", "24", "纵向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (allocIpStr != null) {
                                allocIpStr.put("linetype", "外网");
                            }
                        }
                        if (platform_name.contains("内网")) { //负载均衡,根据机房->筛选相应区域，承载业务含有"横向"，不拿有DMZ，状态未用，增序分配
                            if (hostname_chr[0] == 'P') {
                                logger.debug("[" + this.port + "]负载VIP，内网，生产环境，根据机房->筛选相应区域，承载业务含有\"横向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "2", "24", "横向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (hostname_chr[0] == 'U') {
                                logger.debug("[" + this.port + "]负载VIP，内网，UAT环境，根据机房->筛选相应区域，承载业务含有\"横向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "3", "24", "横向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (hostname_chr[0] == 'S') {
                                logger.debug("[" + this.port + "]负载VIP，内网，SIT环境，根据机房->筛选相应区域，承载业务含有\"横向\"，不拿有DMZ，状态未用，增序分配");
                                allocIpStr = preAllocIp(roomid, null, "3", "24", "横向", 1, "预分配负载VIP给" + hostname);
                            }
                            if (allocIpStr != null) {
                                allocIpStr.put("linetype", "内网");
                            }
                        }
                        if (allocIpStr != null) {
                            ipList.add(allocIpStr);
                        }
                        break;
                    case 6://公网IP
                        if (roomid.compareTo("1") == 0) {   //机房为亚太
                            if (hostname_chr[0] == 'P') {
                                logger.debug("[" + this.port + "]公网IP，机房为亚太，生产环境，分配一个电信IP");
                                allocIpStr = preAllocIp(roomid, null, null, "13", null, 1, "预分配公网IP给" + hostname);  //电信ip
                                if (allocIpStr != null) {
                                    allocIpStr.put("linetype", "电信");
                                    ipList.add(allocIpStr);
                                }
                                logger.debug("[" + this.port + "]公网IP，机房为亚太，生产环境，分配一个联通IP");
                                allocIpStr = preAllocIp(roomid, null, null, "20", null, 1, "预分配公网IP给" + hostname);  //联通ip
                                if (allocIpStr != null) {
                                    allocIpStr.put("linetype", "联通");
                                    ipList.add(allocIpStr);
                                }
                            }
                            if (hostname_chr[0] == 'U' || hostname_chr[0] == 'S') {   //UAT或SIT
                                tmpRs = commonDAO.selectBySql("select * from view_cmdb_network_public_ip where platform_name='" + platform_name + "' and room_name='亚太' and (area_name='UAT' or area_name='SIT')");
                                if (tmpRs.next()) {
                                    try {
                                        allocIpStr = new JSONObject();
                                        allocIpStr.put("ip", tmpRs.getString("ip"));
                                        allocIpStr.put("netmask", tmpRs.getString("netmask"));
                                        allocIpStr.put("gateway", tmpRs.getString("gateway"));
                                        allocIpStr.put("vlan", tmpRs.getString("vlan"));
                                        //allocIpStr.put("linetype","电信");
                                        logger.debug("[" + this.port + "]公网IP，机房为亚太，UAT或SIT，找到已分配IP");
                                    } catch (SQLException e) {
                                        allocIpStr = null;
                                    } finally {
                                        tmpRs.close();
                                    }
                                }
                                if (allocIpStr == null) {
                                    logger.debug("[" + this.port + "]公网IP，机房为亚太，UAT或SIT，没找到已分配IP，分配一个电信IP");
                                    allocIpStr = preAllocIp(roomid, null, null, "13", null, 1, "预分配公网IP给" + hostname);  //电信ip
                                    if (allocIpStr != null) {
                                        allocIpStr.put("linetype", "电信");
                                        ipList.add(allocIpStr);
                                    }
                                }
                            }
                        }
                        if (roomid.compareTo("2") == 0) {       //机房为化龙
                            if (hostname_chr[0] == 'P') {        //机房为化龙，且为生产，三线BGP，按需求提供一个
                                logger.debug("[" + this.port + "]公网IP，机房为化龙，且为生产，三线BGP，按需求提供一个");
                                allocIpStr = preAllocIp(roomid, null, null, "19", null, 1, "预分配公网IP给" + hostname);
                                if (allocIpStr != null) {
                                    allocIpStr.put("linetype", "三线BGP");
                                    ipList.add(allocIpStr);
                                }
                            }
                            if (hostname_chr[0] == 'U' || hostname_chr[0] == 'S') {   //UAT或SIT
                                tmpRs = commonDAO.selectBySql("select * from view_cmdb_network_public_ip where platform_name='" + platform_name + "' and room_name='化龙' and (area_name='UAT' or area_name='SIT')");
                                if (tmpRs.next()) {
                                    try {
                                        allocIpStr = new JSONObject();
                                        allocIpStr.put("ip", tmpRs.getString("ip"));
                                        allocIpStr.put("netmask", tmpRs.getString("netmask"));
                                        allocIpStr.put("gateway", tmpRs.getString("gateway"));
                                        allocIpStr.put("vlan", tmpRs.getString("vlan"));
                                        //allocIpStr.put("linetype","电信");
                                        logger.debug("[" + this.port + "]公网IP，机房为化龙，UAT或SIT，找到已分配IP");
                                    } catch (SQLException e) {
                                        allocIpStr = null;
                                    } finally {
                                        tmpRs.close();
                                    }
                                }
                                if (allocIpStr == null) {
                                    logger.debug("[" + this.port + "]公网IP，机房为化龙，UAT或SIT，没找到已分配IP，分配一个BGPIP");
                                    allocIpStr = preAllocIp(roomid, null, null, "19", null, 1, "预分配公网IP给" + hostname);  //bgpip
                                    if (allocIpStr != null) {
                                        allocIpStr.put("linetype", "三线BGP");
                                        ipList.add(allocIpStr);
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        logger.debug("[" + this.port + "]allocIp:ip分配类型异常，未知的iptype["+ipType+"]。");
                }
                if (allocIpStr == null) {
                    logger.debug("[" + this.port + "]allocIp:没找到符合的条件组合。");
                }
            } catch (SQLException e) {
                logger.error("[" + this.port + "]allocIp:" + e.getMessage());
            }
        }

        return ipList.toString();
    }

    public synchronized JSONObject preAllocIp(String roomid, String platformId, String area, String type, String service, int direct, String remark) {
        return preAllocIp(roomid, platformId, area, type, service, direct, remark, false);
    }

    //机房：roomid, 平台ID：platformId, 区域：area, IP类型：type, 承载业务：service, 取值方向：direct(1增序分配；0逆序分配)
    public synchronized JSONObject preAllocIp(String roomid, String platformId, String area, String type, String service, int direct, String remark, boolean needShadow) {
        //String retIp = null;
        JSONObject retJson = null;
        //Connection connection = ItilDataSource.newInstance().getConn();
        CommonDAO commonDAO = new CommonDAO("view_cmdb_network_ip_exp", connection);
        commonDAO.setSqlLog(false);
        commonDAO.setPort(this.port);
        CommonExample commonExample = new CommonExample();
        //commonExample.setJoinCols("p.id i.belongs_ip_pool_id");
        CommonExample.Criteria criteria = commonExample.createCriteria();
        ResultSet tmpRs = null;
        if (remark == null) {
            remark = "";
        }
        if (roomid != null) {
            criteria.andColumnEqualTo("room_id", Integer.valueOf(roomid));
        }
        if (platformId != null) {
            criteria.andColumnEqualTo("platform_id", platformId);
        }
        if (area != null) {
            List<Object> arealist = new ArrayList<Object>();
            for (String t : area.split(",")) {
                arealist.add(Integer.valueOf(t));
            }
            criteria.andColumnIn("pool_area", arealist);
        }
        if (type != null) {
            List<Object> typelist = new ArrayList<Object>();
            for (String t : type.split(",")) {
                //typelist.add(Integer.valueOf(Dict.g("cmdb_network_ip_type",t.trim())));
                typelist.add(Integer.valueOf(t));
            }
            criteria.andColumnIn("pool_type", typelist);
        }
        if (service != null) {
            criteria.andColumnLike("bearer_service", service);
        }
        criteria.andColumnEqualTo("status", 0);
        criteria.andColumnEqualTo("in_use", 1);

        if (direct == 1) {  //asc
            commonExample.setOrderByClause("id");
        } else {
            commonExample.setOrderByClause("id desc");
        }
        commonExample.setPageSize(100);
        commonExample.setPageIndex(1);
        commonExample.setLockForUpdate(true);
        try {
            commonDAO.setAutoCommit(false);

            tmpRs = commonDAO.selectByExample(commonExample);

            if (tmpRs != null && tmpRs.next()) {
                if (needShadow) {
                    boolean findFreeIP = false;
                    while (!tmpRs.isLast() && !findFreeIP) {
                        String newip = toShadowIp(tmpRs.getString("ip"));
                        if (isFreeIp(newip)) {
                            findFreeIP = true;
                        } else {
                            tmpRs.next();
                        }
                    }
                }
                retJson = new JSONObject();
                retJson.put("ip", tmpRs.getString("ip"));
                retJson.put("netmask", tmpRs.getString("netmask"));
                retJson.put("gateway", tmpRs.getString("gateway"));
                retJson.put("vlan", tmpRs.getString("vlan"));
                //retIp = tmpRs.getString("ip")+","+tmpRs.getString("netmask")+","+tmpRs.getString("gateway");
                commonDAO.executeBySql("update cmdb_network_ip set status=8,remarks='" + remark + "' where id=" + tmpRs.getString("id"));
                commonDAO.commit();
            } else {
                logger.debug("[" + this.port + "]preAllocIp:Alloc fail ! roomid=" + roomid + ", platformId=" + platformId + ", area=" + area + ", type=" + type + ", service=" + service + ", direct=" + direct + ", remark=" + remark);
            }
            tmpRs.close();
        } catch (Exception e) {
            logger.error("[" + this.port + "]preAllocIp:" + e.getMessage());
        } finally {
            commonDAO.setAutoCommit(true);
            //Util.safeClose(connection);
        }

        return retJson;
    }

    public synchronized JSONObject preAllocIp(String preIp, String remark) {
        //String retIp = null;
        JSONObject retJson = null;
        //Connection connection = ItilDataSource.newInstance().getConn();
        CommonDAO commonDAO = new CommonDAO("view_cmdb_network_ip_exp", connection);
        commonDAO.setSqlLog(false);
        commonDAO.setPort(this.port);
        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();

        ResultSet tmpRs = null;
        if (remark == null) {
            remark = "";
        }
        if (preIp != null) {
            criteria.andColumnEqualTo("ip", preIp);
            criteria.andColumnEqualTo("status", 0);
            criteria.andColumnEqualTo("in_use", 1);

            commonExample.setPageSize(1);
            commonExample.setPageIndex(1);
            commonExample.setLockForUpdate(true);
            try {
                commonDAO.setAutoCommit(false);

                tmpRs = commonDAO.selectByExample(commonExample);

                if (tmpRs.next()) {

                    retJson = new JSONObject();
                    retJson.put("ip", tmpRs.getString("ip"));
                    retJson.put("netmask", tmpRs.getString("netmask"));
                    retJson.put("gateway", tmpRs.getString("gateway"));
                    retJson.put("vlan", tmpRs.getString("vlan"));
                    //retIp = tmpRs.getString("ip")+","+tmpRs.getString("netmask")+","+tmpRs.getString("gateway");
                    commonDAO.executeBySql("update cmdb_network_ip set status=8,remarks='" + Util.mysqlEscape(remark) + "' where id=" + tmpRs.getString("id"));
                    commonDAO.commit();
                } else {
                    logger.debug("[" + this.port + "]preAllocIp:Alloc fail ! preip=" + preIp + ", remark=" + remark);
                }
                tmpRs.close();
            } catch (Exception e) {
                logger.error("[" + this.port + "]preAllocIp:" + e.getMessage());
            } finally {
                commonDAO.setAutoCommit(true);
                //Util.safeClose(connection);
            }
        }
        return retJson;
    }

    private boolean isFreeIp(String ip) {
        boolean free = false;
        CommonDAO commonDAO = new CommonDAO("view_cmdb_network_ip_exp", connection);
        try {
            ResultSet rs2 = commonDAO.selectBySql("select count(1) from cmdb_network_ip where ip='" + Util.mysqlEscape(ip) + "' and status=0 and in_use=1");
            if (rs2.next()) {
                free = rs2.getInt(1) == 1;
            }
            rs2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return free;
    }

    //刷新服务器名信息，可重复执行
    public static void freeAllocIp() {
        int freeAllocIpDay = 3;
        String freeAllocIpDaySetting = Configure.getConfig().getProperty("auto.freeallocipday");

        try {
            if (freeAllocIpDaySetting != null) {
                freeAllocIpDay = Integer.parseInt(freeAllocIpDaySetting);
            }
        } catch (NumberFormatException e) {
        }

        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_network_ip", conn);
        commonDAO.setSqlLog(true);
        commonDAO.setWrite2usp(true);

        ResultSet rs = commonDAO.selectBySql("select i.id as id,i.update_time as update_time,chi.host_id from cmdb_network_ip i" +
                " LEFT JOIN cmdb_host_ip chi on chi.ip_id=i.id" +
                " where i.status=8");
        int batch = 0;
        try {
            commonDAO.setAutoCommit(false);
            while (rs.next()) {
                int id = rs.getInt("id");

                //Date modifytime = rs.getDate("modifytime");
                //if (Util.datePlus(modifytime, Calendar.DAY_OF_YEAR, freeHostnameDay).compareTo(new Date()) < 0) {
                String update_time = rs.getString("update_time");
                String device_id = rs.getString("host_id");
                if (Util.workDateDiff(update_time, Util.now()) > (freeAllocIpDay * Util.WORKSECOND)) {
                    if (device_id == null) {
                        commonDAO.executeBySql("update cmdb_network_ip set status=0 where id=" + id);
                    } else {
                        commonDAO.executeBySql("update cmdb_network_ip set status=1 where id=" + id);
                    }
                    if (batch++ > 50) {
                        commonDAO.commit();
                        batch = 0;
                    }
                }
            }
            if (batch > 0) {
                commonDAO.commit();
            }
            commonDAO.setAutoCommit(true);
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    //
    public static void incTablevision() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("tablevision", conn);
        commonDAO.setSqlLog(false);
        try {
            ResultSet rs = commonDAO.selectBySql("select ic.TABLE_NAME as table_name,ic.COLUMN_NAME as column_name,case when ic.COLUMN_KEY='PRI' and ic.EXTRA='auto_increment' then 1 else 0 end as autofill,ic.COLUMN_COMMENT as column_comment " +
                    "from information_schema.`COLUMNS` ic" +
                    " where ic.TABLE_SCHEMA='" + conn.getCatalog() + "'");

            while (rs.next()) {
                commonDAO.executeBySql("insert into tablevision (tablename,columnname,showname,autofill,describes) values('" + rs.getString("table_name") + "','" + rs.getString("column_name") + "','" + rs.getString("column_name") + "'," + rs.getString("autofill") + ",'" + rs.getString("column_comment") + "')");
            }
            rs.close();
        } catch (SQLException e) {
            //e.printStackTrace();
        }
        Util.safeClose(conn);
    }


    public static void initWorkTimeTable(String beginDate, String endDate, int interval) {
        Calendar bCal = Calendar.getInstance();
        bCal.setTime(Util.parseDateTime(beginDate));
        Calendar eCal = Calendar.getInstance();
        eCal.setTime(Util.parseDateTime(endDate));
        initWorkTimeTable(bCal, eCal, interval);
    }

    public static void initWorkTimeTable(Calendar beginDate, Calendar endDate, int interval) {
        int beginWorkHour = 9;
        int endWorkHour = 18;

        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("glob_worktime", conn);
        commonDAO.setSqlLog(false);
        Calendar initCal = (Calendar) beginDate.clone();
        int is_worktime = 0;
        try {
            commonDAO.executeBySql("delete from glob_worktime where begin_time between '" + Util.formatDateTime(beginDate.getTime()) + "' and '" + Util.formatDateTime(endDate.getTime()) + "'");

            do {
                if (initCal.get(Calendar.HOUR_OF_DAY) >= beginWorkHour && initCal.get(Calendar.HOUR_OF_DAY) < endWorkHour && initCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && initCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    is_worktime = 1;
                } else {
                    is_worktime = 0;
                }
                commonDAO.executeBySql("insert into glob_worktime (begin_time,interval_second,is_worktime) values('" + Util.formatDateTime(initCal.getTime()) + "'," + interval + "," + is_worktime + ")");
                initCal.add(Calendar.SECOND, interval);
            } while (initCal.compareTo(endDate) < 0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static void initCabinet(int lastid) {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_cabinet", conn);
        commonDAO.setSqlLog(false);
        CommonDAO cabinet_use = new CommonDAO("cmdb_cabinet_use", conn);
        ResultSet rs = commonDAO.selectBySql("select * from cmdb_cabinet");
        cabinet_use.setAutoCommit(false);
        cabinet_use.setWrite2usp(false);
        cabinet_use.setSqlLog(false);
        int gid = lastid, batch = 0;
        try {
            while (rs.next()) {
                int cabinet_id = rs.getInt("id");
                int cabinet_height = rs.getInt("height_u");

                for (int i = 1; i <= cabinet_height; i++) {
                    cabinet_use.executeBySql("insert into cmdb_cabinet_use (id,u_no,belong_to_cabinet_id) values(" + (++gid) + "," + i + "," + cabinet_id + ")");
                    batch++;
                    if (batch > 50) {
                        cabinet_use.commit();
                        batch = 0;
                    }
                }
            }
            if (batch > 0) {
                cabinet_use.commit();
                batch = 0;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Util.safeClose(conn);
    }

    //刷新设备上架位置信息，可重复执行
    public static void freshCabinetUse() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_cabinet", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);
        //CommonDAO cabinet_use = new CommonDAO("cmdb_cabinet_use", conn);

        //设置机柜使用表的使用记录为未使用
        commonDAO.executeBySql("update cmdb_cabinet_use set u_use_state=0");

        ResultSet rs = commonDAO.selectBySql("select c.id as id,inf.ul as ul,inf.u as u,inf.device_id as device_id from cmdb_cabinet c JOIN (" +
                "select  i.id as device_id,i.device_name as device_name,CASE when m.device_height='' then 1 else m.device_height end as u,i.belong_to_room_id as room_id," +
                "SUBSTRING_INDEX(i.physics_area,'-',1) as col,SUBSTRING_INDEX(SUBSTRING_INDEX(i.physics_area,'-',2),'-',-1) as no, SUBSTRING_INDEX(i.physics_area,'-',-1) as ul" +
                " from cmdb_device_information i " +
                "left JOIN cmdb_device_model m on i.belong_to_device_model_id=m.id" +
                ") inf on inf.room_id=c.belong_to_room_id and inf.col=c.line and inf.no=c.number");
        int err = 0, rec = 0, batch = 0;
        try {
            commonDAO.setAutoCommit(false);
            while (rs.next()) {
                try {
                    int cabinet_id = rs.getInt("id");
                    int use_start_u = rs.getInt("ul");
                    int device_height = rs.getInt("u");
                    int device_id = rs.getInt("device_id");

                    for (int i = 0; i < device_height; i++) {
                        commonDAO.executeBySql("update cmdb_cabinet_use u set u.belong_to_device_id=" + device_id + ",u.u_use_state=1 where u.belong_to_cabinet_id=" + cabinet_id + " and u.u_no=" + (use_start_u + i));
                        rec++;
                        batch++;
                        if (batch > 50) {
                            commonDAO.commit();
                            batch = 0;
                        }
                    }
                    if (batch > 0) {
                        commonDAO.commit();
                        batch = 0;
                    }
                    commonDAO.setAutoCommit(true);
                } catch (Exception ex) {
                    err++;
                }
            }
            rs.close();
            System.out.println("Total update " + rec + " records, error " + err + " records");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Util.safeClose(conn);
    }

    public static void freshCabinetUse_new() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_cabinet", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);
        //CommonDAO cabinet_use = new CommonDAO("cmdb_cabinet_use", conn);

        //设置机柜使用表的使用记录为未使用
        commonDAO.executeBySql("delete from cmdb_cabinet_use");

        ResultSet rs = commonDAO.selectBySql("select c.id as id,inf.ul as ul,inf.u as u,inf.device_id as device_id from cmdb_cabinet c JOIN (" +
                "select  i.id as device_id,i.device_name as device_name,1 as use_state,CASE when m.device_height='' then 1 else m.device_height end as u,i.belong_to_room_id as room_id," +
                "SUBSTRING_INDEX(i.physics_area,'-',1) as col,SUBSTRING_INDEX(SUBSTRING_INDEX(i.physics_area,'-',2),'-',-1) as no, SUBSTRING_INDEX(i.physics_area,'-',-1) as ul" +
                " from cmdb_device_information i " +
                "left JOIN cmdb_device_model m on i.belong_to_device_model_id=m.id" +
                ") inf on inf.room_id=c.belong_to_room_id and inf.col=c.line and inf.no=c.number");
        int err = 0, rec = 0, batch = 0, gid = 1;
        try {
            commonDAO.setAutoCommit(false);
            while (rs.next()) {
                try {
                    int cabinet_id = rs.getInt("id");
                    int use_start_u = rs.getInt("ul");
                    int device_height = rs.getInt("u");
                    int device_id = rs.getInt("device_id");

                    for (int i = 0; i < device_height; i++) {
                        commonDAO.executeBySql("insert into cmdb_cabinet_use (id,u_no,u_use_state,belong_to_device_id,belong_to_cabinet_id) values(" + (++gid) + "," + (use_start_u + i) + ",1," + device_id + "," + cabinet_id + ")");
                        //commonDAO.executeBySql("update cmdb_cabinet_use u set u.belong_to_device_id=" + device_id + ",u.u_use_state=1 where u.belong_to_cabinet_id=" + cabinet_id + " and u.u_no=" + (use_start_u + i));
                        rec++;
                        batch++;
                        if (batch > 50) {
                            commonDAO.commit();
                            batch = 0;
                        }
                    }
                    if (batch > 0) {
                        commonDAO.commit();
                        batch = 0;
                    }
                    commonDAO.setAutoCommit(true);
                } catch (Exception ex) {
                    err++;
                }
            }
            rs.close();
            initCabinet(gid);

            commonDAO.executeBySql("delete from pre_cabinet_unuse_tmp");
            commonDAO.executeBySql("insert into pre_cabinet_unuse_tmp (dup) select max(id) as dup from cmdb_cabinet_use GROUP BY belong_to_cabinet_id,u_no HAVING COUNT(u_no)>1");
            commonDAO.executeBySql("delete from cmdb_cabinet_use where id in (select dup from pre_cabinet_unuse_tmp)");

            System.out.println("Total update " + rec + " records, error " + err + " records");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Util.safeClose(conn);
    }

    //连续可用最大U数计算，可重复执行
    public static void countCabinetMaxFreeU(int count_cabinet_id) {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_cabinet", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        ResultSet rs = null;
        if (count_cabinet_id == 0) {
            rs = commonDAO.selectBySql("select * from cmdb_cabinet");
        } else {
            rs = commonDAO.selectBySql("select * from cmdb_cabinet where id=" + count_cabinet_id);
        }
        try {
            while (rs.next()) {
                int cabinet_id = rs.getInt("id");
                ResultSet crs = commonDAO.selectBySql("select id,u_use_state from cmdb_cabinet_use where belong_to_cabinet_id=" + cabinet_id);
                int maxfree = 0;
                int counting = 0;
                while (crs.next()) {
                    if (crs.getInt("u_use_state") == 0) {
                        counting++;
                    } else {
                        if (counting > maxfree) {
                            maxfree = counting;
                        }
                        counting = 0;
                    }
                }
                if (counting > maxfree) {
                    maxfree = counting;
                }

                crs.close();
                commonDAO.executeBySql("update cmdb_cabinet set max_empty_height_u=" + maxfree + " where id=" + cabinet_id);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Util.safeClose(conn);
    }

    //生成主机总览信息，可重复执行
    public static void updateHostInformation2() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_host_information", conn);
        commonDAO.setSqlLog(false);
        //清除cmdb_host_ip临时表
        //commonDAO.executeBySql("TRUNCATE table cmdb_host_ip;");
        commonDAO.executeBySql("delete from cmdb_host_ip;");
        //清除cmdb_host_information临时表
        //commonDAO.executeBySql("TRUNCATE table cmdb_host_information;");
        commonDAO.executeBySql("delete from cmdb_host_information;");

        //添加主机记录
        commonDAO.executeBySql("insert into cmdb_host_information (host_id,host_name,host_type,logic_area,company,platform_name,project_name,project_id,os_name,app_type) " +
                " select h.id,h.host_name,h.host_type,l.area_name,p.company,p.platform_name,cp.chief_platform_name,cp.id,os.operate_system_detail,h.application_type from cmdb_host h" +
                " LEFT JOIN cmdb_logic_area l on l.id=h.belong_to_logic_area_id" +
                " LEFT JOIN cmdb_operate_system os on os.id=h.belong_to_os_id" +
                " LEFT JOIN cmdb_platform p on p.id=h.belong_to_platform_id" +
                " LEFT JOIN cmdb_chief_platform cp on cp.id=p.belongs_to_chief_id");

        //更新主机信息
//        commonDAO.executeBySql("update cmdb_host_information i,(" +
//                "select h.id,h.host_name,h.host_type,l.area_name,p.company,p.platform_name,h.application_type from cmdb_host h" +
//                " LEFT JOIN cmdb_logic_area l on l.id=h.belong_to_logic_area_id" +
//                " LEFT JOIN cmdb_platform p on p.id=h.belong_to_platform_id" +
//                ") t set i.logic_area=t.area_name,i.platform_name=t.platform_name,i.company=t.company" +
//                " where i.host_id=t.id");

        //整理ip地址信息
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select i.host_id as host_id,n.ip as ip,p.pool_type from cmdb_host_information i" +
                " LEFT JOIN cmdb_device_logic_interface l on l.belongs_device_id=i.host_id and l.interface_device_type=0" +
                " LEFT JOIN cmdb_network_ip n on n.id=l.ip_id" +
                " INNER JOIN cmdb_network_ip_pool p on n.belongs_ip_pool_id=p.id" +
                " where n.ip is not null ;");

        //整理负载vip地址信息 -- 30
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select DISTINCT hip.host_id as host_id, n.load_ip as load_ip,30 from cmdb_network_load n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.real_ip)>0;");

        //整理集群vip地址信息 -- 31
/*        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select DISTINCT h.belong_to_host_id as host_id, i.ip as ip,31 from cmdb_vip_service_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_network_ip_id=i.id" +
                " JOIN cmdb_service_host h on h.id=v.belong_to_service_host_id;");*/
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select DISTINCT h.belong_to_host_id as host_id, i.ip as ip,31 from cmdb_vip_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_net_id=i.id" +
                " JOIN cmdb_service_host h on h.id=v.belong_to_host_id;");

        //整理公网ip地址信息 -- 32
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select DISTINCT hip.host_id as host_id, n.nat_before_ip as load_ip,32 from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.nat_after_ip)>0 and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " where p.pool_area=6;");

        //整理边界ip地址信息 -- 33
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type)" +
                " select DISTINCT hip.host_id as host_id, n.nat_before_ip as load_ip,33 from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.nat_after_ip)>0 and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " where p.pool_area=5;");

        //补充ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type<30 and hip.ip_type!=5 GROUP BY hip.host_id) pip" +
                " SET hi.ip=pip.ips where hi.host_id=pip.host_id;");
        //补充心跳ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type=5 GROUP BY hip.host_id) pip" +
                " SET hi.heartbeat_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充负载vip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type=30 GROUP BY hip.host_id) pip" +
                " SET hi.load_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充集群vip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type=31 GROUP BY hip.host_id) pip" +
                " SET hi.vip_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充公网ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type=32 GROUP BY hip.host_id) pip" +
                " SET hi.public_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充公网ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip) as ips from cmdb_host_ip hip where hip.ip_type=33 GROUP BY hip.host_id) pip" +
                " SET hi.border_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充服务信息
        commonDAO.executeBySql("update cmdb_host_information hi," +
                " (SELECT sh.belong_to_host_id as host_id,GROUP_CONCAT(s.definite_instance_name) as services_name " +
                " from cmdb_service_host sh" +
                " LEFT JOIN cmdb_service s on sh.belong_to_service_id=s.id " +
                " where sh.belong_to_host_id is not null" +
                " GROUP BY sh.belong_to_host_id) pip" +
                " SET hi.services=pip.services_name where hi.host_id=pip.host_id;");

        Util.safeClose(conn);
    }

    //生成主机总览信息，可重复执行
    public static void updateHostInformation() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_host_information", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);
        //清除cmdb_host_ip临时表
        //commonDAO.executeBySql("TRUNCATE table cmdb_host_ip;");
        //commonDAO.executeBySql("delete from cmdb_host_ip;");

        //清除cmdb_host_information临时表
        //commonDAO.executeBySql("TRUNCATE table cmdb_host_information;");
        //commonDAO.executeBySql("delete from cmdb_host_information;");


        //删除不存在的主机记录
        commonDAO.executeBySql("DELETE i from cmdb_host_ip as i" +
                " LEFT JOIN cmdb_host as h on i.host_id=h.id" +
                " where h.id is null;");
        commonDAO.executeBySql("DELETE i from cmdb_host_information as i" +
                " LEFT JOIN cmdb_host as h on i.host_id=h.id" +
                " where h.id is null;");

        //清空更新状态
        commonDAO.executeBySql("update cmdb_host_information set update_status=0");
        commonDAO.executeBySql("update cmdb_host_ip set update_status=0");

        //添加主机记录
        commonDAO.executeBySql("insert into cmdb_host_information (host_id,host_name,host_type,logic_area,company,platform_name,project_name,os_name,app_type,update_status,security_baseline) " +
                " select h.id,h.host_name,h.host_type,l.area_name,p.company,p.platform_name,p.multiple_chief_platform,os.operate_system_detail,h.application_type,1,h.security_baseline from cmdb_host h" +
                " LEFT JOIN cmdb_logic_area l on l.id=h.belong_to_logic_area_id" +
                " LEFT JOIN cmdb_operate_system os on os.id=h.belong_to_os_id" +
                " LEFT JOIN cmdb_platform p on p.id=h.belong_to_platform_id" +
                " LEFT JOIN cmdb_host_information i on i.host_id=h.id" +
                " where i.host_id is null");

        //更新主机信息
        commonDAO.executeBySql("update cmdb_host_information i,(" +
                "select h.id,h.host_name,h.host_type,l.area_name,p.company,p.platform_name,p.multiple_chief_platform," +
                "os.operate_system_detail,h.application_type,h.security_baseline from cmdb_host h" +
                " LEFT JOIN cmdb_logic_area l on l.id=h.belong_to_logic_area_id" +
                " LEFT JOIN cmdb_operate_system os on os.id=h.belong_to_os_id" +
                " LEFT JOIN cmdb_platform p on p.id=h.belong_to_platform_id" +
                ") t set i.host_type=t.host_type,i.logic_area=t.area_name,i.company=t.company,i.platform_name=t.platform_name,i.project_name=t.multiple_chief_platform," +
                "i.os_name=t.operate_system_detail,i.app_type=t.application_type,i.update_status=1,i.security_baseline=t.security_baseline" +
                " where i.host_id=t.id");

        //整理ip地址信息

        commonDAO.executeBySql("update cmdb_host_ip i,(" +
                " select i.host_id as host_id,n.ip as ip,p.pool_type from cmdb_host_information i" +
                " LEFT JOIN cmdb_device_logic_interface l on l.belongs_device_id=i.host_id and l.interface_device_type=0" +
                " LEFT JOIN cmdb_network_ip n on n.id=l.ip_id" +
                " INNER JOIN cmdb_network_ip_pool p on n.belongs_ip_pool_id=p.id" +
                " where n.ip is not null ) t set i.update_status=1,i.ip_type=t.pool_type where i.host_id=t.host_id and i.ip=t.ip;");

        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select i.host_id as host_id,n.ip as ip,p.pool_type,1 from cmdb_host_information i" +
                " LEFT JOIN cmdb_device_logic_interface l on l.belongs_device_id=i.host_id and l.interface_device_type=0" +
                " LEFT JOIN cmdb_network_ip n on n.id=l.ip_id" +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=i.host_id and hi.ip=n.ip" +
                " INNER JOIN cmdb_network_ip_pool p on n.belongs_ip_pool_id=p.id" +
                " where n.ip is not null and hi.host_id is null;");


        //整理负载vip地址信息 -- 30
        commonDAO.executeBySql("update cmdb_host_ip i,(" +
                " select DISTINCT hip.host_id as host_id, n.load_ip as load_ip,30 as pool_type from cmdb_network_load n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.real_ip)>0 ) t " +
                "set i.update_status=1,i.ip_type=t.pool_type where i.host_id=t.host_id and i.ip=t.load_ip;");

        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select DISTINCT hip.host_id as host_id, n.load_ip as load_ip,30,1 from cmdb_network_load n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.real_ip)>0 " +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=hip.host_id and hi.ip=n.load_ip" +
                " where hi.host_id is null;");


        //整理集群vip地址信息 -- 31
/*        commonDAO.executeBySql("update cmdb_host_ip hi,(" +
                " select DISTINCT h.belong_to_host_id as host_id, i.ip as ip,31 as pool_type from cmdb_vip_service_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_network_ip_id=i.id" +
                " JOIN cmdb_service_host h on h.id=v.belong_to_service_host_id) t " +
                " set hi.update_status=1,hi.ip_type=t.pool_type where hi.host_id=t.host_id and hi.ip=t.ip;");
        */
        commonDAO.executeBySql("update cmdb_host_ip hi,(" +
                " select DISTINCT v.belong_to_host_id as host_id, i.ip as ip,31 as pool_type " +
                "from cmdb_vip_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_net_id=i.id" +
                ") t " +
                " set hi.update_status=1,hi.ip_type=t.pool_type where hi.host_id=t.host_id and hi.ip=t.ip;");

/*        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select DISTINCT h.belong_to_host_id as host_id, i.ip as ip,31,1 from cmdb_vip_service_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_network_ip_id=i.id" +
                " JOIN cmdb_service_host h on h.id=v.belong_to_service_host_id" +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=h.belong_to_host_id and hi.ip=i.ip" +
                " where hi.host_id is null;");
        */
        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select DISTINCT v.belong_to_host_id as host_id, i.ip as ip,31,1 from cmdb_vip_host v" +
                " JOIN cmdb_network_ip i on v.belong_to_net_id=i.id" +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=v.belong_to_host_id and hi.ip=i.ip" +
                " where hi.host_id is null;");

        //整理公网ip地址信息 -- 32
        commonDAO.executeBySql("update cmdb_host_ip hi,(" +
                " select DISTINCT hip.host_id as host_id, n.nat_before_ip as load_ip,32 as pool_type from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on hip.ip=n.nat_after_ip and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " where p.pool_area=6) t " +
                " set hi.update_status=1,hi.ip_type=t.pool_type where hi.host_id=t.host_id and hi.ip=t.load_ip");

        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select DISTINCT hip.host_id, n.nat_before_ip,32,1 from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on hip.ip=n.nat_after_ip and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=hip.host_id and hi.ip=n.nat_before_ip" +
                " where p.pool_area=6 and hi.host_id is null");

        //整理边界ip地址信息 -- 33
        commonDAO.executeBySql("update cmdb_host_ip hi,(" +
                " select DISTINCT hip.host_id as host_id, n.nat_before_ip as load_ip,33 as pool_type from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.nat_after_ip)>0 and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " where p.pool_area=5) t " +
                " set hi.update_status=1,hi.ip_type=t.pool_type where hi.host_id=t.host_id and hi.ip=t.load_ip;");

        commonDAO.executeBySql("insert into cmdb_host_ip (host_id,ip,ip_type,update_status)" +
                " select DISTINCT hip.host_id as host_id, n.nat_before_ip as load_ip,33,1 from cmdb_network_nat n" +
                " INNER JOIN cmdb_host_ip hip on FIND_IN_SET(hip.ip,n.nat_after_ip)>0 and n.nat_before_ip!='Any'" +
                " INNER JOIN cmdb_network_ip i on n.nat_before_ip=i.ip" +
                " INNER JOIN cmdb_network_ip_pool p on i.belongs_ip_pool_id=p.id" +
                " LEFT JOIN cmdb_host_ip hi on hi.host_id=hip.host_id and hi.ip=n.nat_before_ip" +
                " where p.pool_area=5 and hi.host_id is null;");

        //删除多余的主机信息
        commonDAO.executeBySql("DELETE from cmdb_host_information where update_status=0");

        //删除多余的IP信息
        commonDAO.executeBySql("DELETE from cmdb_host_ip where update_status=0");

        //补充ip地址详细信息
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "JOIN cmdb_network_ip i on i.ip=hip.ip " +
                "join cmdb_network_ip_pool p on p.id=i.belongs_ip_pool_id " +
                "LEFT JOIN cmdb_network_vlan v on p.belongs_vlan_id=v.id " +
                "set hip.ip_id=i.id,hip.netmask=p.netmask,hip.gateway=p.gateway,hip.vlan=v.vlan,hip.ip_show=hip.ip");

        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "JOIN cmdb_network_ip i on i.ip=hip.ip " +
                "join cmdb_network_ip_pool p on p.id=i.belongs_ip_pool_id " +
                "set hip.ip_type=p.pool_type " +
                "where hip.ip_type not in(31,33)");//集群ip及边界ip不更新类别

        //补充负载ip信息
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "JOIN cmdb_network_ip i on i.ip=hip.ip " +
                "join cmdb_network_ip_pool p on p.id=i.belongs_ip_pool_id " +
                "set hip.ip_show=CONCAT(hip.ip,':','纵向') " +
                "where hip.ip_type=24 and p.bearer_service like '%纵向%'");
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "JOIN cmdb_network_ip i on i.ip=hip.ip " +
                "join cmdb_network_ip_pool p on p.id=i.belongs_ip_pool_id " +
                "set hip.ip_show=CONCAT(hip.ip,':','横向') " +
                "where hip.ip_type=24 and p.bearer_service like '%横向%'");

        //修正公网IP显示
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "set hip.ip_show=CONCAT(hip.ip,':','电信') " +
                "where hip.ip_type=13");
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "set hip.ip_show=CONCAT(hip.ip,':','联通') " +
                "where hip.ip_type=20");
        commonDAO.executeBySql("update cmdb_host_ip hip " +
                "set hip.ip_show=CONCAT(hip.ip,':','三线BGP') " +
                "where hip.ip_type=19");

        //开始回填ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type not in (2,11,18,5,24,13,19,20,31,33) GROUP BY hip.host_id) pip" +
                " SET hi.ip=pip.ips where hi.host_id=pip.host_id;");
        //补充备份ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type in (2,11,18) GROUP BY hip.host_id) pip" +
                " SET hi.backup_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充心跳ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type=5 GROUP BY hip.host_id) pip" +
                " SET hi.heartbeat_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充负载vip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type=24 GROUP BY hip.host_id) pip" +
                " SET hi.load_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充集群vip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type=31 GROUP BY hip.host_id) pip" +
                " SET hi.vip_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充公网ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type in (13,19,20) GROUP BY hip.host_id) pip" +
                " SET hi.public_ip=pip.ips where hi.host_id=pip.host_id;");
        //补充边界ip地址信息
        commonDAO.executeBySql("update cmdb_host_information hi,(SELECT hip.host_id,GROUP_CONCAT(hip.ip_show) as ips from cmdb_host_ip hip where hip.ip_type=33 GROUP BY hip.host_id) pip" +
                " SET hi.border_ip=pip.ips where hi.host_id=pip.host_id;");

        //补充服务信息
        commonDAO.executeBySql("update cmdb_host_information hi," +
                " (SELECT sh.belong_to_host_id as host_id,GROUP_CONCAT(s.service_name) as services_name " +
                " from cmdb_service_host sh" +
                " LEFT JOIN cmdb_service s on sh.belong_to_service_id=s.id " +
                " where sh.belong_to_host_id is not null" +
                " GROUP BY sh.belong_to_host_id) pip" +
                " SET hi.services=pip.services_name where hi.host_id=pip.host_id;");

        //补充域组信息
        commonDAO.executeBySql("update cmdb_host_information chi " +
                "JOIN (select GROUP_CONCAT(dg.group_name) as group_name,dgwh.host_id from cmdb_domain_group_with_host dgwh " +
                "LEFT JOIN cmdb_domain_group dg on dgwh.domaingroup_id=dg.id " +
                "group by dgwh.host_name) dg on dg.host_id=chi.host_id " +
                "set chi.domain_group=dg.group_name;");

        Util.safeClose(conn);
    }

    public static void updateFlowCost() {
        System.out.println(Util.now() + " Ready to insert/update flow's cost records.");
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("glob_process_cost", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        ResultSet rs = commonDAO.selectBySql("select p.process_instance_id,p.start_time,p.end_time,p.calc_end_time,c.process_status " +
                "from view_activiti_process_hi p LEFT JOIN glob_process_cost c on p.process_instance_id=c.process_instance_id");
        int recordCount = 0;
        try {
            String end_time = null;
            String status = null;
            while (rs.next()) {
                String processStatus = rs.getString("process_status");
                end_time = rs.getString("end_time");
                if (end_time == null) {
                    end_time = rs.getString("calc_end_time");
                    status = "0";
                } else {
                    status = "1";
                }

                if (processStatus == null) {
                    commonDAO.executeBySql("insert into glob_process_cost (process_instance_id,cost_time,process_status)" +
                            " values('" + rs.getString("process_instance_id") + "'," + Util.workDateDiff(rs.getString("start_time"), end_time) + "," + status + ")");
                    recordCount++;
                } else {
                    if (processStatus.compareTo("1") < 0) {
                        commonDAO.executeBySql("update glob_process_cost set cost_time=" + Util.workDateDiff(rs.getString("start_time"), end_time) +
                                ",process_status=" + status +
                                " where process_instance_id='" + rs.getString("process_instance_id") + "'");
                        recordCount++;
                    }
                }
            }

            rs.close();
            System.out.println(Util.now() + " Total insert/update " + recordCount + " records.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static void updateTaskCost() {
        System.out.println(Util.now() + " Ready to insert/update task's cost records.");
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("glob_task_cost", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        ResultSet rs = commonDAO.selectBySql("select t.task_id,t.start_time,t.end_time,t.calc_end_time,c.task_status from view_activiti_task_hi t" +
                " LEFT JOIN glob_task_cost c on t.task_id=c.task_instance_id ");
        int recordCount = 0;
        try {
            String end_time = null;
            String status = null;
            while (rs.next()) {
                String taskStatus = rs.getString("task_status");
                end_time = rs.getString("end_time");
                if (end_time == null) {
                    end_time = rs.getString("calc_end_time");
                    status = "0";
                } else {
                    status = "1";
                }

                if (taskStatus == null) {
                    commonDAO.executeBySql("insert into glob_task_cost (task_instance_id,cost_time,task_status)" +
                            " values('" + rs.getString("task_id") + "'," + Util.workDateDiff(rs.getString("start_time"), end_time) + "," + status + ")");
                    recordCount++;
                } else {
                    if (taskStatus.compareTo("1") < 0) {
                        commonDAO.executeBySql("update glob_task_cost set cost_time=" + Util.workDateDiff(rs.getString("start_time"), end_time) +
                                ",task_status=" + status +
                                " where task_instance_id='" + rs.getString("task_id") + "'");
                        recordCount++;
                    }
                }
            }

            rs.close();
            System.out.println(Util.now() + " Total insert/update " + recordCount + " records.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static void updateGuaranteeStatus() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_device_information", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        commonDAO.executeBySql("update cmdb_device_information cdi " +
                "set cdi.guarantee_status=1 " +
                "where cdi.outof_guarantee_deadline<SYSDATE() and cdi.guarantee_status=0");

        Util.safeClose(conn);
    }

    public static void updateVmData() {
        System.out.println(Util.now() + " Ready to reload vm's datas.");
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("vm_folder", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        try {
            JSONObject vmObj = VmwareApi.getFolderList();
            JSONArray list = null;
            if (vmObj != null) {
                commonDAO.executeBySql("delete from vm_folder");
                list = vmObj.getJSONArray("data");
                for (int i = 0; i < list.size(); i++) {
                    commonDAO.executeBySql("insert into vm_folder (folder) values('" +
                            list.getJSONObject(i).getString("name") + "')");
                }
            }

            vmObj = VmwareApi.getClusterList(null, null);
            if (vmObj != null) {
                commonDAO.executeBySql("delete from vm_cluster");
                list = vmObj.getJSONArray("data");
                for (int i = 0; i < list.size(); i++) {
                    commonDAO.executeBySql("insert into vm_cluster (cluster,datacenter,vcenter) values('" +
                            list.getJSONObject(i).getString("cluster_name") + "','" + list.getJSONObject(i).getString("datacenter") + "','" + list.getJSONObject(i).getString("vcenter") + "')");
                }
            }

            vmObj = VmwareApi.getDataStoreList(null, null, null, null);
            if (vmObj != null) {
                commonDAO.executeBySql("delete from vm_datastore");
                list = vmObj.getJSONArray("data");
                for (int i = 0; i < list.size(); i++) {
                    commonDAO.executeBySql("insert into vm_datastore (datastore,datacenter,vcenter,capacity,freespace) values('" +
                            list.getJSONObject(i).getString("datastore_name") + "','" + list.getJSONObject(i).getString("datacenter") +
                            "','" + list.getJSONObject(i).getString("vcenter") + "'," + list.getJSONObject(i).getString("capacity") +
                            "," + list.getJSONObject(i).getString("freespace") + ")");
                }
            }

            ResultSet rs = commonDAO.selectBySql("select vcenter from vm_cluster group by vcenter");
            if (rs != null) {
                commonDAO.executeBySql("delete from vm_portgroup");

                while (rs.next()) {
                    vmObj = VmwareApi.getPortGroupList(null, null, rs.getString("vcenter"), null);
                    if (vmObj != null) {
                        list = vmObj.getJSONArray("data");
                        for (int i = 0; i < list.size(); i++) {
                            //String cl="";
                            for (Object t : list.getJSONObject(i).getJSONArray("cluster").toArray()) {
                                //cl+=(String)t+",";
                                commonDAO.executeBySql("insert into vm_portgroup (portgroup,vlan,cluster,datacenter,vcenter) values('" +
                                        list.getJSONObject(i).getString("name") + "','" + list.getJSONObject(i).getString("vlan") +
                                        "','" + (String) t + "','" + list.getJSONObject(i).getString("datacenter") + "','" + list.getJSONObject(i).getString("vcenter") + "')");

                            }
                            //if(cl.length()>0)cl=cl.substring(0,cl.length()-1);
                        }
                    }
                }
                rs.close();
            }
            System.out.println(Util.now() + " Reload vm's datas is done.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static int initNetworkPolicy() {
        Connection conn = ItilDataSource.newInstance().getConn();
        int succ = 0;
        int buf = 0;
        String tmpIp = null;
        String tmpPort = null;
        String tmpPort2 = null;
        try {
            //this.connection.setAutoCommit(false);
            CommonDAO commonDAO = new CommonDAO("cmdb_network_policy_s1", conn);
            commonDAO.setSqlLog(false);
            ResultSet rs = commonDAO.selectBySql("select * from cmdb_network_policy_s1");
            int batch = 0;
            commonDAO.setAutoCommit(false);
            while (rs.next()) {
                tmpPort = rs.getString("port");
                if (tmpPort != null) {
                    tmpPort2 = "";
                    for (String p : tmpPort.split(",")) {
                        if (p.length() > 0) {
                            try {
                                if (Integer.valueOf(p) > 0) {
                                    tmpPort2 += "tcp" + p + ",";
                                }
                            } catch (Exception e) {
                                tmpPort2 += p + ",";
                            }
                        }
                    }
                    if (tmpPort2.length() > 0) {
                        tmpPort2 = tmpPort2.substring(0, tmpPort2.length() - 1);
                    }
                }
                tmpIp = rs.getString("src_ip");

                if (tmpIp != null) {
                    for (String t : tmpIp.split(",")) {
                        if (t.length() > 0) {
                            commonDAO.executeBySql("insert into cmdb_network_policy_s2 (src_ip,dst_ip,remarks,direct,type,port,platform_name) " +
                                    "values('" + t + "','" + rs.getString("dst_ip") + "','" + rs.getString("remarks") + "','" + rs.getString("direct") + "'," +
                                    "'" + rs.getString("type") + "','" + tmpPort2 + "','" + rs.getString("platform_name") + "')");
                            if (batch++ > 50) {
                                commonDAO.commit();
                                batch = 0;
                            }
                        }
                    }
                }
            }
            if (batch > 0) {
                commonDAO.commit();
            }

            rs = commonDAO.selectBySql("select * from cmdb_network_policy_s2");
            commonDAO.commit();
            batch = 0;
            while (rs.next()) {
                tmpIp = rs.getString("dst_ip");
                if (tmpIp != null) {
                    for (String t : tmpIp.split(",")) {
                        if (t.length() > 0) {
                            commonDAO.executeBySql("insert into cmdb_network_policy_s3 (dst_ip,src_ip,remarks,direct,type,port,platform_name) " +
                                    "values('" + t + "','" + rs.getString("src_ip") + "','" + rs.getString("remarks") + "','" + rs.getString("direct") + "'," +
                                    "'" + rs.getString("type") + "','" + rs.getString("port") + "','" + rs.getString("platform_name") + "')");
                            if (batch++ > 50) {
                                commonDAO.commit();
                                batch = 0;
                            }
                        }
                    }
                }
            }
            if (batch > 0) {
                commonDAO.commit();
            }
            commonDAO.setAutoCommit(true);
            rs.close();
            Util.safeClose(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return succ;
    }

    public static void updateNetworkPolicy() {
        Connection conn = ItilDataSource.newInstance().getConn();

        CommonDAO commonDAO = new CommonDAO("cmdb_host_information", conn);
        commonDAO.setSqlLog(false);
        commonDAO.setWrite2usp(false);

        try {
/*            ResultSet rs =commonDAO.selectBySql("select * from cmdb_network_ip_pool p");
            HashMap<String,String> poolMap = new HashMap<String,String>();
            while (rs.next()){
                poolMap.put(rs.getString("network_num"),rs.getString("pool_area"));
            }
            rs.close();*/

            ResultSet rs = commonDAO.selectBySql("select DISTINCT p.id,p.src_ip,p.dst_ip,nipl.pool_area as src_pool_area,nipl2.pool_area as dst_pool_area," +
                    "hif.platform_name as src_platform_name,hif2.platform_name as dst_platform_name," +
                    "hif.project_name as src_project_name,hif2.project_name as dst_project_name " +
                    "from cmdb_network_policy p " +
                    "LEFT JOIN cmdb_network_ip nip on p.inter_src_ip=nip.ip " +
                    "LEFT JOIN cmdb_network_ip_pool nipl on nip.belongs_ip_pool_id=nipl.id " +
                    "LEFT JOIN cmdb_network_ip nip2 on p.inter_dst_ip=nip2.ip " +
                    "LEFT JOIN cmdb_network_ip_pool nipl2 on nip2.belongs_ip_pool_id=nipl2.id " +
                    "LEFT JOIN cmdb_host_ip hip on p.inter_src_ip=hip.ip " +
                    "LEFT JOIN cmdb_host_information hif on hip.host_id=hif.host_id " +
                    "LEFT JOIN cmdb_host_ip hip2 on p.inter_dst_ip=hip2.ip " +
                    "LEFT JOIN cmdb_host_information hif2 on hip2.host_id=hif2.host_id " +
                    "where p.direct is null");

            while (rs.next()) {
                String id = rs.getString("id");
                String src_ip = rs.getString("src_ip");
                String dst_ip = rs.getString("dst_ip");
                String src_platform_name = rs.getString("src_platform_name");
                String dst_platform_name = rs.getString("dst_platform_name");
                String src_project_name = rs.getString("src_project_name");
                String dst_project_name = rs.getString("dst_project_name");

/*                String inter_src_ip,inter_dst_ip;

                List<String> src_ip_list = IpUtil.grenerateIpMask(src_ip);
                if(src_ip_list != null)inter_src_ip=src_ip_list.get(1);

                List<String> dst_ip_list = IpUtil.grenerateIpMask(dst_ip);
                if(dst_ip_list != null)inter_dst_ip=dst_ip_list.get(1);*/

//                String src_ip_pool_value=poolMap.get(IpUtil.getIpPoolNum(src_ip));
//                String dst_ip_pool_value=poolMap.get(IpUtil.getIpPoolNum(dst_ip));
                int src_pool_area = Util.str2int(rs.getString("src_pool_area"), 0);
                int dst_pool_area = Util.str2int(rs.getString("dst_pool_area"), 0);
                String src_area = "外网", dst_area = "外网";

                if (src_pool_area != 0) {
                    src_area = "内网";
                }
                if (dst_pool_area != 0) {
                    dst_area = "内网";
                }
                if (src_pool_area == 5 || dst_pool_area == 5) {
                    if (src_pool_area == 5) {
                        src_area = "合作单位";
                        dst_area = "网金";
                    }
                    if (dst_pool_area == 5) {
                        src_area = "网金";
                        dst_area = "合作单位";
                    }
                }
                String direct = src_area + "->" + dst_area;

                int policy_type = 0;

                if (src_platform_name != null && dst_platform_name != null && src_platform_name.equals(dst_platform_name)) {
                    policy_type = 4;    //"系统内部"
                } else if ("内网".equals(src_area) && "内网".equals(dst_area)) {
                    if ((src_project_name != null && src_project_name.contains("公共服务")) || (dst_project_name != null && dst_project_name.contains("公共服务"))) {
                        policy_type = 6;    //"公共服务"
                    } else if (src_platform_name == null || dst_platform_name == null) {
                        policy_type = 0;
                    } else {
                        policy_type = 5;    //"跨平台"
                    }
                }
                if ((src_platform_name != null && src_platform_name.contains("跳板机")) || (dst_platform_name != null && dst_platform_name.contains("跳板机"))) {
                    policy_type = 3;     //"跳板机"
                }
                if ("外网".equals(src_area) || "外网".equals(dst_area)) {
                    policy_type = 2;    //"公网"
                }
                if (src_pool_area == 5 || dst_pool_area == 5) {
                    policy_type = 1;    //边界
                }
//// TODO: 2017/9/12 更新相应信息到策略表
                commonDAO.executeBySql("update cmdb_network_policy p set p.direct='" + direct + "',p.network_strategy=" + policy_type + " where p.id=" + id);
            }
        } catch (SQLException se) {
            logger.error(se.getMessage());
        }
        Util.safeClose(conn);
    }

    public static int updateGroupName() {
        int retValue = -1;

        LdapApi ldapApi = new LdapApi();
        List<String> groupList = ldapApi.getLdapGroup();
        if (groupList.size() > 0) {
            Connection conn = ItilDataSource.newInstance().getConn();
            CommonDAO commonDAO = new CommonDAO("cmdb_domain_group", conn);
            commonDAO.setSqlLog(false);
            commonDAO.setWrite2usp(false);
            int count = 0;
            for (String groupName : groupList) {
                ResultSet rs = commonDAO.selectBySql("select id from cmdb_domain_group where group_name='" + Util.mysqlEscape(groupName) + "'");
                try {
                    if (rs != null && rs.next()) {
                        logger.debug("Group_name:" + groupName + " is exist !");
                        rs.close();
                    } else {
                        if (commonDAO.executeBySql("insert into cmdb_domain_group (group_name) values('" + Util.mysqlEscape(groupName) + "')") > 0) {
                            count++;
                        }
                    }
                } catch (SQLException e) {
                    logger.error("ERROR at updateGroupName:" + e.getMessage());
                }
            }
            logger.debug("Success write [" + count + "] records to cmdb .");
            Util.safeClose(conn);
        }
        ldapApi.closeLdapContext();
        return retValue;
    }

    public static void main(String[] avgs) {
        //Automate.updateVmData();
        //Automate.initCabinet();
        //Automate.freshCabinetUse();
        //Automate.countCabinetMaxFreeU(0);
        //Automate.updateHostInformation2();
        //Automate.incTablevision();
        //Automate.freeHostname();
        //Automate.initWorkTimeTable("2017-01-01 00:00:00", "2017-06-30 23:59:00", 1800);
        //Automate.updateFlowCost();
        //Automate.updateTaskCost();
        //System.out.println(Util.workDateDiff("2017-05-27 13:47:44","2017-05-30 06:31:06"));
        //System.out.println("Dict(cmdb_network_ip_type,web):"+Dict.g("cmdb_network_ip_type","WEB"));
        //System.out.println("AllocIp:" + Automate.allocIp("PA0WW01379", "柳州银行", 1));
        //System.out.println("AllocIp:" + Automate.allocIp("PA0WW00168", "柳州银行", 2));
        //System.out.println("AllocIp:" + Automate.allocIp("UC0WD00319", "柳州银行", 3));
        //Automate.initNetworkPolicy();
        //Automate.freeAllocIp();
        //System.out.println(Util.redisDecode(Util.redisEncode("lkj098|324|kltg09")));
    }
}
