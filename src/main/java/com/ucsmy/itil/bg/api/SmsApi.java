package com.ucsmy.itil.bg.api;

import com.ucsmy.itil.bg.common.Configure;
import com.ucsmy.itil.bg.common.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Max on 2016/12/13.
 */
public class SmsApi {

    //private static String url = "http://172.17.16.56:80/itil/message/messct/reviceMes";
    //private static String url = "http://127.0.0.1/message/messct/reviceMes";
    //private static String url = "http://172.17.21.178:8080/itil/message/messct/reviceMes";
    private static Logger logger = LogManager.getLogger(SmsApi.class.getName());

    public String sendSms(String jsonpStr) {
        String retStr = null;
        //logger.debug("SendSMS source json string:" + jsonpStr);
        Properties smsConfig = Configure.getConfig();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(smsConfig.getProperty("sms.url"));

        try {
            List formparams = new ArrayList();
            String preSendStr = Util.encodeStr(Util.encodeStr(jsonpStr));
            formparams.add(new BasicNameValuePair("data", preSendStr));

/*
            byte[] b = jsonpStr.getBytes("UTF-8");
            Base64 base64 = new Base64();
            b = base64.encode(b);
            byte[] b1 = base64.encode(b);
            formparams.add(new BasicNameValuePair("data", new String(b1)));
*/

            UrlEncodedFormEntity uefEntity;
            uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(uefEntity);

            // logger.debug("executing request " + httppost.getURI());

            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    retStr = EntityUtils.toString(entity, "UTF-8");
                    logger.debug("APIResponse content: " + retStr);
                }
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return retStr;
    }
}
