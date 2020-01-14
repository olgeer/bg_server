package com.ucsmy.itil.bg.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.model.CommonExample;
import com.ucsmy.itil.bg.service.UcsmyFunction;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
//import java.time.*;
import java.util.*;

/**
 * Created by Max on 2016/12/8.
 */
public class Util {
    public static int WORKHOURS = 9;
    public static long WORKSECOND = WORKHOURS * 3600;

    private static UcsmyLog logger = new UcsmyLog(Util.class.getName());

    public static int str2int(String number) {
        return str2int(number, 0);
    }

    public static int str2int(String number, int defaultValue) {
        int retInt = defaultValue;
        try {
            retInt = Integer.parseInt(number);
        } catch (NumberFormatException e) {
        }
        return retInt;
    }

    public static String keywordFilter(String column) {
        String keyword = "|ADD|ALL|ALTER|ANALYZE|AND|AS|ASC|ASENSITIVE|BEFORE" +
                "|BETWEEN|BIGINT|BINARY|BLOB|BOTH|BY|CALL|CASCADE|CASE" +
                "|CHANGE|CHAR|CHARACTER|CHECK|COLLATE|COLUMN|CONDITION" +
                "|CONNECTION|CONSTRAINT|CONTINUE|CONVERT|CREATE|CROSS" +
                "|CURRENT_DATE|CURRENT_TIME|CURRENT_TIMESTAMP|CURRENT_USER" +
                "|CURSOR|DATABASE|DATABASES|DAY_HOUR|DAY_MICROSECOND" +
                "|DAY_MINUTE|DAY_SECOND|DEC|DECIMAL|DECLARE|DEFAULT" +
                "|DELAYED|DELETE|DESC|DESCRIBE|DETERMINISTIC|DISTINCT" +
                "|DISTINCTROW|DIV|DOUBLE|DROP|DUAL|EACH|ELSE|ELSEIF" +
                "|ENCLOSED|ESCAPED|EXISTS|EXIT|EXPLAIN|FALSE|FETCH|FLOAT" +
                "|FLOAT4|FLOAT8|FOR|FORCE|FOREIGN|FROM|FULLTEXT|GOTO|GRANT" +
                "|GROUP|HAVING|HIGH_PRIORITY|HOUR_MICROSECOND|HOUR_MINUTE" +
                "|HOUR_SECOND|IF|IGNORE|IN|INDEX|INFILE|INNER|INOUT" +
                "|INSENSITIVE|INSERT|INT|INT1|INT2|INT3|INT4|INT8|INTEGER" +
                "|INTERVAL|INTO|IS|ITERATE|JOIN|KEY|KEYS|KILL|LABEL|LEADING" +
                "|LEAVE|LEFT|LIKE|LIMIT|LINEAR|LINES|LOAD|LOCALTIME" +
                "|LOCALTIMESTAMP|LOCK|LONG|LONGBLOB|LONGTEXT|LOOP|LOW_PRIORITY" +
                "|MATCH|MEDIUMBLOB|MEDIUMINT|MEDIUMTEXT|MIDDLEINT|MINUTE_MICROSECOND" +
                "|MINUTE_SECOND|MOD|MODIFIES|NATURAL|NOT|NO_WRITE_TO_BINLOG|NULL" +
                "|NUMERIC|ON|OPTIMIZE|OPTION|OPTIONALLY|OR|ORDER|OUT|OUTER|OUTFILE" +
                "|PRECISION|PRIMARY|PROCEDURE|PURGE|RAID0|RANGE|READ|READS|REAL" +
                "|REFERENCES|REGEXP|RELEASE|RENAME|REPEAT|REPLACE|REQUIRE|RESTRICT" +
                "|RETURN|REVOKE|RIGHT|RLIKE|SCHEMA|SCHEMAS|SECOND_MICROSECOND|SELECT" +
                "|SENSITIVE|SEPARATOR|SET|SHOW|SMALLINT|SPATIAL|SPECIFIC|SQL" +
                "|SQLEXCEPTION|SQLSTATE|SQLWARNING|SQL_BIG_RESULT|SQL_CALC_FOUND_ROWS" +
                "|SQL_SMALL_RESULT|SSL|STARTING|STRAIGHT_JOIN|TABLE|TERMINATED|THEN" +
                "|TINYBLOB|TINYINT|TINYTEXT|TO|TRAILING|TRIGGER|TRUE|UNDO|UNION|UNIQUE" +
                "|UNLOCK|UNSIGNED|UPDATE|USAGE|USE|USING|UTC_DATE|UTC_TIME|UTC_TIMESTAMP" +
                "|VALUES|VARBINARY|VARCHAR|VARCHARACTER|VARYING|WHEN|WHERE|WHILE|WITH" +
                "|WRITE|X509|XOR|YEAR_MONTH|ZEROFILL|";
        String retString = column;
        if (keyword.indexOf("|" + retString.toUpperCase() + "|") != -1) {
            retString = "`" + retString + "`";
        }
        return retString;
    }

