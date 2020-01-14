package com.ucsmy.itil.bg.common;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 2016/12/28.
 */
public class TableInfo {
    private String name;
    private String cnname;
    private String key;
    private List<Column> columns;

    public TableInfo() {
    }

    public TableInfo(String name, Connection conn) {
        this.name = name.trim();
        this.cnname = "";
        String[] tables = name.split(",");

        this.columns = new ArrayList<Column>();
        int idx = 0;
        for (String table : tables) {
            String alias = "";
            table = table.trim();
            int aliasIdx = table.indexOf(" ");
            if (aliasIdx > 0) {
                alias = table.substring(aliasIdx + 1);
                table = table.substring(0, aliasIdx);
            }
            try {
                Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                //System.out.println("***************Schema:"+conn.getCatalog());
                ResultSet rs = st.executeQuery("select t.TABLE_COMMENT from information_schema.`TABLES` t where t.TABLE_SCHEMA='" + conn.getCatalog() + "' and t.TABLE_NAME='" + table + "'");
                if (rs.next()) {
                    this.cnname += rs.getString(1) + ",";
                    rs.close();
                }
                rs = st.executeQuery("select i.column_name,i.column_type,i.is_nullable,i.column_key,i.column_comment,v.* from information_schema.`COLUMNS` i " +
                        "LEFT JOIN tablevision v on i.column_name=v.columnname and i.table_name=v.tablename " +
                        "where i.table_schema='" + conn.getCatalog() + "' and i.table_name='" + table + "' order by v.ui_sort_order");
                while (rs.next()) {
                    Column column = new Column();
                    column.setName(rs.getString(1));
                    String type = (rs.getString(2));
                    int l = type.indexOf("(");
                    int r = type.indexOf(")");
                    if (l > 0 & r > 0) {
                        column.setType(type.substring(0, l));
                        try {
                            column.setSize(Integer.parseInt(type.substring(l + 1, r).split(",")[0]));
                        } catch (Exception e) {
                            column.setSize(0);
                        }
                    } else {
                        column.setType(type);
                        column.setSize(0);
                    }

                    column.setMaynull(rs.getString("is_nullable").compareTo("YES") == 0);
                    column.setKey(rs.getString("column_key").compareTo("PRI") == 0);
                    if (column.isKey()) setKey(column.getName());
                    column.setComment(rs.getString("column_comment"));
                    column.setAlias(alias);
                    column.setAutofill(rs.getInt("autofill") == 1);
                    column.setShowname(rs.getString("showname"));
                    column.setDescribes(rs.getString("describes"));
                    column.setDefaultvalue(rs.getString("defaultvalue"));
                    column.setAutoformula(rs.getString("autoformula"));
                    column.setDictionaryname(rs.getString("dictionaryname"));
                    column.setFormat(rs.getString("format"));
                    column.setFormcontrol(rs.getString("formcontrol"));
                    column.setIn_use(rs.getInt("in_use"));
                    column.setUi_sort_order(rs.getInt("ui_sort_order"));
                    column.setUi_sort_length(rs.getString("ui_sort_length"));
                    columns.add(column);
                }

                rs.close();
                //conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        if (this.cnname.length() > 0) {
            this.cnname = this.cnname.substring(0, this.cnname.length() - 1);
        } else {
            this.cnname = this.name;
        }
    }

    public String getColumnType(String colname) {
        Column findColumn = getColumnByName(colname);
        String retType = null;
        if (findColumn != null) retType = findColumn.getType();
        return retType;
    }

    public Column getColumnByName(String colname) {
        Column retColumn = null;
        for (Column c : columns) {
            String columnName = c.getName();
            if (c.getAlias().length() > 0) {
                columnName = c.getAliasName();
            }
            if (columnName.compareToIgnoreCase(colname) == 0) {
                retColumn = c;
                break;
            }
        }
        return retColumn;
    }

    public String getCnname() {
        return cnname;
    }

    public void setCnname(String cnname) {
        this.cnname = cnname;
    }

    public String getColumnList() {
        String retString = "";

        for (Column c : columns) {
            String columnName = c.getName();
            if (c.getAlias().length() > 0) {
                retString += c.getAliasName() + ",";
            } else {
                retString += Util.keywordFilter(columnName) + ",";
            }
        }
        if (retString.length() > 0)
        retString = retString.substring(0, retString.length() - 1);

        return retString;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public static void main(String[] args) {
        Connection connection = ItilDataSource.newInstance().getConn();
        TableInfo ti = new TableInfo("glob_obj", connection);
        System.out.println(ti);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.WriteMapNullValue);
    }
}

