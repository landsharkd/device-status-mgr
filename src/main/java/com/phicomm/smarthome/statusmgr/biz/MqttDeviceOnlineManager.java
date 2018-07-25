package com.phicomm.smarthome.statusmgr.biz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ClusterSlotHashUtil;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import com.phicomm.smarthome.cache.Cache;
import com.phicomm.smarthome.statusmgr.constants.Contants;
import com.sun.jersey.client.impl.async.FutureClientResponseListener;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/**
 * 
 * package: com.phicomm.smarthome.statusmgr.biz
 * class: MqttDeviceOnlineManager.java
 * date: 2018年6月28日 上午11:15:50
 * author: wen.xia
 * description:
 */
@Component
@SuppressWarnings({"unchecked","rawtypes", "unused"})
public class MqttDeviceOnlineManager {
    
    private static Logger logger = LogManager.getLogger(MqttDeviceOnlineManager.class);
    
    @Autowired
    private Cache cache;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    private ThreadPoolExecutor executor = 
            new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS
                    , new ArrayBlockingQueue<>(1000), new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        }
                    });
        
    // device_status_mgr_bv_map : ${broker} -> ${version}
    public static final String BROKERS_VERSION_MAP = Contants.APP_NAME + Contants.SEP + "bv"+ Contants.SEP + "map";
    
    //device_status_mgr_bvdsp_ : device_status_mgr_bvdsp_${broker}_${version}_${deviceId} -> ${onlineTime}
    public static final String BROKER_VERSION_DEVICE_STATUS_PREFIX = Contants.APP_NAME + Contants.SEP + "bvdsp" + Contants.SEP;
    //device_status_mgr_dsqrp_ : device_status_mgr_dsqrp_${deviceId} -> true/false
    public static final String DEVICE_STATUS_QUICK_READ_PREFIX = Contants.APP_NAME + Contants.SEP + "dsqrp" + Contants.SEP;
    
    private String makeBroVerDevStatKey(String broker, Long version, String deviceId) {
        return BROKER_VERSION_DEVICE_STATUS_PREFIX + broker + Contants.SEP + version + Contants.SEP + deviceId;
    }
    
    private String makeDevStatQckRdKey(String deviceId) {
        return DEVICE_STATUS_QUICK_READ_PREFIX + deviceId;
    }
    
    private String pareseDeviceIdFromBroVerDevStatKey(String key) {
        return key.substring(key.lastIndexOf(Contants.SEP) + 1); 
    }
    
    private Long getBroVerDevStat(String broker, Long version, String deviceId) {
       return (Long) cache.get(makeBroVerDevStatKey(broker, version, deviceId));
    }
    
    private Boolean getDevStatQckRd(String deviceId) {
        long start = System.currentTimeMillis();
        String key = makeDevStatQckRdKey(deviceId);
        Boolean r = (Boolean) cache.get(key);
        logger.debug(String.format("getDevStatQckRd redis get[%s ms], key = %s, result = %s", System.currentTimeMillis() - start
                , key, r));
        return r;
    }
    
    private Map<String, Object> getBroVerMap(){
        long start = System.currentTimeMillis();
        Map<String, Object> map = cache.getHash(BROKERS_VERSION_MAP);
        logger.debug(String.format("getBroVerMap redis get[%s ms], key = %s, result = %s", System.currentTimeMillis() - start
                , BROKERS_VERSION_MAP, map));
        return map;
    }
    
    private Long getBroVer(String broker) {
        return (Long)cache.getHashValue(BROKERS_VERSION_MAP, broker);
    }
    
    private void putBroVerDevStat(String broker, Long version, String deviceId, long time) {
         cache.put(makeBroVerDevStatKey(broker, version, deviceId), time, -1);
     }
     
     /*private void putDevStatQckRd(String deviceId, boolean online , RedisOperations opt) {
         opt.opsForValue().set(makeDevStatQckRdKey(deviceId), online);
     }*/
    
    private void putBroVerDevStat(String broker, Long version, String deviceId, long time, RedisOperations opt) {
         opt.opsForValue().set(makeBroVerDevStatKey(broker, version, deviceId), time);
     }
     
     private void putDevStatQckRd(String deviceId, boolean online) {
         cache.put(makeDevStatQckRdKey(deviceId), online, -1);
     }
     
     private void putBroVer(String broker, Long version){
         cache.putHashValue(BROKERS_VERSION_MAP, broker, version, -1);
     }
     
     private void deleteBroVerDevStat(String broker, Long version, String deviceId) {
         cache.delete(makeBroVerDevStatKey(broker, version, deviceId));
     }
     
     private void deleteDevStatQckRd(String deviceId) {
         cache.delete(makeDevStatQckRdKey(deviceId));
     }
     
     private void deleteBroVerDevStat(String broker, Long version, String deviceId, RedisOperations opt) {
         opt.delete(makeBroVerDevStatKey(broker, version, deviceId));
     }
     
     
    private void deleteDevStatQckRd(String deviceId, RedisOperations opt) {
         opt.delete(makeDevStatQckRdKey(deviceId));
     }
    
    public void deviceOnline(String broker, String deviceId) {
        synchronized (deviceId) {
            long now = System.currentTimeMillis();
            Long version = getBroVer(broker);
            if(version == null) {
                version = 1L;
                putBroVer(broker, version);
            }
            
            //事务&管道
            /*final Long ver = version;
            List<Object> result = redisTemplate.executePipelined(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    putBroVerDevStat(broker, ver, deviceId, now, operations);
                    logger.info(String.format("online: device = %s on broker = %s, version = %s , onlineTimestamp = %s[%s]"
                            , deviceId, broker, ver, now, new Date(now)));
                    predict(deviceId, broker, now, operations);
                    deleteDeviceStatusFromReadCache(deviceId, operations);
                    return operations.exec();
                }
            });
            logger.info(String.format("deviceOnline update[%s ms] redis by transaction [put st, delete kicks, delete qr],  result : %s"
                    , System.currentTimeMillis() - now, result));*/
            
            //非事物操作
            putBroVerDevStat(broker, version, deviceId, now);
            logger.info(String.format("online: device = %s on broker = %s, version = %s , onlineTimestamp = %s[%s]"
                    , deviceId, broker, version, now, new Date(now)));
            predict(deviceId, broker, now);
            deleteDeviceStatusFromReadCache(deviceId);
            logger.info(String.format("deviceOnline update[%s ms] redis"
                    , System.currentTimeMillis() - now));
        }
    }
    
    public void deviceOffline(String broker, String deviceId) {
        synchronized (deviceId) {
            long now = System.currentTimeMillis();
            Long version = getBroVer(broker);
            //事务&管道
            /*final Long ver = version;
            List<Object> result = redisTemplate.executePipelined(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    deleteBroVerDevStat(broker, ver, deviceId, operations);
                    deleteDeviceStatusFromReadCache(deviceId, operations);
                    logger.info(String.format("offline: device = %s on broker = %s, version = %s , offlineTimestamp = %s[%s]"
                            , deviceId, broker, ver, now, new Date(now)));
                    return operations.exec();
                }
            });
            logger.info(String.format("deviceOffline update[%s ms] redis by transaction [delete st, delete qr],  result : %s" 
                    , System.currentTimeMillis() - now, result));*/
            
            
            deleteBroVerDevStat(broker, version, deviceId);
            deleteDeviceStatusFromReadCache(deviceId);
            logger.info(String.format("offline: device = %s on broker = %s, version = %s , offlineTimestamp = %s[%s]"
                    , deviceId, broker, version, now, new Date(now)));
            logger.info(String.format("deviceOffline update[%s ms] redis"
                    , System.currentTimeMillis() - now));
        }
    }
    
    public void clearBroker(String broker) {
        Long version = getBroVer(broker);
        Long newVersion = (version == null) ? 1L : version + 1L;
        putBroVer(broker, newVersion);
        logger.info(String.format("clearBroker: broker = %s", broker));
        //异步删除旧version和quick read
        executor.submit(new Runnable() {
            @Override
            public void run() {
                String keyPatten = makeBroVerDevStatKey(broker, version, "*");
                Set<String> oldKeys = redisTemplate.keys(keyPatten);
                //删除quickRead
                Set<String> qkRdKeys = new HashSet<>();
                for(String oldKey : oldKeys) {
                    qkRdKeys.add(makeDevStatQckRdKey(pareseDeviceIdFromBroVerDevStatKey(oldKey)));
                }
                mDelByConcurrency(qkRdKeys);
                logger.info(String.format("clearBroker - delete quick read devices, broker = %s, qkRdKeysSize = %s", broker, qkRdKeys.size()));
                mDelByConcurrency(oldKeys);
                logger.info(String.format("clearBroker - delete old version broker devices, broker = %s, version = %s , oldKeysSize = %s", broker, version, oldKeys.size()));
            }
        });
    }
    
    public boolean isOnline(String deviceId) {
        if(deviceId == null || StringUtils.isBlank(deviceId)) {
            throw new IllegalArgumentException("deviceId is blank");
        }
        
        Map<String, Object> broVerMap = getBroVerMap();
        if(broVerMap == null || broVerMap.size() == 0) {
            return false;
        }
        Set<String> statusKeys = new HashSet<>();
        for(Entry<String, Object> entry : broVerMap.entrySet()) {
            String broker = entry.getKey();
            Long version = (Long)entry.getValue();
            if(StringUtils.isNotBlank(broker) && version != null) {
                statusKeys.add(makeBroVerDevStatKey(broker, version, deviceId));
            }
        }
        
        List<Long> statusList = mgetByConcurrency(statusKeys, false);
        if(CollectionUtils.isNotEmpty(statusList)) {
            for(Long s : statusList) {
                if(s != null && s > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public List<Boolean> isOnlines(List<String> deviceIds) throws InterruptedException, ExecutionException {
        Map<String, Integer> replicateMap = new HashMap<>();
        List<Boolean> result = new ArrayList<>(deviceIds.size());
        for(int i = 0 ; i < deviceIds.size(); i++) {
               String deviceId = deviceIds.get(i);
               if(replicateMap.put(deviceId, 1) != null) {
                   throw new IllegalArgumentException(String.format("deviceIds=%s, but deviceIds[%s]=%s 重复", deviceIds, i, deviceId));
               }
               if(deviceId == null || StringUtils.isBlank(deviceId)) {
                   throw new IllegalArgumentException(String.format("deviceIds=%s, but deviceIds[%s] is blank", deviceIds, i, deviceId));
               }
               result.add(i, false);
        }

        Map<String, Object> broVerMap = getBroVerMap();
        if(broVerMap == null || broVerMap.size() == 0) {
            return result;
        }
        
        List<String> statusKeys = new ArrayList<>();
        
        //获取所有需要查询的keys
        for(Entry<String, Object> entry : broVerMap.entrySet()) {
            String broker = entry.getKey();
            Long version = (Long)entry.getValue();
            if(StringUtils.isNotBlank(broker) && version != null) {
                for(String deviceId : deviceIds) {
                    statusKeys.add(makeBroVerDevStatKey(broker, version, deviceId));
                }
            }
        }
        
        List<Long> statusList = mgetByConcurrency(statusKeys, true);
        
        //根据将调用结果，计算成在线状态
        if(CollectionUtils.isNotEmpty(statusList)) {
            for(int i=0 ; i < statusList.size(); ) {
                Long s = statusList.get(i);
                if(s != null && s > 0) {
                    int devIndex = i % deviceIds.size() ;
                    result.set(devIndex, true);
                    //i = (i/deviceIds.size() + 1) * deviceIds.size();
                   // continue;
                }
                i++;
            }
        }
        return result;
    }
    
    public boolean isOnlineWithReadCache(String deviceId) {
        if(deviceId == null || StringUtils.isBlank(deviceId)) {
            throw new IllegalArgumentException("deviceId is blank");
        }
        try {
            Boolean onlineStatus = getDeviceStatusFromReadCache(deviceId);
            if(onlineStatus != null) {
                return onlineStatus;
            }
        }catch(Exception e) {
            logger.error(String.format("getDeviceStatusFromReadCache error, deviceId = %s , ", deviceId), e);
        }
        
        boolean online = isOnline(deviceId);
        
        try {
            putDeviceStatusToReadCache(deviceId, online);
        }catch(Exception e) {
            logger.error(String.format("putDeviceStatusToReadCache error ,  deviceId = %s , online=%s", deviceId, online), e);
        }
        
        return online;
    }
    
    public Map<String, Map<String, Object>> getOnlineData() {
        Map<String, Map<String, Object>> r = new HashMap<String, Map<String,Object>>();
        
        Map<String, Object> broVerMap = getBroVerMap();
        if(broVerMap == null || broVerMap.size() == 0) {
            return null;
        }
        
        List<String> brokers = new ArrayList<>();
        List<String> allKeys = new ArrayList<>();
        Map<String, Integer> brokerSizeMap = new HashMap<>();
        List<Future<Set<String>>> fList= new ArrayList<>();
        for(Entry<String, Object> entry : broVerMap.entrySet()) {
            String broker = entry.getKey();
            Long version = (Long)entry.getValue();
            if(StringUtils.isNotBlank(broker) && version != null) {
                brokers.add(broker);
               //brokerSizeMap.put(broker, keysList.size());
                fList.add(executor.submit(new Callable<Set<String>>() {
                    @Override
                    public Set<String> call() throws Exception {
                        long now = System.currentTimeMillis(); 
                        Set<String> keys = redisTemplate.keys(makeBroVerDevStatKey(broker, version, "*"));
                        logger.debug(String.format("getAllOnlineData redis keys pattern[%s ms], pattern = %s, result = %s", System.currentTimeMillis() - now
                                , makeBroVerDevStatKey(broker, version, "*"), keys));
                        return keys;
                    }
                }));
            }
        }
        
        for(int i = 0 ; i < fList.size(); i++) {
            Set<String> s = null;
            try {
                s = fList.get(i).get();
            } catch (Exception e) {
                logger.error("", e);
            }
            if(s != null) {
                allKeys.addAll(s);
                brokerSizeMap.put(brokers.get(i), s.size());
            }
            
        }
        
        List<Long> statusList = mgetByConcurrency(allKeys, true);
        int start = 0;
        for(String broker : brokers) {
            int size = brokerSizeMap.get(broker);
            List<String> brokerKeys = allKeys.subList(start, start + size);
            List<Long> brokerStatus = statusList.subList(start, start + size);
            r.put(broker, new HashMap<>());
            for(int i = 0 ; i < brokerKeys.size(); i ++) {
                r.get(broker).put(brokerKeys.get(i), brokerStatus.get(i));
            }
            start = start + size;
        }
        return r;
    }
    
    public Map<String, Object> getOnlineReadCache() {
        Map<String, Object> r = new HashMap<>();
        Set<String> keys = redisTemplate.keys(makeDevStatQckRdKey("*"));
        if(keys != null && keys.size() > 0) {
            List<String> keysList = new ArrayList<>(keys);
            List<Boolean> qResult = mgetByConcurrency(keysList, true);
            if(qResult != null) {
                for(int i = 0 ; i< keysList.size(); i++) {
                    r.put(keysList.get(i), qResult.get(i));
                }
            }
        }
        return r;
    }
    
    private <K, V> List<V> mgetByConcurrency(Collection<K> statusKeys, boolean needOrdered) {
        long start = System.currentTimeMillis();
        List statusList = new ArrayList<>();
        if(statusKeys == null || statusKeys.size() == 0) {
            return statusList;
        }
        
        if(statusKeys.size() == 1) {
            K key = statusKeys.iterator().next();
            statusList.add(redisTemplate.opsForValue().get(key));
            logger.debug(String.format("mgetByConcurrency[%s ms], statusKeys = %s, statusList = %s", System.currentTimeMillis() - start
                    , statusKeys, statusList));
            return statusList;
        }
        
        try {
            //转换成同slot的keys
            Map<Integer, List<K>> slotKeysMap = new HashMap<>();
            for(K key : statusKeys) {
                int slot = ClusterSlotHashUtil.calculateSlot(redisTemplate.getKeySerializer().serialize(key));
                List<K> slotKeys= slotKeysMap.get(slot);
                if(slotKeys == null) {
                    slotKeys = new ArrayList<>();
                    slotKeysMap.put(slot, slotKeys);
                }
                slotKeys.add(key);
            }
            
            //根据slot并发调用keys
            Map<Integer, Future<List<V>>> futureMap = new HashMap<>();
            for(Entry<Integer, List<K>> entry : slotKeysMap.entrySet()) {
                List<K> slotkeysList = entry.getValue();
                futureMap.put(entry.getKey(), executor.submit(new Callable<List<V>>() {
                    @Override
                    public List<V> call() throws Exception {
                        return redisTemplate.opsForValue().multiGet(slotkeysList);
                    }
                }));
            }

            //不需要按顺序返回， 则直接返回
            if(!needOrdered || statusKeys instanceof Set) {
                for(Entry<Integer, Future<List<V>>> entry : futureMap.entrySet()) {
                    statusList.addAll(entry.getValue().get());
                }
                logger.debug(String.format("mgetByConcurrency[%s ms], statusKeys = %s, statusList = %s", System.currentTimeMillis() - start
                        , statusKeys, statusList));
                return statusList;
            }
            
            //根据slot获取调用结果
            Map<Integer, List<V>> slotResultMap = new HashMap<>();
            for(Entry<Integer, Future<List<V>>> entry : futureMap.entrySet()) {
                slotResultMap.put(entry.getKey(), entry.getValue().get());
            }
            
            //遍历调用结果，根据multiGet的有序性, 将result和key对应起来
            Map<K, V> keyResultMap = new HashMap<>();
            for(Entry<Integer, List<K>> entry : slotKeysMap.entrySet()) {
                List<K> slotKeys = entry.getValue();
                List<V> slotResults = slotResultMap.get(entry.getKey());
                for(int i = 0; i < slotKeys.size(); i++) {
                    if(slotResults != null) {
                        keyResultMap.put(slotKeys.get(i), slotResults.get(i));
                    }
                }
            }
            
            //根据key的顺序把调用结果装入list
            for(K statusKey : statusKeys) {
                statusList.add(keyResultMap.get(statusKey));
            }
        }catch(Exception e) {
            logger.error("mgetByConcurrency call redis by slot error , ", e);
            statusList = redisTemplate.opsForValue().multiGet(statusKeys);
        }
        logger.debug(String.format("mgetByConcurrency[%s ms], statusKeys = %s, statusList = %s", System.currentTimeMillis() - start
                , statusKeys, statusList));
        return statusList;
    }

    private <K> void mDelByConcurrency(Collection<K> statusKeys) {
        
        List statusList = new ArrayList<>();
        if(statusKeys == null || statusKeys.size() == 0) {
            return;
        }
        
        if(statusKeys.size() == 1) {
            K key = statusKeys.iterator().next();
            redisTemplate.delete(key);
            return;
        }
        try {
            //转换成同slot的keys
            Map<Integer, List<K>> slotKeysMap = new HashMap<>();
            for(K key : statusKeys) {
                int slot = ClusterSlotHashUtil.calculateSlot(redisTemplate.getKeySerializer().serialize(key));
                List<K> slotKeys= slotKeysMap.get(slot);
                if(slotKeys == null) {
                    slotKeys = new ArrayList<>();
                    slotKeysMap.put(slot, slotKeys);
                }
                slotKeys.add(key);
            }
            
            //根据slot并发调用keys
            for(Entry<Integer, List<K>> entry : slotKeysMap.entrySet()) {
                List<K> slotkeysList = entry.getValue();
                executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                         redisTemplate.delete(slotkeysList);
                         return null;
                    }
                });
            }
        }catch(Exception e) {
            logger.error("mgetByConcurrency call redis by slot error , ", e);
            redisTemplate.delete(statusKeys);
        }
        
    }
    
    /**
     * read cache
     */
    private void deleteDeviceStatusFromReadCache(String deviceId) {
        deleteDevStatQckRd(deviceId);
    }
    
    private void deleteDeviceStatusFromReadCache(String deviceId, RedisOperations opt) {
        deleteDevStatQckRd(deviceId, opt);
    }
    
    private void putDeviceStatusToReadCache(String deviceId, boolean online) {
        putDevStatQckRd(deviceId, online);
    }
    
    private Boolean getDeviceStatusFromReadCache(String deviceId) {
        return getDevStatQckRd(deviceId);
    }
    
    public void put(String key, String key2, Long val) {
        cache.putHashValue(key, key2, val);
    }
    
    public void predict(String deviceId, String onlineBroker, long onlineTime) {
        Map<String, Object> brokersMap = getBroVerMap();
        for(Entry<String, Object> entry : brokersMap.entrySet()) {
            if(!entry.getKey().equals(onlineBroker)) {
                String broker = entry.getKey();
                Long version = (Long)entry.getValue();
                Long status = getBroVerDevStat(broker, version, deviceId);
                if(status != null && status > 0 && status < onlineTime) {
                    deleteBroVerDevStat(broker, version, deviceId);
                    logger.info(String.format("offline by kicked: device = %s on broker = %s, version = %s , onlineTimestamp = %s[%s]"
                            , deviceId, onlineBroker, version, status, new Date(status)));
                }
            }
        }
    }
    
    public void predict(String deviceId, String onlineBroker, long onlineTime, RedisOperations opt) {
        Map<String, Object> brokersMap = getBroVerMap();
        for(Entry<String, Object> entry : brokersMap.entrySet()) {
            if(!entry.getKey().equals(onlineBroker)) {
                String broker = entry.getKey();
                Long version = (Long)entry.getValue();
                Long status = getBroVerDevStat(broker, version, deviceId);
                if(status != null && status > 0 && status < onlineTime) {
                    deleteBroVerDevStat(broker, version, deviceId, opt);
                    logger.info(String.format("offline by kicked: device = %s on broker = %s, version = %s , onlineTimestamp = %s[%s]"
                            , deviceId, onlineBroker, version, status, new Date(status)));
                }
            }
        }
    }
    
    public Map<String, Object> getBrokers() {
        return getBroVerMap();
    }
    
    public void addBroker(String broker, Long version) {
        putBroVer(broker, version);
    }
    
    public void addDevice(String broker, Long version, String deviceId) {
        putBroVerDevStat(broker, version, deviceId, System.currentTimeMillis());
    }

    public static void main(String[] args) {
        String keys = "device_status_mgr_bvdsp_/172.17.0.1:5701_1_phihome_admin_1526029583252188, device_status_mgr_bvdsp_test1_5_phihome_admin_1526029583252188, device_status_mgr_bvdsp_test2_1_phihome_admin_1526029583252188, device_status_mgr_bvdsp_test3_1_phihome_admin_1526029583252188";
        String[] keyArr = keys.split(",");
        for(String key : keyArr) {
            System.out.println(ClusterSlotHashUtil.calculateSlot(key));
        }
        Map<Integer, List<String>> map = new HashMap<>();
        StringRedisSerializer f = new StringRedisSerializer();
        for(int i = 0 ; i < 20000; i++) {
            String key = "device_status_mgr_bvdsp_/172.17.0.1:5701_1_" + i;
            int slot = ClusterSlotHashUtil.calculateSlot(f.serialize(key));
            List<String> keyList = map.get(slot);
            if(keyList == null) {
                keyList = new ArrayList<>();
                map.put(slot, keyList);
            }
            keyList.add(key);
        }
        for(Entry<Integer, List<String>> entry : map.entrySet()) {
            if(entry.getValue() != null && entry.getValue().size() >1 ) {
                System.out.println(String.format("slot : %s, keys : %s", entry.getKey(), entry.getValue()));
            }
        }
        
        JedisCluster j = new JedisCluster(new HostAndPort("172.31.44.234", 7003));
        long start = System.currentTimeMillis();
        byte[] key1 = f.serialize("device_status_mgr_bvdsp_/172.17.0.1:5701_1_1458");
        byte[] key2 = f.serialize("device_status_mgr_bvdsp_/172.17.0.1:5701_1_3327");
        System.out.println(j.mget(key1, key2));
        System.out.println(System.currentTimeMillis() - start);
        try {
            j.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void reset() {
        Set<String> keys = redisTemplate.keys(Contants.APP_NAME + "*");
        mDelByConcurrency(keys);
    }
    
    public void reset(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        mDelByConcurrency(keys);
    }
    
}