    /**
     * 根据属性名获取属性值
     */
    private static Object getFieldValueByName(String fieldName, Object o) {
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = o.getClass().getMethod(getter, new Class[]{});
            Object value = method.invoke(o, new Object[]{});
            return value;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * 获取属性名数组
     */
    private static String[] getFiledName(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            System.out.println(fields[i].getType());
            fieldNames[i] = fields[i].getName();
        }
        return fieldNames;
    }

    /**
     * 获取属性类型(type)，属性名(name)，属性值(value)的map组成的list
     */
    public static List getFiledsInfo(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        String[] fieldNames = new String[fields.length];
        List list = new ArrayList();
        Map infoMap = null;
        for (int i = 0; i < fields.length; i++) {
            infoMap = new HashMap();
            infoMap.put("type", fields[i].getType().toString());
            infoMap.put("name", fields[i].getName());
            infoMap.put("value", getFieldValueByName(fields[i].getName(), o));
            list.add(infoMap);
        }
        return list;
    }

    /**
     * 获取对象的所有属性值，返回一个对象数组
     */
    public static Object[] getFiledValues(Object o) {
        String[] fieldNames = Util.getFiledName(o);
        Object[] value = new Object[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            value[i] = Util.getFieldValueByName(fieldNames[i], o);
        }
        return value;
    }

    public static String today() {
        return Util.formatDate(new java.util.Date());
    }

    public static String now() {
        return Util.formatDateTime(new java.util.Date());
    }

    public static String toJson(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        String jsonStr = "{";
        Object val;

        for (int i = 0; i < fields.length; i++) {
            jsonStr += "\"" + fields[i].getName() + "\":";
            val = getFieldValueByName(fields[i].getName(), o);
            if (val instanceof String) {
                jsonStr += "\"";
            }
            jsonStr += val;
            if (val instanceof String) {
                jsonStr += "\"";
            }
            if (i != fields.length - 1) {
                jsonStr += ",";
            }
        }
        jsonStr += "}";
        return jsonStr;
    }

    public static String encodeStr(String plainText) {
        byte[] b = plainText.getBytes();
        Base64 base64 = new Base64();
        b = base64.encode(b);
        String s = new String(b);

        return s;
    }

    public static String decodeStr(String encodeStr) {
        byte[] b = encodeStr.getBytes();
        Base64 base64 = new Base64();
        b = base64.decode(b);
        String s = new String(b);
        return s;
    }

    public static String formatDateTime(java.util.Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return f.format(date);
    }

    public static String formatDate(java.util.Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return f.format(date);
    }

    public static java.util.Date parseDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = null;
        try {
            date = sdf.parse(dateString);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return date;
    }

