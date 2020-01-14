package com.ucsmy.itil.bg.execute;

import com.ucsmy.itil.bg.common.DbConn;
import com.ucsmy.itil.bg.common.ItilDataSource;
import com.ucsmy.itil.bg.common.Util;
import com.ucsmy.itil.bg.model.CommonDAO;
import com.ucsmy.itil.bg.service.CommonObject;
import org.apache.commons.codec.digest.Md5Crypt;

import java.sql.Connection;
import java.util.Date;

/**
 * Created by Max on 2017/1/17.
 */
public class MixcodeTest {
    private static int testDepth = 7;
    private Connection connection = null;
    private CommonDAO commonDAO = null;

    public MixcodeTest() {
        this.connection = ItilDataSource.newInstance().getConn();
        commonDAO = new CommonDAO("mixcode", connection);
        commonDAO.executeBySql("delete from mixcode");
    }

    public void testCode(String sourceCode) {
        String testCode;
        String mixCode;

        for (int c = 33; c < 127; c++) {
            char ch = (char) c;
            testCode = sourceCode + String.valueOf(ch);
            mixCode = Util.sqlPrepare(Util.mixCode(testCode.getBytes()));
            //mixCode = Md5Crypt.md5Crypt(testCode.getBytes());
            testCode = Util.sqlPrepare(testCode);
            //System.out.println("TestCode:"+testCode);

            if (commonDAO.executeBySql("insert into mixcode(mixcode,source)  values('" + mixCode + "','" + testCode + "')") == 0) {
                System.out.println("-----ERROR------          testcode=" + testCode + ",mixcode=" + mixCode);
            }
            if (testCode.length() < testDepth) testCode(testCode);
        }
    }

    public static void main(String[] args) {
        Date begin = new Date();
        new MixcodeTest().testCode("");
        Date end = new Date();
        long pass = end.getTime() - begin.getTime();
        System.out.println("Pass:" + pass + "ms,Begin:" + begin + ",End:" + end);
    }
}
