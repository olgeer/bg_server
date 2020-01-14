package com.ucsmy.itil.bg.api;

import java.util.*;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/*
 * 利用HttpClient进行post请求的工具类
 */
public class HttpsClient {
    public String doPost(String url, String body, String charset) {
        CloseableHttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;
        try {
            if (url.toLowerCase().startsWith("https")) {
                httpClient = new SSLClient();
            } else {
                httpClient = HttpClients.createDefault();
            }
            httpPost = new HttpPost(url);
            StringEntity stringEntity = new StringEntity(body, charset);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, charset);
                }
                response.close();
            }
            httpClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public String doPost(String url, Map<String, String> map, String charset) {
        HttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;
        try {
            httpClient = new SSLClient();
            httpPost = new HttpPost(url);
            //设置参数
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, String> elem = (Entry<String, String>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
            }
            if (list.size() > 0) {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, charset);
                httpPost.setEntity(entity);
            }
            HttpResponse response = httpClient.execute(httpPost);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, charset);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        String url = "http://172.17.21.69:8001/vm-api";
        String charset = "utf-8";
        HttpsClient httpsClient = new HttpsClient();
        String body = "{\"action\": \"vmware.datastore\"}";

        String httpVmApiRtn = httpsClient.doPost(url, body, charset);
        System.out.println("result:" + httpVmApiRtn);
    }
}