    public static java.util.Date parseDateTime(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date date = null;
        try {
            date = sdf.parse(dateString);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return date;
    }

    // 01. java.util.Date --> java.time.LocalDateTime
//    public static LocalDateTime UDateToLocalDateTime(java.util.Date date) {
//        Instant instant = date.toInstant();
//        ZoneId zone = ZoneId.systemDefault();
//        return LocalDateTime.ofInstant(instant, zone);
//    }

    // 02. java.util.Date --> java.time.LocalDate
//    public static LocalDate UDateToLocalDate(java.util.Date date) {
//        Instant instant = date.toInstant();
//        ZoneId zone = ZoneId.systemDefault();
//        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
//        return localDateTime.toLocalDate();
//    }

    // 03. java.util.Date --> java.time.LocalTime
//    public static LocalTime UDateToLocalTime(java.util.Date date) {
//        Instant instant = date.toInstant();
//        ZoneId zone = ZoneId.systemDefault();
//        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
//        return localDateTime.toLocalTime();
//    }

    // 04. java.time.LocalDateTime --> java.util.Date
//    public static java.util.Date LocalDateTimeToUdate(LocalDateTime localDateTime) {
//        ZoneId zone = ZoneId.systemDefault();
//        Instant instant = localDateTime.atZone(zone).toInstant();
//        return Date.from(instant);
//    }

    // 05. java.time.LocalDate --> java.util.Date
//    public static java.util.Date LocalDateToUdate(LocalDate localDate) {
//        ZoneId zone = ZoneId.systemDefault();
//        Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
//        return Date.from(instant);
//    }

    // 06. java.time.LocalTime --> java.util.Date
//    public static java.util.Date LocalTimeToUdate(LocalTime localTime, LocalDate localDate) {
//        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
//        ZoneId zone = ZoneId.systemDefault();
//        Instant instant = localDateTime.atZone(zone).toInstant();
//        return Date.from(instant);
//    }

    public static String getQuot(Object object) {
        String quot = "";
        if (object != null) {
            String type = object.getClass().getName();
            switch (type) {
                case "integer":
                case "long":
                case "double":
                    quot = "";
                    break;
                default:
                    quot = "\"";
            }
        }
        return quot;
    }


    public static boolean isDigitalType(String type) {
        boolean retValue = false;
        if (type != null) {
            switch (type.toLowerCase()) {
                case "int":
                case "tinyint":
                case "long":
                    retValue = true;
                    break;
                default:
                    retValue = false;
            }
        }
        return retValue;
    }

    public static String getParameter(String params, String paramName) {
        String retString = null;
        String[] maps = params.split("&");
        for (String temp : maps) {
            String[] map = temp.split("=");
            if (map[0].compareTo(paramName) == 0) {
                retString = map[1];
            }
        }
        return retString;
    }

    public static Properties toProperties(String params) {
        Properties retProperties = null;
        if (params != null) {
            retProperties = new Properties();
            String[] maps = params.split("&");
            String value = null;
            for (String temp : maps) {
                String[] map = temp.split("=");
                if (map.length == 1) {
                    retProperties.setProperty(map[0].trim(), "");
                } else {
                    if (!map[0].contains("sEcho") && !map[0].contains("iColumns") && !map[0].contains("iDisplay")
                            && !map[0].contains("sColumns") && !map[0].contains("mDataProp") && !map[0].contains("sSearch")
                            && !map[0].contains("iSortCol") && !map[0].contains("sSortDir") && !map[0].contains("iSortingCols")
                            && !map[0].contains("bRegex") && !map[0].contains("bSearchable") && !map[0].contains("bSortable")
                            && !map[0].contains("iTotalRows") && !map[0].contains("sAction") && map.length > 1) {
                        try {
                            value = temp.substring(temp.indexOf('=') + 1).trim();
                            //value = value.replace('+', '＋');
                            //value = value.replace(' ','·');
                            value = value.replaceAll(" ", "%20");
                            value = value.replaceAll("\\+", "%2B");
                            value = URLDecoder.decode(value, "utf-8");
                            //value = value.replace('·',' ');
                            //value = value.replace('＋', '+');
                            retProperties.setProperty(map[0], value);
                        } catch (UnsupportedEncodingException e) {
                            logger.debug("UnsupportedEncodingException at [URLDecoder.decode(" + value + "）]" + e.getMessage());
                        }
                    }
                }
            }
        }
        return retProperties;
    }

    public static Properties removeCommonProperties(Properties properties) {
        properties.remove("_tablename");
        properties.remove("_selectcol");
        properties.remove("_pagesize");
        properties.remove("_pageindex");
        properties.remove("_action");
        properties.remove("_token");
        properties.remove("_username");
        properties.remove("_callback");
        properties.remove("_jsvalue");
        properties.remove("_");
        properties.remove("_sortcol");
        properties.remove("_sortdir");
        properties.remove("_groupcol");
        properties.remove("_joincols");
        properties.remove("_start");
        properties.remove("_end");
        properties.remove("undefined");
        return properties;
    }

    public static String encrypt(String info) {
        return encrypt(info.getBytes());
    }

    public static String encrypt(byte[] info) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(info);
            byte[] byteDigest = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < byteDigest.length; offset++) {
                i = byteDigest[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            //32位加密
            return buf.toString();
            // 16位的加密
            //return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static void safeClose(Connection connection) {
        try {
            connection.close();
        } catch (SQLException se) {
            logger.error(se.getMessage());
        }
    }

    public static String creatUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String sqlPrepare(String sql) {
        sql = sql.replace("\\", "\\\\");
        sql = sql.replace("\"", "\\\"");
        sql = sql.replace("'", "\\\'");
        return sql;
    }

    public static String mixCode(byte[] source) {
        Random random = new Random(source.length * checkSum(source));
        byte[] randomArray = new byte[256];
        random.nextBytes(randomArray);

        byte[] tmp = source.clone();
        String retString = "";
        int l = tmp.length;
        int t = 0;
        for (int i = 0; i < l; i++) {
            t = (byte) tmp[i] + t;
            if (t < 0) {
                t = t + 256;
            }
            if (t > 255) {
                t = t % 256;
            }
            randomArray[t] = (byte) (t + randomArray[t]);
        }

        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                randomArray[x * 16 + 15] = (byte) (randomArray[x * 16 + y] + randomArray[x * 16 + 15]);
                randomArray[15 * 16 + y] = (byte) (randomArray[x * 16 + y] + randomArray[15 * 16 + y]);
            }
        }
        String xCode = "";
        String yCode = "";
        for (int i = 0; i < 16; i++) {
            xCode += toHexString(randomArray[i * 16 + 15]);
            yCode += toHexString(randomArray[15 * 16 + i]);
        }
        retString = xCode + yCode;

