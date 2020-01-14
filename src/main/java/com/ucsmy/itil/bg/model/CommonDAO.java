package com.ucsmy.itil.bg.model;

import com.ucsmy.itil.bg.common.*;
import com.ucsmy.itil.bg.service.SystemLog;
import com.ucsmy.itil.bg.service.UcsmyFunction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 2016/12/29.
 */
public class CommonDAO {
    //private static Logger logger = LogManager.getLogger(CommonDAO.class.getName());
    private UcsmyLog logger = new UcsmyLog(CommonDAO.class.getName());
    private TableInfo tableInfo;
    private Connection connection;
    private String operator;
    private boolean sqlLog = true;
    private boolean write2usp = true;
    private int port;
    private String traceid;

    public CommonDAO(String tableName, Connection connection) {
        this.tableInfo = new TableInfo(tableName, connection);
        this.connection = connection;
        this.operator = "System";
        setPort(Util.random());
    }

    public CommonDAO(String tableName, Connection connection, String operatorName) {
        this.tableInfo = new TableInfo(tableName, connection);
        this.connection = connection;
        this.operator = operatorName;
        setPort(Util.random());
    }

    public String getTraceid() {
        return traceid;
    }

    public void setTraceid(String traceid) {
        this.traceid = traceid;
        logger.setTrace(traceid);
    }

    public void setSqlLog(boolean sqlLog) {
        this.sqlLog = sqlLog;
    }

    public boolean isWrite2usp() {
        return write2usp;
    }

