package com.ucsmy.itil.bg.common;

import com.ucsmy.itil.bg.model.CommonDAO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by Max on 2017/6/7.
 */
public class Dict {
    private static ArrayList<Section> itilDict = null;
    private static Properties index = null;

    static {
        itilDict = new ArrayList<Section>();
        index = new Properties();
        Connection conn = ItilDataSource.newInstance().getConn();
        ResultSet rs = new CommonDAO("dictionary", conn).selectBySql("select kind,detial as detail,GROUP_CONCAT(code) as valuelist,GROUP_CONCAT(`value`) as keylist from dictionary where code is not null and `value` is not null GROUP BY kind;");
        int idx = 0;
        try {
            while (rs.next()) {
                itilDict.add(new Section(rs.getString("kind"), rs.getString("detail"), rs.getString("keylist"), rs.getString("valuelist")));
                index.setProperty(rs.getString("kind"), String.valueOf(idx++));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Util.safeClose(conn);
        }
    }

    public static String g(String kind, String key) {
        Section tmp = itilDict.get(Integer.valueOf(index.getProperty(kind)));
        return tmp.getValueByKey(key);
    }
}

class Section {
    private String kind;
    private String name;
    private Properties keyValue;

    public Section(String kind, String name, String keystr, String valuestr) {
        this.kind = kind;
        this.name = name;
        String[] keys = keystr.split(",");
        String[] values = valuestr.split(",");
        this.keyValue = new Properties();
        for (int i = 0; i < keys.length; i++) {
            this.keyValue.setProperty(keys[i], values[i]);
        }
    }

    public String getValueByKey(String key) {
        return this.keyValue.getProperty(key);
    }
}
