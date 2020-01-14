package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ucsmy.itil.bg.common.Configure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * LDAP服务类
 *
 * @author 李智强
 * @version 20170223
 */
public class LdapApi {
    private String ldapURL = "LDAP://172.17.20.16:389";//ip:port
    private LdapContext ldapContext;
    private boolean vaild;
    private static Logger logger = LogManager.getLogger(LdapApi.class.getName());

    public LdapApi() {
        Properties property = Configure.getConfig();
        if (property.getProperty("ldap.url") != null) this.ldapURL = property.getProperty("ldap.url");

        this.ldapContext = loginLdap(property.getProperty("ldap.account"), property.getProperty("ldap.password"));
        this.vaild = this.ldapContext != null;
    }
    /**
     * 类构造方法
     * @param account   登录账号，形式为xxxxxxx@xxx.xx，其中@后的为LDAP的domain
     * @param password  密码
     */
    public LdapApi(String account, String password) {
        Properties property = Configure.getConfig();
        if (property.getProperty("ldap.url") != null) this.ldapURL = property.getProperty("ldap.url");

        this.ldapContext = loginLdap(account, password);
        this.vaild = this.ldapContext != null;
    }

    /**
     * 类构造方法
     * @param account   登录账号，形式为 xxxxxxx@xxx.xx，其中@后的为LDAP的domain
     * @param password  密码
     * @param ldapurl   LDAP服务地址，格式为 LDAP://address:port
     */
    public LdapApi(String account, String password, String ldapurl) {
        this.ldapURL = ldapurl;
        this.ldapContext = loginLdap(account, password);
        this.vaild = this.ldapContext != null;
    }

    /**
     * 获取用户账号密码的有效状态
     *
     * @return 有效则为true，否则为false
     */
    public boolean isVaild() {
        return vaild;
    }

    /**
     * 获取LDAP链接上下文
     * @return LdapContext对象，如账号密码验证失败，则返回NULL
     */
    public LdapContext getLdapContext() {
        return ldapContext;
    }

    /**
     * 获取LDAP链接
     * @return
     */
    public String getLdapURL() {

        return ldapURL;
    }

