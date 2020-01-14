package com.ucsmy.itil.bg.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by Max on 2017/1/6.
 */
public class CmdbConn {
    private static Properties jdbcProperty = null;
    private static HikariDataSource ds = null;
    private static final int connectiontimeout = 10 * 1000;//等待连接池分配连接的最大时长（毫秒），超过这个时长还没可用的连接则发生SQLException， 缺省:30秒
    private static final int idletimeout = 10 * 60 * 1000;//一个连接idle状态的最大时长（毫秒），超时则被释放（retired），缺省:10分钟
    private static final int maxlifetime = 30 * 60 * 1000;//一个连接的生命时长（毫秒），超时而且没被使用则被释放（retired），缺省:30分钟，建议设置比数据库超时时长少30秒，参考MySQL wait_timeout参数（show variables like '%timeout%';）
    private static final int logintimeout = 30 * 60;        //登录超时，默认30分钟，单位秒
    private static final int minimum = 10;
    private static final int maximum = 15;//连接池中允许的最大连接数。缺省值：10；推荐的公式：((core_count * 2) + effective_spindle_count)

    static {
        try {
            jdbcProperty = new Properties();
            InputStream in = Object.class.getResourceAsStream("/mybatis/cmdb.properties");
            jdbcProperty.load(in);
            in.close();

            initConnectionPool();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initConnectionPool() {
        //连接池配置
        if (ds != null) ds.close();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(jdbcProperty.getProperty("mysql.driverClassName"));
        config.setJdbcUrl(jdbcProperty.getProperty("mysql.url"));
        config.setUsername(jdbcProperty.getProperty("mysql.username"));
        config.setPassword(jdbcProperty.getProperty("mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 500);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.setConnectionTimeout(connectiontimeout);
        config.setIdleTimeout(idletimeout);
        config.setMaxLifetime(maxlifetime);
        config.setMinimumIdle(minimum);
        config.setMaximumPoolSize(maximum);
        //config.setConnectionTestQuery("SELECT 1");
        //config.setValidationTimeout(idletimeout);
        //config.setJdbc4ConnectionTest(true);
        config.setAutoCommit(true);
        config.setPoolName("cmdb_161");
        ds = new HikariDataSource(config);
        try {
            ds.setLoginTimeout(logintimeout);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    /**
     * 销毁连接池
     */
    public static void close() {
        ds.close();
    }

    public static Connection getConn() {
        try {
            if (ds.isClosed()) {
                System.out.println("--------------------DataSource is closed ! ------------------------");
                initConnectionPool();
            }
            return ds.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            //initConnectionPool();
            return null;
        }
    }


}
