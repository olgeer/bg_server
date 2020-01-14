package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.common.Configure;

import java.util.Properties;

/**
 * Created by Max on 2017/6/27.
 */
public class VmwareApi {
    private static String url = "https://devops.wangjin.local/";
    private static String charset = "utf-8";
    private static HttpsClient httpsClient = null;

    static {
        if (Configure.getConfig().getProperty("vm.url") != null) url = Configure.getConfig().getProperty("vm.url");
        if (Configure.getConfig().getProperty("vm.charset") != null)
            charset = Configure.getConfig().getProperty("vm.charset");
        httpsClient = new HttpsClient();
    }

    public static JSONObject getFolderList() {
        JSONObject retJson = null;
        String body = "{\"action\": \"vmware.folder\"}";
        retJson = JSON.parseObject(httpsClient.doPost(url, body, charset));
        return retJson;
    }

    public static JSONObject getClusterList(String cluster, String datacenter) {
        JSONObject retJson = null;
        JSONObject body = JSON.parseObject("{\"action\": \"vmware.cluster\"}");
        JSONObject filter = new JSONObject();
        if (cluster != null) filter.put("cluster", cluster);
        if (datacenter != null) filter.put("datacenter", datacenter);
        body.put("filter", filter);
        retJson = JSON.parseObject(httpsClient.doPost(url, body.toJSONString(), charset));
        return retJson;
    }

    public static JSONObject getDataStoreList(String cluster, String datacenter, String vcenter, String datastore) {
        JSONObject retJson = null;
        JSONObject body = JSON.parseObject("{\"action\": \"vmware.datastore\"}");
        JSONObject filter = new JSONObject();
        if (cluster != null) filter.put("cluster", cluster);
        if (datacenter != null) filter.put("datacenter", datacenter);
        if (vcenter != null) filter.put("vcenter", vcenter);
        if (datastore != null) filter.put("datastore", datastore);
        body.put("filter", filter);
        retJson = JSON.parseObject(httpsClient.doPost(url, body.toJSONString(), charset));
        return retJson;
    }

    public static JSONObject getPortGroupList(String cluster, String datacenter, String vcenter, String vlan) {
        JSONObject retJson = null;
        JSONObject body = JSON.parseObject("{\"action\": \"vmware.portgroup\"}");
        JSONObject filter = new JSONObject();
        if (cluster != null) filter.put("cluster", cluster);
        if (vlan != null) filter.put("vlan", vlan);
        if (vcenter != null) filter.put("vcenter", vcenter);
        if (datacenter != null) filter.put("datacenter", datacenter);
        body.put("filter", filter);
        retJson = JSON.parseObject(httpsClient.doPost(url, body.toJSONString(), charset));
        return retJson;
    }

    public static JSONObject getVcenterList() {
        JSONObject retJson = null;
        String body = "{\"action\": \"vmware.vcenter\"}";
        retJson = JSON.parseObject(httpsClient.doPost(url, body, charset));
        return retJson;
    }

    public static String access(APIRequest params) {
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
            case "getfolder": {
                JSONObject retJson = getFolderList();
                if (retJson != null) {
                    JSONArray fds = retJson.getJSONArray("data");
                    String[] fdList = new String[fds.size()];
                    for (int i = 0; i < fds.size(); i++) {
                        fdList[i] = fds.getJSONObject(i).getString("name");
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(JSONObject.toJSONString(fdList));
                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"Folder获取失败！\"}");
                }
                break;
            }
            case "getcluster": {
                JSONObject retJson = getClusterList(paramsProperties.getProperty("_cluster"), paramsProperties.getProperty("_datacenter"));
                if (retJson != null) {
                    JSONArray cls = retJson.getJSONArray("data");
                    String[] clsList = new String[cls.size()];
                    for (int i = 0; i < cls.size(); i++) {
                        clsList[i] = cls.getJSONObject(i).getString("cluster_name") + "[" + cls.getJSONObject(i).getString("datacenter") + "/" + cls.getJSONObject(i).getString("vcenter") + "]";
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(JSONObject.toJSONString(clsList));
                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"Cluster获取失败！\"}");
                }
                break;
            }
            case "getdatastore": {
                JSONObject retJson = getDataStoreList(paramsProperties.getProperty("_cluster"), paramsProperties.getProperty("_datacenter"), paramsProperties.getProperty("_vcenter"), paramsProperties.getProperty("_datastore"));
                if (retJson != null) {
                    JSONArray cls = retJson.getJSONArray("data");
                    String[] clsList = new String[cls.size()];
                    for (int i = 0; i < cls.size(); i++) {
                        clsList[i] = cls.getJSONObject(i).getString("datastore_name") + "[可用" + cls.getJSONObject(i).getString("freespace") + "(Gb)]";
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(JSONObject.toJSONString(clsList));
                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"Datastore获取失败！\"}");
                }
                break;
            }
            case "getportgroup": {
                JSONObject retJson = getPortGroupList(paramsProperties.getProperty("_cluster"), paramsProperties.getProperty("_datacenter"), paramsProperties.getProperty("_vcenter"), paramsProperties.getProperty("_vlan"));
                if (retJson != null) {
                    JSONArray cls = retJson.getJSONArray("data");
                    String[] clsList = new String[cls.size()];
                    for (int i = 0; i < cls.size(); i++) {
                        clsList[i] = cls.getJSONObject(i).getString("name");
                    }
                    apiResponse.setReturnCode("200");
                    apiResponse.setReturnData(JSONObject.toJSONString(clsList));
                } else {
                    apiResponse.setReturnCode("505");
                    apiResponse.setReturnData("{\"result\":0,\"msg\":\"Portgroup获取失败！\"}");
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
