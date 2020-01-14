package com.ucsmy.itil.bg.common;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;

public class DbConn {

    private static SqlSessionFactory sqlSessionFactory = null;

    static {
        try {
            InputStream is = Resources.getResourceAsStream("mybatis/mybatisConfig.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

}