    /**
     * 断开LDAP链接
     */
    public void closeLdapContext() {
        try {
            this.ldapContext.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证用户账号密码有效的静态方法
     * @param account   登录账号，形式为 xxxxxxx@xxx.xx，其中@后的为LDAP的domain
     * @param password  密码
     * @param ldapurl   LDAP服务地址，格式为 LDAP://address:port
     * @return 有效则为true，否则为false
     */
    public static boolean loginLdap(String account, String password, String ldapurl) {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");//"none","simple","strong"
        env.put(Context.SECURITY_PRINCIPAL, account);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.PROVIDER_URL, ldapurl);
        boolean retValue = false;
        try {
            LdapContext ctx = new InitialLdapContext(env, null);
            logger.debug(account + " verify by ldap success ! ");
            retValue = true;
            ctx.close();
        } catch (Exception e) {
            //e.printStackTrace();
            //System.err.println("Problem login ldap: " + e);
            retValue = false;
        }
        return retValue;
    }

    /**
     * 登录LDAP服务器
     *
     * @param account  登录账号，形式为 xxxxxxx@xxx.xx，其中@后的为LDAP的domain
     * @param password 密码
     * @return LdapContext对象，如账号密码验证失败，则返回NULL
     */
    private LdapContext loginLdap(String account, String password) {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");//"none","simple","strong"
        env.put(Context.SECURITY_PRINCIPAL, account);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.PROVIDER_URL, ldapURL);
        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            logger.debug(account + " verify by ldap success ! ");
        } catch (Exception e) {
            //e.printStackTrace();
            logger.debug("Problem login ldap: " + account);
        }
        return ctx;
    }

    /**
     * 获取LDAP服务器上所有用户的信息JSON串
     *
     * @return 信息JSON串
     */
    public String getLdapUserInJson() {
        return JSON.toJSONString(getLdapUser(), SerializerFeature.WriteMapNullValue);
    }

    /**
     * 获取LDAP服务器上所有用户的信息
     *
     * @return List<LdapUser>
     */
    public List<LdapUser> getLdapUser() {
        String searchFilter = "(&(objectCategory=person)(objectClass=user)(name=*))";
        String searchBase = "DC=wangjin,DC=local";
        String[] attributes = {"userPrincipalName", "userAccountControl"};
        String retString = "";
        int count = 0;

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setReturningAttributes(attributes);

        List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
        try {
            NamingEnumeration<SearchResult> answer = this.ldapContext.search(searchBase, searchFilter, searchCtls);
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                //System.out.println("<<<::["+(count++)+":" + sr.getNameInNamespace()+"|"+sr.getAttributes()+"]::>>>>");
                LdapUser ldapUser = new LdapUser();
                String[] orgs = sr.getNameInNamespace().split(",");
                String ou = "";
                String dc = "";
                for (int i = 0; i < orgs.length; i++) {
                    String[] val = orgs[i].split("=");
                    switch (val[0]) {
                        case "CN":
                            ldapUser.setCn(val[1]);
                            break;
                        case "OU":
                            ou = val[1] + "/" + ou;
                            break;
                        case "DC":
                            dc = dc + "." + val[1];
                            break;
                    }
                }
                if (dc.length() > 0) dc = dc.substring(1);
                ldapUser.setDc(dc);
                if (ou.length() > 0) ou = ou.substring(0, ou.length() - 2);
                ldapUser.setOu(ou);
                Attributes attrs = sr.getAttributes();
                Attribute acc = attrs.get("userPrincipalName");
                if (acc != null) {
                    ldapUser.setAccountWithDomain((String) acc.get(0));
                    ldapUser.setAccount((String) ((String) acc.get(0)).split("@")[0]);
                }
                int status = Integer.parseInt((String) attrs.get("userAccountControl").get(0));
                ldapUser.setDisable((status & 2) == 2);
                ldapUsers.add(ldapUser);
                count++;
            }
            logger.debug("Totle record is " + count);
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Problem searching directory: " + e);
        }

        return ldapUsers;
    }

    public List<String> getLdapGroup() {
        String searchFilter = "(objectCategory=group)";
        String searchBase = "OU=MSS,OU=RSA,OU=运维,DC=wangjin,DC=local";
        int count = 0;

        Properties property = Configure.getConfig();
        if (property.getProperty("ldap.searchbase") != null) searchBase = property.getProperty("ldap.searchbase");

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        List<String> groupList = new ArrayList<String>();
        try {
            NamingEnumeration<SearchResult> answer = this.ldapContext.search(searchBase, searchFilter, searchCtls);
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                logger.debug(sr.getName());
                String groupName = sr.getName();
                groupName = groupName.substring(0, groupName.indexOf(",")).replace("CN=", "");
                groupList.add(groupName);
                count++;
            }
            logger.debug("Total [" + count + "] domain group records found !");
        } catch (NamingException e) {
            logger.error("Problem searching directory: " + e.getMessage());
        }

        return groupList;
    }

    public static void main(String[] args) {
        LdapApi ldapApi = new LdapApi("itsm_ad@wangjin.local", "IIttssmm!!0");
        //LdapApi ldapApi= new LdapApi("wangjin.local\\ucs_lizhiqiang","olgeer4444$");
        System.out.println(JSON.toJSONString(ldapApi.getLdapGroup()));
        //System.out.println(ldapApi.getLdapUserInJson());
//        if (args.length > 2) {
//            System.out.println(LdapApi.loginLdap(args[0], args[1], args[2]) ? "验证成功" : "验证失败");
//        } else {
//            System.out.println("Usage : Cmd (UserAccount) (Password) (LdapURL)");
//        }
    }
}