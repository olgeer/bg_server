package com.ucsmy.itil.bg.common;

import com.ucsmy.itil.bg.api.APIRequest;
import com.ucsmy.itil.bg.model.CommonDAO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 2017/7/11.
 */
public class WhiteList {
    private static List<Rule> ruleList;
    private static UcsmyLog logger;

    static {
        loadWhiteList();
    }

    public static void loadWhiteList(){
        logger = new UcsmyLog(WhiteList.class.getName());
        Connection conn = ItilDataSource.newInstance().getConn();
        CommonDAO commonDAO = new CommonDAO("cmdb_interface_white_list", conn);
        ResultSet rs = commonDAO.selectBySql("select * from cmdb_interface_white_list order by rule_weight desc");

        try {
            if (rs != null) {
                ruleList = new ArrayList<Rule>();
                while (rs.next()) {
                    String ips = rs.getString("ips");
                    String interfaces = rs.getString("interfaces");
                    String actions = rs.getString("actions");
                    String tables = rs.getString("tables");
                    int ruleWeight = rs.getInt("rule_weight");
                    boolean allow = rs.getBoolean("allow");

                    for (String ip : ips.split(",")) {
                        for (String inter : interfaces.split(",")) {
                            for (String action : actions.split(",")) {
                                if (tables != null) {
                                    for (String table : tables.split(",")) {
                                        Rule rule = new Rule();
                                        rule.setIp(ip);
                                        rule.setInter(inter);
                                        rule.setTable(table);
                                        rule.setAction(action);
                                        rule.setRuleWeight(ruleWeight);
                                        rule.setAllow(allow);
                                        ruleList.add(rule);
                                    }
                                } else {
                                    Rule rule = new Rule();
                                    rule.setIp(ip);
                                    rule.setInter(inter);
                                    rule.setTable(tables);
                                    rule.setAction(action);
                                    rule.setRuleWeight(ruleWeight);
                                    rule.setAllow(allow);
                                    ruleList.add(rule);
                                }
                            }
                        }
                    }
                }
                rs.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static boolean isWhiteList(APIRequest request) {
        boolean allowToAccess = true;
        if (request != null && request.getParameters() != null) {
            String tablenames = request.getParameters().getProperty("_tablename");
            if (tablenames != null) {
                for (String tablename : tablenames.split(",")) {
                    tablename = tablename.trim().split(" ")[0];
                    if (!isAllow(request.getIp(), request.getResoure(), request.getMethod(), tablename)) {
                        allowToAccess = false;
                        logger.debug("[" + request.getPort() + "]isWhiteList:" + allowToAccess + "[" + request.getIp() + "," + request.getResoure() + "," + request.getMethod() + "," + tablenames + "]", request.getTraceid());
                        break;
                    }
                }
            } else {
                allowToAccess = isAllow(request.getIp(), request.getResoure(), request.getMethod(), tablenames);
                if (!allowToAccess) {
                    logger.debug("[" + request.getPort() + "]isWhiteList:" + allowToAccess + "[" + request.getIp() + "," + request.getResoure() + "," + request.getMethod() + "," + tablenames + "]", request.getTraceid());
                }
            }
        }
        return allowToAccess;
    }

    public static boolean isAllow(String ip, String inter, String action, String table) {
        boolean allowToAccess = false;
        try {
            for (int i = 0; i < ruleList.size(); i++) {
                if (ruleList.get(i).getIp().compareToIgnoreCase(ip) == 0 || ruleList.get(i).getIp().compareToIgnoreCase("any") == 0) {
                    if (ruleList.get(i).getInter().compareToIgnoreCase(inter) == 0 || ruleList.get(i).getInter().compareToIgnoreCase("any") == 0) {
                        if (inter.compareToIgnoreCase("setting") == 0) {
                            allowToAccess = ruleList.get(i).isAllow();
                        } else {
                            if (ruleList.get(i).getAction().compareToIgnoreCase("any") == 0 || ruleList.get(i).getAction().compareToIgnoreCase(action) == 0) {
                                if (ruleList.get(i).getTable() == null) {
                                    allowToAccess = ruleList.get(i).isAllow();
                                    break;
                                } else {
                                    if (ruleList.get(i).getTable().compareToIgnoreCase("any") == 0) {
                                        allowToAccess = ruleList.get(i).isAllow();
                                        break;
                                    } else {
                                        if (table != null) {
                                            if (ruleList.get(i).getTable().compareToIgnoreCase(table) == 0) {
                                                allowToAccess = ruleList.get(i).isAllow();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("鉴权时发生错误，ip:["+ip+"],inter:["+inter+"]action:["+action+"],table:["+table+"]："+e.getMessage());
        }
        return allowToAccess;
    }
}


class Rule {
    private String ip;
    private String inter;
    private String table;
    private String action;
    private int ruleWeight;
    private boolean allow;

    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getInter() {
        return inter;
    }

    public void setInter(String inter) {
        this.inter = inter;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getRuleWeight() {
        return ruleWeight;
    }

    public void setRuleWeight(int ruleWeight) {
        this.ruleWeight = ruleWeight;
    }
}