    public void setWrite2usp(boolean write2usp) {
        this.write2usp = write2usp;
        logger.setWrite2usp(write2usp);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            this.connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    public void commit() {
        try {
            this.connection.commit();
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public int countByExample(CommonExample commonExample) {
        int retValue = 0;
        int tempPageSize = commonExample.getPageSize();
        commonExample.setPageSize(0);
        ResultSet rs;
        if (commonExample.getGroupByClause() != null && commonExample.getGroupByClause().trim().length() > 0) {
            rs = selectBySql("select count(1) from (" + getSelectByExample(commonExample.getGroupByClause(), commonExample) + ") t");
        } else {
            rs = selectByExample("count(1)", commonExample);
        }
        commonExample.setPageSize(tempPageSize);
        try {
            if (rs != null) {
                if (rs.next()) {
                    retValue = rs.getInt(1);
                    rs.close();
                }
            } else {
                retValue = -1;
            }
        } catch (Exception e) {
            logger.error("[" + this.port + "]countByExample ERROR:" + e.getMessage());
        }

        return retValue;
    }

    public ResultSet selectByKey(Object keyValue) {
        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo(tableInfo.getKey(), keyValue);
        return selectByExample(commonExample);
    }

    public ResultSet selectByExample(CommonExample commonExample) {
        return selectByExample(null, commonExample);
    }

    public ResultSet selectByExample(String columnList, CommonExample commonExample) {
        ResultSet rs = null;
        String sqlStr = "select ";
        try {
            if (columnList == null) {
                columnList = tableInfo.getColumnList();
            }
            if (columnList.length() > 0) {
                //Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                if (commonExample.isDistinct()) {
                    sqlStr += "distinct ";
                }
                sqlStr += Util.mysqlAntiInject(columnList) + " from ";
                String[] tables = tableInfo.getName().split(",");
                sqlStr += tables[0];
                if (tables.length > 1 && commonExample.getJoinCols() != null) {
                    String[] joins = commonExample.getJoinCols().split(",");
                    if (tables.length == joins.length + 1) {
                        for (int j = 1; j < tables.length; j++) {
                            String[] cols = joins[j - 1].trim().split(" ");
                            if (cols.length > 1) {
                                if (cols[0].trim().endsWith("(-)")) {
                                    cols[0] = cols[0].replaceAll("\\(-\\)", "");
                                    sqlStr += " left join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                } else if (cols[1].trim().endsWith("(-)")) {
                                    cols[1] = cols[1].replaceAll("\\(-\\)", "");
                                    sqlStr += " right join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                } else {
                                    sqlStr += " join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                }
                            }
                        }
                    }
                }
                sqlStr += commonExample.generateCriteriaSql();
                logger.debug("[" + this.port + "]selectByExample SQL=[" + sqlStr + "] values:" + commonExample.values.toString());

                PreparedStatement ps = commonExample.loadValue(connection.prepareStatement(sqlStr));

                rs = ps.executeQuery();
            }
        } catch (Exception se) {
            if (se.getMessage().contains("regexp")) {
                logger.debug("[" + this.port + "]selectByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "] values:" + commonExample.values.toString());
            } else {
                logger.error("[" + this.port + "]selectByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "] values:" + commonExample.values.toString());
            }
            //se.printStackTrace();
        } finally {
            commonExample.values.clear();
        }

        return rs;
    }

    public String getSelectByExample(String columnList, CommonExample commonExample) {
        String sqlStr = "select ";
        try {
            if (columnList == null) {
                columnList = tableInfo.getColumnList();
            }
            if (columnList.length() > 0) {
                //Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                if (commonExample.isDistinct()) {
                    sqlStr += "distinct ";
                }
                sqlStr += Util.mysqlAntiInject(columnList) + " from ";
                String[] tables = tableInfo.getName().split(",");
                sqlStr += tables[0];
                if (tables.length > 1 && commonExample.getJoinCols() != null) {
                    String[] joins = commonExample.getJoinCols().split(",");
                    if (tables.length == joins.length + 1) {
                        for (int j = 1; j < tables.length; j++) {
                            String[] cols = joins[j - 1].trim().split(" ");
                            if (cols.length > 1) {
                                if (cols[0].trim().endsWith("(-)")) {
                                    cols[0] = cols[0].replaceAll("\\(-\\)", "");
                                    sqlStr += " left join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                } else if (cols[1].trim().endsWith("(-)")) {
                                    cols[1] = cols[1].replaceAll("\\(-\\)", "");
                                    sqlStr += " right join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                } else {
                                    sqlStr += " join " + tables[j].trim() + " on " + cols[0] + "=" + cols[1];
                                }
                            }
                        }
                    }
                }
                sqlStr += commonExample.generateCriteriaSqlWithValue();
                logger.debug("[" + this.port + "]getSelectByExample SQL=[" + sqlStr + "]");
            }
        } catch (Exception se) {
            if (se.getMessage().contains("regexp")) {
                logger.debug("[" + this.port + "]getSelectByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "]");
            } else {
                logger.error("[" + this.port + "]getSelectByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "]");
            }
            //se.printStackTrace();
        } finally {
            commonExample.values.clear();
        }

        return sqlStr;
    }

    public ResultSet selectBySql(String sql) {
        ResultSet rs = null;
        try {
            if (sql != null) {
                Statement ps = connection.createStatement();
                logger.debug("[" + this.port + "]SQL:" + sql);

                rs = ps.executeQuery(sql);
            }
        } catch (SQLException se) {
            logger.error("[" + this.port + "]selectBySql ERROR:" + se.getMessage() + " with SQL:[" + sql + "]");
        }

        return rs;
    }

    public int executeBySql(String sql) {
        return executeBySql(sql, this.sqlLog);
    }

    public int executeBySql(String sql, boolean sql_log) {
        int retValue = 0;
        try {
            if (sql != null) {
                Statement ps = connection.createStatement();
                logger.debug("[" + this.port + "]SQL:" + sql);
                if (sql_log) {
                    log4sql("direct", sql, null);
                }
                retValue = ps.executeUpdate(sql);
            }
        } catch (SQLException se) {
            logger.error("[" + this.port + "]executeBySql ERROR:" + se.getMessage() + " with SQL:[" + sql + "]");
            if (sql_log) {
                log4sql("direct", sql,null,se.getMessage());
            }
        }

        return retValue;
    }

    public int insertByExample(CommonExample commonExample) {
        return insertByExample(commonExample, this.sqlLog);
    }

    public int insertByExample(CommonExample commonExample, boolean sql_log) {  //如果有自增量字段，则返回自增量值
        int retValue = 0;
        String sqlStr = "insert into " + tableInfo.getName();
        try {
            sqlStr += commonExample.generateInsertSql();
            logger.debug("[" + this.port + "]insertByExample SQL[" + sqlStr + "] values:" + commonExample.values.toString());

            PreparedStatement preparedStatement = commonExample.loadValue(connection.prepareStatement(sqlStr, Statement.RETURN_GENERATED_KEYS));
            retValue = preparedStatement.executeUpdate();
            if (retValue > 0) {
                ResultSet rs = preparedStatement.getGeneratedKeys();
                if (rs != null) {
                    if (rs.next()) {
                        retValue = rs.getInt(1);
                    }
                    rs.close();
                }
            }

            if (sql_log) {
                log4sql("insert", sqlStr, commonExample.values.toString());
            }
            commonExample.values.clear();
        } catch (SQLException se) {
            logger.error("[" + this.port + "]insertByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "] values:" + commonExample.values.toString());
            if (sql_log) {
                log4sql("insert", sqlStr, commonExample.values.toString(), se.getMessage());
            }
        }

        return retValue;
    }

    public int updateByExample(CommonExample commonExample) {
        return updateByExample(commonExample, this.sqlLog);
    }

    public int updateByExample(CommonExample commonExample, boolean sql_log) {
        int retValue = 0;
        String sqlStr = "update " + tableInfo.getName() + " set " + commonExample.generateUpdateSql() + commonExample.generateCriteriaSql();
        try {
            logger.debug("[" + this.port + "]updateByExample SQL[" + sqlStr + "] values:" + commonExample.values.toString());

            PreparedStatement preparedStatement = commonExample.loadValue(connection.prepareStatement(sqlStr));
            retValue = preparedStatement.executeUpdate();

            if (sql_log) {
                log4sql("update", sqlStr, commonExample.values.toString());
            }
            commonExample.values.clear();
        } catch (SQLException se) {
            logger.error("[" + this.port + "]updateByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "] values:" + commonExample.values.toString());
            if (sql_log) {
                log4sql("update", sqlStr, commonExample.values.toString(), se.getMessage());
            }
        }

        return retValue;
    }

    public int deleteByKey(Object keyValue) {
        CommonExample commonExample = new CommonExample();
        CommonExample.Criteria criteria = commonExample.createCriteria();
        criteria.andColumnEqualTo(tableInfo.getKey(), keyValue);
        return deleteByExample(commonExample);
    }

    public int deleteByExample(CommonExample commonExample) {
        return deleteByExample(commonExample, this.sqlLog);
    }

    public int deleteByExample(CommonExample commonExample, boolean sql_log) {
        int retValue = 0;
        String sqlStr = "delete from " + tableInfo.getName();
        try {
            sqlStr += commonExample.generateCriteriaSql();
            logger.debug("[" + this.port + "]deleteByExample SQL[" + sqlStr + "] values:" + commonExample.values.toString());

            PreparedStatement preparedStatement = commonExample.loadValue(connection.prepareStatement(sqlStr));
            retValue = preparedStatement.executeUpdate();

            if (sql_log) {
                log4sql("delete", sqlStr, commonExample.values.toString());
            }
            commonExample.values.clear();
        } catch (SQLException se) {
            logger.error("[" + this.port + "]deleteByExample ERROR:" + se.getMessage() + " with SQL:[" + sqlStr + "] values:" + commonExample.values.toString());
            if (sql_log) {
                log4sql("delete", sqlStr, commonExample.values.toString(), se.getMessage());
            }
        }

        return retValue;
    }

    private void log4sql(String action, String sql, String values) {
        log4sql(action, sql, values, "");
    }

    private void log4sql(String action, String sql, String values, String errorMsg) {
        String account = this.operator == null ? "Guest" : this.operator;
        SystemLog.log(account, action, this.tableInfo.getName(), sql, values, errorMsg);
    }

    public String resultSet2Json(ResultSet resultSet, TableInfo tableInfo, int total, CommonExample commonExample) {
        return resultSet2Json(resultSet, null, tableInfo, total, commonExample);
    }

    public String resultSet2Json(ResultSet resultSet, String selectCol, TableInfo tableInfo, int total, CommonExample commonExample) {
        return resultSet2Json(resultSet, selectCol, tableInfo, total, commonExample,false);
    }

    public String resultSet2Json(ResultSet resultSet, String selectCol, TableInfo tableInfo, int total, CommonExample commonExample,boolean needTableInfo) {
        String retString = "";
        if (resultSet != null) {
            try {
                if (!resultSet.isClosed()) {
                    List<Column> columnList = new ArrayList<Column>();
                    for (Column column : tableInfo.getColumns()) {
                        try {
                            if (resultSet.findColumn(column.getAliasName()) > 0) {
                                columnList.add(column);
                            }
                        } catch (SQLException sqle) {
                            //logger.info("Column " + column.getAliasName() + " is not find !");
                        }
                    }
                    TableInfo tableInfo1 = new TableInfo();
                    tableInfo1.setColumns(columnList);
                    tableInfo1.setName(tableInfo.getName());
                    tableInfo1.setKey(tableInfo.getKey());

                    if (selectCol != null) {
                        //处理指定字段查询
                        String[] selectColumns = selectCol.split(",");
                        for (int c = 0; c < selectColumns.length; c++) {
                            boolean findColumn = false;
                            selectColumns[c] = selectColumns[c].trim();
                            for (Column column : columnList) {
                                if (column.getAliasName().equalsIgnoreCase(selectColumns[c])) {
                                    findColumn = true;
                                    break;
                                }
                            }
                            if (!findColumn) {
                                Column newColumn = new Column();

                                newColumn.setName(selectColumns[c]);
                                newColumn.setAlias("");
                                newColumn.setType("unkonw");
                                newColumn.setSize(0);
                                newColumn.setComment(selectColumns[c]);
                                newColumn.setShowname(selectColumns[c]);
                                columnList.add(newColumn);
                            }
                        }
                    }

                    while (resultSet.next()) {
                        String colJson = "";
                        for (Column column : columnList) {
                            String value = null;
                            value = resultSet.getString(column.getAliasName());
                            column.setTempValue(value);
                        }

                        for (Column column : columnList) {
                            //发现自动化公式，符合函数规则，则调用函数方法并将返回值赋予此字段
                            if (column.getAutoformula() != null && column.getTempValue() == null) {
                                if (column.getAutoformula().startsWith("func:")) {
                                    column.setTempValue(UcsmyFunction.Func(column.getAutoformula(), tableInfo1));
                                }
                            }

                            if (colJson.length() > 0) {
                                colJson += ",";
                            }

                            String quot = "";
                            switch (column.getType()) {
                                case "int":
                                case "tinyint":
                                case "long":
                                    break;
                                default:
                                    quot = "\"";
                            }

                            if (column.getTempValue() != null) {
                                column.setTempValue(column.getTempValue().replace("\\", "\\\\").replace("\"", "\\\""));
                            }
                            if (column.getTempValue() == null) {
                                quot = "";
                            }

                            colJson += "\"" + column.getAliasName() + "\":" + quot + column.getTempValue() + quot;
                        }
                        if (retString.length() > 0) {
                            retString += "},{";
                        }
                        retString += colJson;
                    }

                    ResultSetParameter resultSetParameter = new ResultSetParameter();
                    if(needTableInfo) {
                        resultSetParameter.setTableInfo(tableInfo1);
                    }
                    resultSetParameter.setTotal(total);
                    resultSetParameter.setCommonExample(commonExample);

                    if (retString.length() > 0) {
                        retString = "{" + retString + "}";
                    }
                    retString = "{\"data\":[" + retString + "],\"resultsetparameter\":" + resultSetParameter.toString() + "}";
                    //retString = retString.replaceAll("[\\t\\n\\r]", "");
                    retString = retString.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                }
            } catch (SQLException se) {
                logger.error("[" + this.port + "]resultSet2Json ERROR:" + se.getMessage());
            }
        }

        return retString;
    }

    //test
    public static void main(String[] args) {

    }
}
