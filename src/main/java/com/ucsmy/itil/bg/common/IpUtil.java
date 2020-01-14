package com.ucsmy.itil.bg.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpUtil {

	/**
	 * 处理以指定开始ip到结束ip模板  [模板：start-end]
	 * @param startIp 开始ip
	 * @param endIp 结束ip
	 * @return
	 */
	public static List<String> grenerateIp(String startIp, String endIp) {
		// eg : 192.168.1.100-192.168.3.200
		long start = ipToLong(startIp);
		long end = ipToLong(endIp);
		
		List<String> rs = new ArrayList<>();
		for (; start <= end; start++) {
			rs.add(LongToIp(start));
		}
		
		return rs;
	}
	
	
	
	/**
	 * 处理以掩码方式指定的ip模板   [模版 ： start:mask]
	 * @param startIp 起始IP地址
	 * @param ipMask 子网掩码
	 */
	public static List<String> grenerateIpByNetmask(String startIp, String netmask) {
		// eg : 122.13.216.0/255.255.255.224
		long start = ipToLong(netmask);
		return grenerateIp(startIp, Long.bitCount(start));
	}
	
	
	/**
	 * 处理以掩码方式指定的ip模板   [模版 ： start/mask]
	 * @param startIp 起始IP地址
	 * @param ipMask 子网掩码
	 */
	public static List<String> grenerateIp(String startIp, int ipMask) {
		// eg : 192.168.1.0/24
		long start = ipToLong(startIp);
		Long e_ = start | ((int) Math.pow(2, 32 - ipMask) - 1);
		
		
		List<String> rs = new ArrayList<>();
		for (; start <= e_; start++) {
			rs.add(LongToIp(start));
		}
		
		return rs;
	}
	
	
	/**
	 * 处理以掩码方式指定的ip模板   [模版 ： start/mask]
	 * @param startIp 起始IP地址
	 * @param ipMask 子网掩码
	 */
	public static List<String> grenerateIpMask(String startIp) {
		// eg : 192.168.1.0/24
		String[] split = startIp.split("/");
		List<String> rs=null;

		if(isIp(split[0])) {
			long start = ipToLong(split[0]);
			Long e_=start;
			if(split.length>1) {
				 e_= start | ((int) Math.pow(2, 32 - Integer.parseInt(split[1])) - 1);
			}

			rs = new ArrayList<>();
			for (; start <= e_; start++) {
				rs.add(LongToIp(start));
			}
		}
		
		return rs;
	}
	
	
	/**
	 * 获取下一个Ip
	 * @param ip
	 * @return
	 */
	public static String grenerateNextIp(String ip) {
		long start = ipToLong(ip);
		return LongToIp(start + 1);
	}
	
	
	public static long ipToLong(String ip) {
		String[] ips = ip.split("\\.");
		int[] s = { Integer.parseInt(ips[0]), Integer.parseInt(ips[1]), Integer.parseInt(ips[2]),
				Integer.parseInt(ips[3]) };
		long s_ = (long) (s[0] * Math.pow(2, 24) + s[1] * Math.pow(2, 16) + s[2] * Math.pow(2, 8) + s[3]);
		return s_;
	}


	public static String LongToIp(long ip) {
		int i, j, k, l;
		String binaryString = Long.toBinaryString(ip);
		while (binaryString.length() < 32) {
			binaryString = "0"+binaryString;
		}
		
		i = Integer.parseInt(binaryString.substring(0, 8), 2);
		j = Integer.parseInt(binaryString.substring(8, 16), 2);
		k = Integer.parseInt(binaryString.substring(16, 24), 2);
		l = Integer.parseInt(binaryString.substring(24, 32), 2);
		
		return i + "." + j + "." + k + "." + l;
	}
	
	
	public static boolean isIp(String ipAddress) {
		String ip = "[1-9](\\d{1,2})?\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))";
		Pattern pattern = Pattern.compile(ip);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();   
	} 
	
	
	public static boolean isIpMask(String ipAddress) {
		String ip = "[1-9](\\d{1,2})?\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))/[1-9](\\d{1,2})?";
		Pattern pattern = Pattern.compile(ip);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();   
	}

	/**
	 * 获取ip段的网络号
	 * @param ipWithMask	ip池地址，格式如 10.0.0.1/28
	 * @return		网络号，格式如10.0.0.0，如没有掩码则直接返回此ip，如ip格式错误则返回Null
	 */
	public static String getIpPoolNum(String ipWithMask){
		String poolNum=null;
		if(isIpMask(ipWithMask)){
			String[] ipMask=ipWithMask.split("/");
			poolNum=getIpPoolNum(ipMask[0],num2Mask(Integer.parseInt(ipMask[1])));
		}else if(isIp(ipWithMask))poolNum=ipWithMask;
		return poolNum;
	}

	/**
	 * 获取ip段的网络号
	 * @param ip	ip池地址，格式如：10.0.0.1
	 * @param netmask	掩码，格式如：255.255.255.240
	 * @return			网络号，格式如10.0.0.0
	 */
	public static String getIpPoolNum(String ip,String netmask){
		return LongToIp(ipToLong(ip)&ipToLong(netmask));
	}

	/**
	 * 数值掩码转换为ip掩码
	 * @param masknum	数值掩码，如28
	 * @return			ip掩码，如：255.255.255.240
	 */
	public static String num2Mask(int masknum){
		return LongToIp(ipToLong("255.255.255.255")-(long)Math.pow(2,32-masknum)-1);
	}

	/**
	 * ip掩码转换为数值掩码
	 * @param mask		ip掩码，如：255.255.255.240
	 * @return			数值掩码，如28
	 */
	public static int mask2Num(String mask){
		long tmp=ipToLong("255.255.255.255")-ipToLong(mask);
		return tmp==0?32:32-Long.toBinaryString(tmp).length();
	}

	/**
	 * 判断ip是否属于该ip池
	 * @param ip		判断ip
	 * @param poolip	ip池地址
	 * @param netmask	掩码
	 * @return
	 */
	public static boolean isInIpPool(String ip,String poolip,String netmask){
		boolean inPool=false;
		if(ip!=null && poolip!=null && netmask!=null){
			long tmpip=ipToLong(ip)&ipToLong(netmask);
			long tmppool=ipToLong(poolip) & ipToLong(netmask);
			if( tmpip==tmppool )inPool=true;
		}
		return inPool;
	}
	
}
