package com.newegg.ec.redis.client;

import com.google.common.base.Strings;
import com.newegg.ec.redis.entity.AutoCommandParam;
import com.newegg.ec.redis.entity.AutoCommandResult;
import com.newegg.ec.redis.entity.DataCommandsParam;
import com.newegg.ec.redis.util.RedisUtil;
import com.newegg.ec.redis.util.SignUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.newegg.ec.redis.client.RedisURI.MAX_ATTEMPTS;
import static com.newegg.ec.redis.client.RedisURI.TIMEOUT;
import static com.newegg.ec.redis.util.RedisUtil.removeCommandAndKey;

/**
 * @author Jay.H.Zou
 * @date 2019/7/22
 */
public class RedisClusterClient implements IRedisClusterClient {

    private JedisCluster jedisCluster;

    private RedisClient redisClient;

    public RedisClusterClient(RedisURI redisURI) {
        Set<HostAndPort> hostAndPortSet = redisURI.getHostAndPortSet();
        String redisPassword = redisURI.getRequirePass();
        if (Strings.isNullOrEmpty(redisPassword)) {
            jedisCluster = new JedisCluster(hostAndPortSet);
        } else {
            jedisCluster = new JedisCluster(hostAndPortSet, TIMEOUT, TIMEOUT, MAX_ATTEMPTS, redisPassword, new GenericObjectPoolConfig());
        }
        redisClient = RedisClientFactory.buildRedisClient(redisURI);
    }

    @Override
    public JedisCluster getRedisClusterClient() {
        return jedisCluster;
    }

    @Override
    public boolean exists(String key) {
        return jedisCluster.exists(key);
    }

    @Override
    public String type(String key) {
        return jedisCluster.type(key);
    }

    @Override
    public long ttl(String key) {
        return jedisCluster.ttl(key);
    }

    @Override
    public Long del(String key) {
        return jedisCluster.del(key);
    }

    @Override
    public AutoCommandResult query(AutoCommandParam autoCommandParam) {
        String key = autoCommandParam.getKey();
        int count = autoCommandParam.getCount();
        String type = type(key);
        long ttl = ttl(key);
        Object value = null;
        switch (type) {
            case TYPE_STRING:
                value = jedisCluster.get(key);
                break;
            case TYPE_HASH:
                value = jedisCluster.hgetAll(key);
                break;
            case TYPE_LIST:
                value = jedisCluster.lrange(key, 0, count);
                break;
            case TYPE_SET:
                value = jedisCluster.srandmember(key, count);
                break;
            case TYPE_ZSET:
                value = jedisCluster.zrangeWithScores(key, 0, count);
                break;
            default:
                break;
        }
        return new AutoCommandResult(ttl, type, value);
    }

    @Override
    public Object string(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String[] list = SignUtil.splitBySpace(command);
        String cmd = command.toUpperCase();
        String key = list[1];
        Object result = null;
        if (cmd.startsWith(GET)) {
            result = jedisCluster.get(key);
        } else if (cmd.startsWith(SET)) {
            result = jedisCluster.set(key, list[2]);
        }
        return result;
    }

    @Override
    public Object hash(DataCommandsParam dataCommandsParam) {
        AutoCommandResult autoCommandResult = new AutoCommandResult();
        String command = dataCommandsParam.getCommand();
        String[] list = SignUtil.splitBySpace(command);
        String cmd = command.toUpperCase();
        String key = list[1];
        String type = type(key);
        long ttl = ttl(key);
        Object result = null;
        if (cmd.startsWith(HGETALL)) {
            result = jedisCluster.hgetAll(key);
        } else if (cmd.startsWith(HGET)) {
            result = jedisCluster.hget(key, list[2]);
        } else if (cmd.startsWith(HMGET)) {
            String[] items = removeCommandAndKey(list);
            result = jedisCluster.hmget(key, items);
        } else if (cmd.startsWith(HKEYS)) {
            result = jedisCluster.hkeys(key);
        } else if (cmd.startsWith(HSET)) {
            Map<String, String> hash = new HashMap<>();
            String[] items = removeCommandAndKey(list);
            for (int i = 0; i < items.length; i += 2) {
                String subKey = items[i];
                if (!Strings.isNullOrEmpty(subKey)) {
                    hash.put(subKey, items[i + 1]);
                }
            }
            result = jedisCluster.hset(key, hash);
        }
        autoCommandResult.setTtl(ttl);
        autoCommandResult.setType(type);
        autoCommandResult.setValue(result);
        return autoCommandResult;
    }

    @Override
    public Object list(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String[] list = SignUtil.splitBySpace(command);
        String cmd = command.toUpperCase();
        String key = list[1];
        String[] items = removeCommandAndKey(list);
        Object result = null;
        if (cmd.startsWith(LPUSH)) {
            result = jedisCluster.lpush(key, items);
        } else if (cmd.startsWith(RPUSH)) {
            result = jedisCluster.rpush(key, items);
        } else if (cmd.startsWith(LINDEX)) {
            result = jedisCluster.lindex(key, Integer.parseInt(list[2]));
        } else if (cmd.startsWith(LLEN)) {
            result = jedisCluster.llen(key);
        } else if (cmd.startsWith(LRANGE)) {
            int start = Integer.parseInt(list[2]);
            int stop = Integer.parseInt(list[3]);
            result = jedisCluster.lrange(key, start, stop);
        }
        return result;
    }

    @Override
    public Object set(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String[] list = SignUtil.splitBySpace(command);
        String cmd = command.toUpperCase();
        String key = list[1];
        Object result = null;
        if (cmd.startsWith(SCARD)) {
            result = jedisCluster.scard(key);
        } else if (cmd.startsWith(SADD)) {
            result = jedisCluster.sadd(key, removeCommandAndKey(list));
        } else if (cmd.startsWith(SMEMBERS)) {
            result = jedisCluster.smembers(key);
        } else if (cmd.startsWith(SRANDMEMBER)) {
            int count = 1;
            if (list.length > 2) {
                count = Integer.parseInt(list[2]);
            }
            result = jedisCluster.srandmember(key, count);
        }
        return result;
    }

    @Override
    public Object zset(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String[] list = SignUtil.splitBySpace(command);
        String cmd = command.toUpperCase();
        String key = list[1];
        String param1 = list[2];
        String param2 = list[3];
        Object result = null;
        if (cmd.startsWith(ZCARD)) {
            result = jedisCluster.zcard(key);
        } else if (cmd.startsWith(ZSCORE)) {
            result = jedisCluster.zscore(key, param1);
        } else if (cmd.startsWith(ZCOUNT)) {
            result = jedisCluster.zcount(key, param1, param2);
        } else if (cmd.startsWith(ZRANGE)) {
            int start = Integer.parseInt(param1);
            int stop = Integer.parseInt(param2);
            if (list.length > 4) {
                result = jedisCluster.zrangeWithScores(key, start, stop);
            } else {
                result = jedisCluster.zrange(key, start, stop);
            }
        } else if (cmd.startsWith(ZADD)) {
            result = jedisCluster.zadd(key, Double.parseDouble(param1), param2);
        }
        return result;
    }

    @Override
    public Object type(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String key = RedisUtil.getKey(command);
        return type(key);
    }

    @Override
    public Object del(DataCommandsParam dataCommandsParam) {
        String command = dataCommandsParam.getCommand();
        String key = RedisUtil.getKey(command);
        return del(key);
    }

    @Override
    public void close() {
        if (redisClient != null) {
            redisClient.close();
        }
        if (jedisCluster != null) {
            jedisCluster.close();
        }
    }

}
