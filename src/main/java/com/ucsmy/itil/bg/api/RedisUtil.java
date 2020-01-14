package com.ucsmy.itil.bg.api;

import com.alibaba.fastjson.JSONObject;
import com.ucsmy.itil.bg.common.Configure;
import com.ucsmy.itil.bg.common.UcsmyLog;
import com.ucsmy.itil.bg.service.SystemLog;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Properties;

public class RedisUtil {

    //Redis服务器IP
    private static String ADDR = "172.17.21.188";
    //private static String ADDR = "127.0.0.1";

    //Redis的端口号
    private static int PORT = 6379;

    //访问密码
    private static String AUTH = "admin";

    //可用连接实例的最大数目，默认值为8；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    private static int MAX_ACTIVE = 256;

    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 20;

    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 2000;

    private static int TIMEOUT = 2000;

    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;

    private static JedisPool jedisPool = null;

    private static UcsmyLog logger = new UcsmyLog(RedisUtil.class.getName());

    /**
     * 初始化Redis连接池
     */
    static {
        try {
            Properties redisProperties = Configure.getConfig();
            if (redisProperties != null) {
                ADDR = redisProperties.getProperty("redis.addr");
                PORT = Integer.parseInt(redisProperties.getProperty("redis.port"));
                AUTH = redisProperties.getProperty("redis.password");
                System.out.println("Reids server at " + ADDR + ":" + PORT);
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(MAX_ACTIVE);
                config.setMaxIdle(MAX_IDLE);
                config.setMaxWaitMillis(MAX_WAIT);
                config.setTestOnBorrow(TEST_ON_BORROW);
                jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT,AUTH);
            }
            //jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis error:" + e.getMessage());
        }
    }

    /**
     * 获取Jedis实例
     *
     * @return
     */
    public synchronized static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis error:" + e.getMessage());
            return null;
        }
    }

    /**
     * 释放jedis资源
     *
     * @param jedis
     */
    public static void returnResource(final Jedis jedis) {
        if (jedis != null) {
            jedisPool.returnResource(jedis);
        }
    }

    public static int batchSet(String key, ArrayList<JSONObject> values) {
        int count = 0;
        Jedis jedis = jedisPool.getResource();
        for (JSONObject value : values) {
            try {
                jedis.set(value.getString(key), value.toString());
                count++;
            } catch (Exception e) {
                //e.printStackTrace();
                logger.error("Redis error:" + e.getMessage());
            }
        }
        jedisPool.returnResource(jedis);
        return count;
    }

    public static void main(String[] args) {
        RedisUtil.getJedis().lpush("test", "test");
    }
}