        return retString.toUpperCase();
    }

    public static int checkSum(byte[] source) {
        int retValue = 0;
        if (source != null) {
            byte[] tmp = source.clone();
            int l = tmp.length;

            for (int i = 0; i < l; i++) {
                //retString += Integer.toHexString(tmp[i] ^ tmp[tmp.length - i - 1]);
                tmp[0] = (byte) (tmp[0] + tmp[i]);
            }
            retValue = tmp[0];
        }
        return retValue;
        //return new String(tmp).substring(0,l/2);
    }

    public static String toHexString(String source) {
        byte[] tmp = source.getBytes();
        String retString = "";
        for (int i = 0; i < tmp.length; i++) {
            retString += toHexString(tmp[i]);
        }
        return retString.toUpperCase();
    }

    public static String toHexString(byte b) {
        String tmpStr = Integer.toHexString(b);
        if (tmpStr.length() > 2) {
            tmpStr = tmpStr.substring(tmpStr.length() - 2, tmpStr.length());
        }
        if (tmpStr.length() < 2) {
            tmpStr = "0" + tmpStr;
        }
        return tmpStr;
    }

    //File size less then 100MB, you can setting Xms to change this limit.
    public static byte[] readFile(String fileName) {
        File fileToEncode = new File(fileName);

        FileInputStream fis = null;
        byte[] data = null;
        try {
            fis = new FileInputStream(fileToEncode);
            logger.debug(fis.available() + " Bytes of " + fileName);
            data = new byte[fis.available()];
            fis.read(data);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        return data;
    }

    public static String getFirstDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); //获取本周一的日期
        return df.format(cal.getTime());
    }

    public static String getLastDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return df.format(cal.getTime());
    }

    //unit is like Calendar.DAY_OF_YEAR
    public static java.util.Date datePlus(java.util.Date orgDate, int unit, int value) {
        Calendar eDate = Calendar.getInstance();
        if (orgDate != null) {
            eDate.setTime(orgDate);
        }
        eDate.add(unit, value);
        return eDate.getTime();
    }

    public static boolean findInStringArray(String key, String[] strings) {
        return findInStringArray(key, strings, false);
    }

    public static boolean findInStringArray(String key, String[] strings, boolean ignoreCase) {
        boolean found = false;
        if (strings.length > 0 && key != null) {
            for (String tmp : strings) {
                if (ignoreCase) {
                    if (key.compareToIgnoreCase(tmp) == 0) {
                        found = true;
                        break;
                    }
                } else {
                    if (key.compareTo(tmp) == 0) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    public static boolean isHoliday(String date){
        Calendar bCal = Calendar.getInstance();
        bCal.setTime(Util.parseDate(date));
        return isHoliday(bCal);
    }

    public static boolean isHoliday(Calendar day) {
        boolean holiday = false;
        String[] holidays = {
                "2017-01-01", "2017-01-02", "2017-01-27", "2017-01-30", "2017-01-31", "2017-02-01",
                "2017-02-02", "2017-04-02", "2017-04-03", "2017-04-04", "2017-05-01", "2017-05-28", "2017-05-29",
                "2017-10-02", "2017-10-03", "2017-10-04", "2017-10-05", "2017-10-06", "2018-01-01", "2018-02-15",
                "2018-02-16", "2018-02-17", "2018-02-20", "2018-02-15", "2018-02-21",
                "2018-04-05", "2018-04-06", "2018-04-30", "2018-05-01",
                "2018-06-18", "2018-09-24", "2018-10-01", "2018-10-02",
                "2018-10-03", "2018-10-04", "2018-10-05"
        };
        String[] workdays = {
                "2017-01-22", "2017-02-04", "2017-04-01", "2017-05-27", "2017-09-30",
                "2018-02-11", "2018-02-24", "2018-04-08", "2018-04-28", "2018-09-29", "2018-09-30"
        };

        String tmpDate = Util.formatDate(day.getTime());

        if (day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            if (!findInStringArray(tmpDate, Configure.getConfig().getProperty("workdays").split(","))) {
                holiday = true;
            }
//            if (!findInStringArray(tmpDate, workdays)) {
//                holiday = true;
//            }
        }
        if (!holiday) {
            holiday = findInStringArray(tmpDate, Configure.getConfig().getProperty("holidays").split(","));
            //holiday = findInStringArray(tmpDate, holidays);
        }

        return holiday;
    }

    //计算两个时间之间的工作耗时，单位为秒
    public static long workDateDiff(String beginDate, String endDate) {
        long costSecond = 0;
        int workBeginHour = 9;
        int workBeginMinute = 0;
        int workBeginSecond = 0;
        int workEndHour = 18;
        int workEndMinute = 0;
        int workEndSecond = 0;

        if (beginDate != null && endDate != null) {

            try {
                Calendar bCal = Calendar.getInstance();
                bCal.setTime(Util.parseDateTime(beginDate));
                Calendar eCal = Calendar.getInstance();
                eCal.setTime(Util.parseDateTime(endDate));

                long oneWorkDayCost = (workEndHour - workBeginHour) * 3600 + (workEndMinute - workBeginMinute) * 60 + workEndSecond - workBeginSecond;

                //先判断开始时间是否非上班时间，校正为最近的上班时间
                Calendar tmpBCal = (Calendar) bCal.clone();
                tmpBCal.set(Calendar.HOUR_OF_DAY, workBeginHour);
                tmpBCal.set(Calendar.MINUTE, workBeginMinute);
                tmpBCal.set(Calendar.SECOND, workBeginSecond);
                Calendar tmpECal = (Calendar) bCal.clone();
                tmpECal.set(Calendar.HOUR_OF_DAY, workEndHour);
                tmpECal.set(Calendar.MINUTE, workEndMinute);
                tmpECal.set(Calendar.SECOND, workEndSecond);
                if (bCal.compareTo(tmpBCal) < 0) {
                    bCal = (Calendar) tmpBCal.clone();
                }
                if (bCal.compareTo(tmpECal) > 0) {
                    tmpBCal.add(Calendar.DAY_OF_YEAR, 1);
                    bCal = (Calendar) tmpBCal.clone();
                }

                //重新定位结束时间，如果为节假日，则定位到往前的最后一个工作日的下班时间
                while (isHoliday(bCal)) {
                    bCal.add(Calendar.DAY_OF_WEEK, 1);
                }

                //判断结束时间是否为非上班时间，校正为最近的下班时间
                tmpBCal = (Calendar) eCal.clone();
                tmpBCal.set(Calendar.HOUR_OF_DAY, workBeginHour);
                tmpBCal.set(Calendar.MINUTE, workBeginMinute);
                tmpBCal.set(Calendar.SECOND, workBeginSecond);

                tmpECal = (Calendar) eCal.clone();
                tmpECal.set(Calendar.HOUR_OF_DAY, workEndHour);
                tmpECal.set(Calendar.MINUTE, workEndMinute);
                tmpECal.set(Calendar.SECOND, workEndSecond);
                if (eCal.compareTo(tmpECal) > 0) {
                    eCal = (Calendar) tmpECal.clone();
                }
                if (eCal.compareTo(tmpBCal) < 0) {
                    tmpECal.add(Calendar.DAY_OF_YEAR, -1);
                    eCal = (Calendar) tmpECal.clone();
                }
                if (bCal.compareTo(eCal) < 0) {
                    //开始时间与结束时间为同一天
                    if (Util.formatDate(bCal.getTime()).compareTo(Util.formatDate(eCal.getTime())) == 0) {
                        costSecond += (eCal.getTimeInMillis() - bCal.getTimeInMillis()) / 1000;
                    } else {
                        //开始循环计算耗费时间，结束条件为到达结束日期
                        if (Util.formatDate(bCal.getTime()).compareTo(Util.formatDate(eCal.getTime())) < 0) {
                            tmpECal = (Calendar) bCal.clone();
                            tmpECal.set(Calendar.HOUR_OF_DAY, workEndHour);
                            tmpECal.set(Calendar.MINUTE, workEndMinute);
                            tmpECal.set(Calendar.SECOND, workEndSecond);
                            costSecond += (tmpECal.getTimeInMillis() - bCal.getTimeInMillis()) / 1000;
                            bCal.add(Calendar.DAY_OF_YEAR, 1);
                            bCal.set(Calendar.HOUR_OF_DAY, workBeginHour);
                            bCal.set(Calendar.MINUTE, workBeginMinute);
                            bCal.set(Calendar.SECOND, workBeginSecond);
                        }
                        while (Util.formatDate(bCal.getTime()).compareTo(Util.formatDate(eCal.getTime())) < 0) {    //开始日期与结束日期不在同一天，比结束时间小
                            //如果为节假日则定位到往后顺延的最近一个工作日
                            if (!isHoliday(bCal)) {
                                costSecond += oneWorkDayCost;
                            }
                            bCal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        tmpECal = (Calendar) eCal.clone();
                        tmpECal.set(Calendar.HOUR_OF_DAY, workBeginHour);
                        tmpECal.set(Calendar.MINUTE, workBeginMinute);
                        tmpECal.set(Calendar.SECOND, workBeginSecond);
                        costSecond += (eCal.getTimeInMillis() - tmpECal.getTimeInMillis()) / 1000;
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return costSecond;
    }

    public static String mysqlEscape(String val) {
        String tmp = val;
        if (tmp != null) {
            tmp = tmp.replaceAll("\\\\", "\\\\\\\\");
            tmp = tmp.replace("\'", "\\\'");
            tmp = tmp.replace("%", "\\%");
        }
        return tmp;
    }

    public static String mysqlAntiInject(String val) {
        String tmp = val;
        if (tmp != null) {
            tmp = tmp.replace(";", "");        //防注入措施，去掉断句分号
            tmp = tmp.replace("--", "");       //防注入措施，去掉注释符
            tmp = tmp.replace("/*", "");       //防注入措施，去掉注释符
            tmp = tmp.replace("\'", "\\\'");
        }
        return tmp;
    }

    public static String splitEncode(String value) {
        return value.replaceAll("\\\\,", "@@douhao@@");
    }

    public static String splitDecode(String value) {
        return value.replaceAll("@@douhao@@", "\\\\,").replaceAll("\\\\,", ",");
    }

    public static String regexpFix(String regStr) {
        String tmpStr = regStr;
        if (tmpStr.endsWith("|") && !tmpStr.endsWith("\\\\|")) {
            tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        }
        if (tmpStr.endsWith("\\")) {
            tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        }
        if (tmpStr.endsWith("(")) {
            tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        }
        if (tmpStr.endsWith("[")) {
            tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        }
        if (tmpStr.length() != regStr.length()) {
            tmpStr = regexpFix(tmpStr);
        }
        return tmpStr;
    }

    public static int findInJSONArray(JSONArray array, String key, String value) {
        int find = -1;
        for (int i = 0; i < array.size(); i++) {
            if (value.compareToIgnoreCase(((JSONObject) array.get(i)).getString(key)) == 0) {
                find = i;
                break;
            }
        }
        return find;
    }

    public static String redisEncode(String orgString) {
        return orgString.replace("|", "%7c");
    }

    public static String redisDecode(String encodeString) {
        return encodeString.replace("%7c", "|");
    }

    public static int random() {
        return new Random().nextInt();
    }

    public static void main(String[] argv){
        System.out.println(Util.isHoliday("2019-10-01"));
    }
}
