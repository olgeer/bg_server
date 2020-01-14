package com.ucsmy.itil.bg.service;

import com.ucsmy.itil.bg.api.LdapApi;
import com.ucsmy.itil.bg.common.DbConn;
import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.model.CommonExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * 用户认证类
 * 提供用户认证，登出，状态维持，状态查询，用户锁定/解锁，设置用户有效/无效等功能
 * @author 李智强
 * @version 20170223
 */
public class Authentication {
    private static int authTimeout = 20 * 60;     //认证有效期，单位（秒），默认为20分钟
    private static int maxFailureCount = 5;       //每天最大错误次数，超过则锁定用户状态
    private static boolean authByLdap = true;    //是否通过AD域进行认证
    private static String ldapDomain = "@wangjin.local";    //Ldap域
    private Connection connection;
    private CommonDAO commonDAO;
    private static Logger logger = LogManager.getLogger(CommonDAO.class.getName());

    public Authentication(Connection connection) {
        this.connection = connection;
        this.commonDAO = new CommonDAO("user_basic", connection);
    }

    /**
     * 登录系统，如配置为LDAP验证，则提交到AD域服务器验证
     * 如同账号连续登录失败超过5次则锁定账号，必须联系管理员解锁
     * 登录规则为单点登录，不同IP登录会踢除原登录IP的登录状态
     *
     * @param username 用户登录账号
     * @param password 登录密码
     * @param ip       登录IP，格式为 xxx.xxx.xxx.xxx
     * @return 成功返回token字符串，失败返回null
     */
    public String login(String username, String password, String ip) {
        String retValue = null;

        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo("usba_account", username);
        ResultSet resultSet = commonDAO.selectByExample(commonExample);

        try {
            if (resultSet.next()) {
                String usba_password = resultSet.getString("usba_password");
                int usba_failure_count = resultSet.getInt("usba_failure_count");
                int usba_account_expired = resultSet.getInt("usba_account_expired");
                int usba_credential_expired = resultSet.getInt("usba_credential_expired");
                int usba_account_enable = resultSet.getInt("usba_account_enable");
                int usba_account_locked = resultSet.getInt("usba_account_locked");
                //String usba_credential_token = resultSet.getString("usba_credential_token");
                String usba_credential_ip = resultSet.getString("usba_credential_ip");

                boolean authStatus = false;
                if (authByLdap) {
                    authStatus = new LdapApi(username + ldapDomain, password).isVaild();
                } else {
                    authStatus = usba_password.compareToIgnoreCase(Util.encrypt(password)) == 0 ? true : false;
                }

                if (usba_account_enable == 1 && usba_account_expired == 1 && usba_account_locked == 1 && authStatus) {
                    criteria = commonExample.createValueCriteria();
                    criteria.setKeyValue("usba_failure_count", 0);
                    criteria.setKeyValue("usba_last_active_time", Util.formatDateTime(new Date()));
                    criteria.setKeyValue("usba_credential_expired", 1);
                    retValue = Util.creatUUID();
                    criteria.setKeyValue("usba_credential_token", retValue);
                    criteria.setKeyValue("usba_credential_ip", ip);

                    commonDAO.updateByExample(commonExample);
                } else {
                    if (usba_account_locked != 0) {
                        criteria = commonExample.createValueCriteria();
                        criteria.setKeyValue("usba_failure_count", ++usba_failure_count);
                        if (usba_failure_count > maxFailureCount && usba_credential_expired == 0 && usba_credential_ip.compareTo(ip) != 0) {
                            criteria.setKeyValue("usba_account_locked", 0);
                        }
                        logger.debug("Update " + commonDAO.updateByExample(commonExample) + " record success !");
                    }
                }
                resultSet.close();
            } else {
                logger.debug("Username=" + username + " dos't exist !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retValue;
    }

    /**
     * 登出操作方法，登出失败对现有登录状态无任何影响
     * 特别说明：此处需要token及ip主要是考虑到需要确认是有效用户本人登出，避免其他用户恶意登出他人账号
     *
     * @param username 登出用户账号
     * @param token    登录时返回的有效token
     * @param ip       客户端IP
     * @return 成功登出则返回True，否则返回False
     */
    public boolean logout(String username, String token, String ip) {
        boolean retValue = false;

        //参数无效，返回
        if (username == null || token == null || ip == null) return retValue;

        CommonExample commonExample = new CommonExample();
        //设置符合的条件
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo("usba_account", username);
        criteria.andColumnEqualTo("usba_credential_token", token);
        criteria.andColumnEqualTo("usba_credential_ip", ip);
        //设置更新内容
        criteria = commonExample.createValueCriteria();
        criteria.setKeyValue("usba_credential_expired", 0);
        criteria.setKeyValue("usba_last_active_time", Util.formatDateTime(new Date()));
        if (commonDAO.updateByExample(commonExample) > 0) retValue = true;

        return retValue;
    }

    /**
     * 设置用户有效
     *
     * @param username 用户账号
     * @return 操作成功则返回True，否则返回False
     */
    public boolean enable(Object username) {
        return setColumnValueByUsername("usba_account_enable", 1, username) > 0 ? true : false;
    }

    /**
     * 设置用户无效
     *
     * @param username  用户账号
     * @return 操作成功则返回True，否则返回False
     */
    public boolean disable(Object username) {
        return setColumnValueByUsername("usba_account_enable", 0, username) > 0 ? true : false;
    }

    /**
     * 用户解锁
     *
     * @param username  用户账号
     * @return 操作成功则返回True，否则返回False
     */
    public boolean unlock(Object username) {
        return setColumnValueByUsername("usba_account_locked", 1, username) > 0 ? true : false;
    }

    /**
     * 锁定用户
     *
     * @param username  用户账号
     * @return 操作成功则返回True，否则返回False
     */
    public boolean lock(Object username) {
        return setColumnValueByUsername("usba_account_locked", 0, username) > 0 ? true : false;
    }

    public int setColumnValueByUsername(String column, Object value, Object username) {
        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo("usba_account", username);
        criteria = commonExample.createValueCriteria();
        criteria.setKeyValue(column, value);
        return commonDAO.updateByExample(commonExample);
    }

    /**
     * 后台扫描账号登录状态过期
     * 判断依据为最近活动时间距当前时间不超过30分钟（可通过设置authTimeout修改）
     *
     * @return 扫描后踢出的用户数
     */
    public int flashCredentailExpiredStatus() {
        int retValue = 0;

        Date now = new Date();
        Date expireTime = new Date();
        expireTime.setTime(now.getTime() - authTimeout * 1000);
        CommonExample commonExample = new CommonExample();
        //设置符合的条件
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnLessThan("usba_last_active_time", Util.formatDateTime(expireTime));
        criteria.andColumnEqualTo("usba_credential_expired", 1);
        //设置更新内容
        criteria = commonExample.createValueCriteria();
        criteria.setKeyValue("usba_credential_expired", 0);

        retValue = commonDAO.updateByExample(commonExample);
        return retValue;
    }

    /**
     * Token有效验证，实现功能同keepAlive方法
     *
     * @param username 登出用户账号
     * @param token    登录时返回的有效token
     * @param ip       客户端IP
     * @return 操作成功则返回True，否则返回False
     */
    public boolean tokenAvailable(String username, String token, String ip) {
        return keepAlive(username, token, ip);
    }

    /**
     * 保持用户登录状态，要求应用当用户有操作时调用此方法，确保用户登录状态有效，不被定时服务提出
     *
     * @param username 登出用户账号
     * @param token    登录时返回的有效token
     * @param ip       客户端IP
     * @return 操作成功则返回True，否则返回False
     */
    public boolean keepAlive(Object username, String token, String ip) {
        boolean retValue = false;

        //参数无效，返回
        if (username == null || token == null || ip == null) return retValue;

        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo("usba_account", username);
        ResultSet resultSet = commonDAO.selectByExample(commonExample);

        try {
            if (resultSet.next()) {
                int usba_account_expired = resultSet.getInt("usba_account_expired");
                int usba_credential_expired = resultSet.getInt("usba_credential_expired");
                int usba_account_enable = resultSet.getInt("usba_account_enable");
                int usba_account_locked = resultSet.getInt("usba_account_locked");
                String usba_last_active_time = resultSet.getString("usba_last_active_time");
                String usba_credential_token = resultSet.getString("usba_credential_token");
                String usba_credential_ip = resultSet.getString("usba_credential_ip");

                Date lastActiveTime = Util.parseDateTime(usba_last_active_time);
                Date now = new Date();

                //状态正常且认证状态未过期
                if (usba_account_enable == 1 && usba_account_locked == 1 && usba_credential_expired == 1 &&
                        token.compareTo(usba_credential_token) == 0 && usba_credential_ip.compareTo(ip) == 0) {

                    //准备设置更新的内容
                    criteria = commonExample.createValueCriteria();

                    if (now.getTime() - lastActiveTime.getTime() > authTimeout * 1000) {//判断是否已过期
                        //已过期，更新认证状态为已过期
                        criteria.setKeyValue("usba_credential_expired", 0);
                    } else {
                        //未过期，更新最近活动时间
                        criteria.setKeyValue("usba_last_active_time", Util.formatDateTime(now));
                        retValue = true;
                    }
                    commonDAO.updateByExample(commonExample);
                }

            }
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retValue;
    }

    public static void main(String[] args) {
        try {
            Connection connection = ItilDataSource.newInstance().getConn();
            System.out.println(new Authentication(connection).login("ucs_lizhiqiang", "olgeer4444$", "172.0.0.1") != null ? "Login success !" : "Login failure !");
            //System.out.println(new Authentication(connection).keepAlive("111111", "d565be3d604040ce9f977d82266eabc7", "127.0.0.1") ? "Alive success !" : "Alive failure !");
            connection.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }
}
