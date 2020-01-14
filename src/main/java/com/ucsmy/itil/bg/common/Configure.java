package com.ucsmy.itil.bg.common;

import java.io.*;
import java.util.Properties;

/**
 * Created by Max on 2017/6/6.
 */
public class Configure {
    private static Properties config = null;
    private static String configPath=null;

    static {
        config = new Properties();
        if(configPath==null) {
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) { //linux，非开发环境
                if(System.getenv("ITSM_CONFIG_PATH")!=null){
                    configPath=System.getenv("ITSM_CONFIG_PATH");
                }else {
                    configPath = "/usr/local/itsm/conf/application.properties";
                    //configPath = "/opt/jboss/itsm/conf/application.properties";
                }
            } else {
                configPath = "D:\\dev\\bg_service\\src\\main\\resources\\application.properties";
            }
        }
        init();
    }

    private static void init(){
        try {
            //InputStream in = Object.class.getResourceAsStream("/application.properties");
            InputStream in = new BufferedInputStream(new FileInputStream(new File(configPath)));
            config.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getConfigPath() {
        return configPath;
    }

    public static void setConfigPath(String configPath) {
        Configure.configPath = configPath;
        init();
    }

    public static Properties getConfig() {
        return config;
    }
}
