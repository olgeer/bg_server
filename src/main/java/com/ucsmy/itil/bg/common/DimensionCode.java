package com.ucsmy.itil.bg.common;

import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Max on 2017/1/19.
 */
public class DimensionCode {
    private static final int DIMENSIONS = 2;
    private static final int BORDER_SIZE = 16;
    private static final int AREA = BORDER_SIZE * BORDER_SIZE;
    private static final int FILEMAXSIZE = 100 * 1024 * 1024;   //100MB

    //以文件为输入对象的多维码产生函数
    public static String encrypt(File source) {
        String retString = null;
        FileInputStream fis = null;
        byte[] data = null;
        try {
            fis = new FileInputStream(source);
            int filesize = fis.available();
            if (filesize <= FILEMAXSIZE) {
                data = new byte[filesize];
                fis.read(data);
                retString = encrypt(data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return retString;
    }

    //以字符串为输入对象的多维码产生函数
    public static String encrypt(String source) {
        return encrypt(source.getBytes());
    }

    //以字节数组为输入对象的多维码产生函数
    public static String encrypt(byte[] source) {
        Random random = new Random(source.length * checkSum(source));
        byte[] randomArray = new byte[AREA];
        random.nextBytes(randomArray);

        byte[] tmp = source.clone();
        String retString = "";
        int l = tmp.length;
        int t = 0;
        for (int i = 0; i < l; i++) {
            t = (byte) tmp[i] + t;
            //t = (byte) tmp[i];
            if (t < 0) t = t + 256; //单字节最大为255，控制不为负数，所以此处恒定为256，与维度及边长无关；
            if (t > AREA - 1) t = t % AREA;
            //while (t > AREA - 1) t = t - AREA;
            randomArray[t] = (byte) (t + randomArray[t]);
        }

        for (int x = 0; x < (BORDER_SIZE - 1); x++) {
            for (int y = 0; y < (BORDER_SIZE - 1); y++) {
                randomArray[x * BORDER_SIZE + (BORDER_SIZE - 1)] = (byte) (randomArray[x * BORDER_SIZE + y] + randomArray[x * BORDER_SIZE + (BORDER_SIZE - 1)]);
                randomArray[(BORDER_SIZE - 1) * BORDER_SIZE + y] = (byte) (randomArray[x * BORDER_SIZE + y] + randomArray[(BORDER_SIZE - 1) * BORDER_SIZE + y]);
            }
        }

        for (int i = 0; i < BORDER_SIZE - 1; i++) {
            retString += toHexString(randomArray[i * BORDER_SIZE + (BORDER_SIZE - 1)]);
            retString += toHexString(randomArray[(BORDER_SIZE - 1) * BORDER_SIZE + i]);
        }
        retString += toHexString(randomArray[AREA - 1]);
        retString += toHexString((byte) checkSum(retString.toUpperCase().getBytes()));

        return retString.toUpperCase();
    }

    //多维码校验函数，返回true则校验合法，false为校验失败
    public static boolean valid(String code) {
        return toHexString((byte) checkSum(code.substring(0, code.length() - 2).getBytes())).toUpperCase().compareTo(code.substring(code.length() - 2)) == 0;
    }

    //校验和生成函数
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

    //字符数组转十六进制字符串
    public static String toHexString(byte b) {
        String tmpStr = Integer.toHexString(b);
        if (tmpStr.length() > 2) tmpStr = tmpStr.substring(tmpStr.length() - 2, tmpStr.length());
        if (tmpStr.length() < 2) tmpStr = "0" + tmpStr;
        return tmpStr;
    }

    public static void main(String[] args) {
        //String fileName = "D:\\优迈\\UCS\\个人文件\\staff.mht";
        String fileName = "D:\\360Downloads\\sdk_package.zip";
        //String fileName = "D:\\spider\\shixin\\个人12K3.txt";

        byte[] data = Util.readFile(fileName);
        java.util.Date begin = new java.util.Date();
        System.out.println(Util.encrypt(data));
        java.util.Date md5 = new java.util.Date();
        System.out.println("Pass:" + (md5.getTime() - begin.getTime()) + "ms,Begin:" + begin + ",End:" + md5);
        String teststr = DimensionCode.encrypt(data);
        System.out.println(teststr);
        System.out.println("Vaild ? " + DimensionCode.valid(teststr));
        java.util.Date end = new java.util.Date();
        System.out.println("Pass:" + (end.getTime() - md5.getTime()) + "ms,Begin:" + md5 + ",End:" + end);

    }
}
