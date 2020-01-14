package com.ucsmy.itil.bg.execute;

import com.ucsmy.itil.bg.api.LdapApi;
import com.ucsmy.itil.bg.api.LdapUser;
import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Max on 2017/2/22.
 */
public class SyncLdapUser {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(SyncLdapUser.class.getName());
        String account = "ucs_lizhiqiang@wangjin.local";
        String password = "olgeer4444$";
        String ldapurl = "LDAP://172.17.20.16:389";

        LdapApi ldapApi = new LdapApi(account, password, ldapurl);
        List<LdapUser> ldapUserList = ldapApi.getLdapUser();
        ldapApi.closeLdapContext();

        int totalAdd = 0;
        int totalUpdate = 0;
        if (ldapUserList.size() > 0) {
            Connection conn = ItilDataSource.newInstance().getConn();
            CommonDAO commonDAO = new CommonDAO("user_basic", conn);
            CommonDAO departmentDAO = new CommonDAO("department", conn);
            CommonExample commonExample = new CommonExample();
            for (LdapUser ldapUser : ldapUserList) {
                if (ldapUser.getCn().compareTo("User") != 0 && ldapUser.getAccount() != null) {
                    commonExample.clear();
                    CommonExample.Criteria criteria = commonExample.createCriteria();
                    String[] department = ldapUser.getOu().split("/");
                    criteria.andColumnEqualTo("depa_name", department[department.length - 1]);
                    ResultSet rs = departmentDAO.selectByExample("depa_id", commonExample);
                    String depa_id = null;
                    try {
                        if (rs.next()) depa_id = rs.getString("depa_id");
                    } catch (SQLException se) {
                        logger.debug("Department[" + department[department.length - 1] + "] is not exist !");
                    } finally {
                        try {
                            rs.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    commonExample.clear();
                    criteria = commonExample.createCriteria();
                    criteria.andColumnEqualTo("usba_name", ldapUser.getCn());
                    CommonExample.Criteria valueCriteria = commonExample.createValueCriteria();
                    valueCriteria.setKeyValue("usba_account", ldapUser.getAccount());
                    //valueCriteria.setKeyValue("usba_mail", ldapUser.getAccountWithDomain());
                    valueCriteria.setKeyValue("usba_account_enable", ldapUser.isDisable() ? 0 : 1);
                    totalUpdate++;
                    if (commonDAO.updateByExample(commonExample) == 0 && ldapUser.isDisable() == false) {
                        commonExample.clear();
                        valueCriteria = commonExample.createValueCriteria();
                        valueCriteria.addKeyValue("usba_id", Util.creatUUID());
                        valueCriteria.addKeyValue("usba_account", ldapUser.getAccount());
                        valueCriteria.addKeyValue("usba_password", "f379eaf3c831b04de153469d1bec345e"); //默认密码为：666666
                        valueCriteria.addKeyValue("usba_name", ldapUser.getCn());
                        if (depa_id != null) valueCriteria.addKeyValue("depa_id", depa_id);
                        //valueCriteria.addKeyValue("usba_mail",ldapUser.getAccountWithDomain());
                        if (commonDAO.insertByExample(commonExample) == 0) {
                            commonExample.clear();
                            valueCriteria = commonExample.createValueCriteria();
                            valueCriteria.addKeyValue("usba_id", Util.creatUUID());
                            valueCriteria.addKeyValue("usba_account", ldapUser.getAccount());
                            valueCriteria.addKeyValue("usba_password", "f379eaf3c831b04de153469d1bec345e");
                            valueCriteria.addKeyValue("usba_name", ldapUser.getCn());
                            if (depa_id != null) valueCriteria.addKeyValue("depa_id", depa_id);
                            //valueCriteria.addKeyValue("usba_mail",ldapUser.getAccountWithDomain());
                            commonDAO.insertByExample(commonExample);
                        }
                        totalAdd++;
                        totalUpdate--;
                    }
                }
            }
            Util.safeClose(conn);
        }
        logger.debug("SyncLdapUser done ! Total add " + totalAdd + " records, total update " + totalUpdate + " records .");
    }
}
