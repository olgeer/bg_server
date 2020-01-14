package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;
import com.ucsmy.itil.bg.common.Util;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 2016/12/13.
 */
public class Sms {
    private String mstip = null;
    private Receiver mail;
    private Receiver sms;
    private String tekon = null;
    private List<SmsBody> data;

    public Receiver getMail() {
        return mail;
    }

    public void setMail(Receiver mail) {
        this.mail = mail;
    }

    public Receiver getSms() {
        return sms;
    }

    public void setSms(Receiver sms) {
        this.sms = sms;
    }

    private class SmsBody {
        private String title = null;
        private String detail = null;
        private String send_user_id = null;
        private String sendtime = null;
        private String relation_domain = "";
        private String relation_domain_id = "";

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getSend_user_id() {
            return send_user_id;
        }

        public void setSend_user_id(String send_user_id) {
            this.send_user_id = send_user_id;
        }

        public String getSendtime() {
            return this.sendtime;
        }

        public void setSendtime(String sendtime) {
            this.sendtime = sendtime;
        }

        public String getRelation_domain() {
            return relation_domain;
        }

        public void setRelation_domain(String relation_domain) {
            this.relation_domain = relation_domain;
        }

        public String getRelation_domain_id() {
            return relation_domain_id;
        }

        public void setRelation_domain_id(String relation_domain_id) {
            this.relation_domain_id = relation_domain_id;
        }

        public SmsBody(String t, String d, String suid, String st, String rd, String rdid) {
            this.title = t;
            this.detail = d;
            this.send_user_id = suid;
            this.sendtime = st;
            this.relation_domain = rd;
            this.relation_domain_id = rdid;
        }
    }

    public Sms() {
        this.tekon = "123456";
        this.data = (List<SmsBody>) new ArrayList<SmsBody>();
    }

    public String getTekon() {
        return tekon;
    }

    public void setTekon(String tekon) {
        this.tekon = tekon;
    }

    public String getMstip() {
        return mstip;
    }

    public void setMstip(String mstip) {
        this.mstip = mstip;
    }

    public List<SmsBody> getData() {
        return data;
    }

    public void setData(List<SmsBody> data) {
        this.data = data;
    }

    public void addSmsBody(String titl, String date, String sendUserId, String st, String relationDomain, String relationDomainId) {
        SmsBody smsBody = new SmsBody(titl, date, sendUserId, st, relationDomain, relationDomainId);
        this.data.add(smsBody);
    }

    public void addSmsReceiver(String receiverJson) {
        this.sms = JSON.parseObject(receiverJson, Receiver.class);
    }

    public void addMailReceiver(String receiverJson) {
        this.mail = JSON.parseObject(receiverJson, Receiver.class);
    }
}


