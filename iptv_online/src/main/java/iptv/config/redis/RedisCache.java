package iptv.config.redis;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RedisCache {

	/**连接数据库名称*/
	private String CONNECTION_INFO = "";
	public final static String CAHCENAME="cache";//缓存名
	public final static int CAHCETIME=60;//默认缓存时间

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate_read;

	public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void setRedisTemplate_read(
			RedisTemplate<String, String> redisTemplate_read) {
		this.redisTemplate_read = redisTemplate_read;
	}

	public <T> boolean putCache(String key, T obj) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serialize(obj);
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(bkey, bvalue);
				return true;
			}
		});
		return result;
	}
	
	public <T> void putCacheWithExpireTime(String key, T obj, final long expireTime) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serialize(obj);
		redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				connection.setEx(bkey, expireTime, bvalue);
				return true;
			}
		});
	}

	public <T> boolean putListCache(String key, List<T> objList) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serializeList(objList);
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(bkey, bvalue);
				return true;
			}
		});
		return result;
	}
	
	public <T> Long lpushCache(String key, T obj) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serialize(obj);
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.lPush(bkey, bvalue);
			}
		});
		return result;
	}
	public <T> Long rpushCache(String key, T obj) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serialize(obj);
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.rPush(bkey, bvalue);
			}
		});
		return result;
	}

	public <T> boolean putListCacheWithExpireTime(String key, List<T> objList, final long expireTime) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serializeList(objList);
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				connection.setEx(bkey, expireTime, bvalue);
				return true;
			}
		});
		return result;
	}
	
	public <T> Long getSequence(final String key) {
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.incr(key.getBytes());
			}
		});
		return result;
	}

	public <T> T rpopCache(final String key, Class<T> targetClass) {
		byte[] result = redisTemplate.execute(new RedisCallback<byte[]>() {
			@Override
			public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.rPop(key.getBytes());
			}
		});
		if (result == null) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserialize(result, targetClass);
	}
	
	public <T> T brpopCache(final String key, Class<T> targetClass) {
		List<byte[]> result = redisTemplate.execute(new RedisCallback<List<byte[]>>() {
			@Override
			public List<byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.bRPop(600, key.getBytes());
			}
		});
		if (result == null || result.size() < 2) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserialize(result.get(1), targetClass);
	}
	
	public <T> T getCache(final String key, Class<T> targetClass) {
		byte[] result = null;
		try {
			result = redisTemplate_read.execute(new RedisCallback<byte[]>() {
				@Override
				public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.get(key.getBytes());
				}
			});
		} catch (DataAccessException e) {
			result = redisTemplate.execute(new RedisCallback<byte[]>() {
				@Override
				public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.get(key.getBytes());
				}
			});
		}
		
		if (result == null || result.length == 0) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserialize(result, targetClass);
	}
	
	/**
	 * 获取一个key的剩余有效时间
	 * @param key
	 * @return
	 */
	public Long getKeyExpireTime(final String key) {
		final byte[] bkey = key.getBytes();
		Long result;
		try {
			result = redisTemplate_read.execute(new RedisCallback<Long>(){
				@Override
				public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
					return redisConnection.ttl(bkey);
				}
			});
		} catch (Exception e) {
			result = redisTemplate.execute(new RedisCallback<Long>(){
				@Override
				public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
					return redisConnection.ttl(bkey);
				}
			});
		}
		return result;
	}
	
	/**
	 * 如果key不存在，将指定的value写到key里面去，返回true,如果key已经存在，不设置value值，返回false
	 * @param key
	 * @param value
	 * @return
	 */
	public Boolean  setnx(String key,String value){
		final byte[] bkey = key.getBytes();
		byte[] bvalue=value.getBytes();
		boolean result=false;
		result = redisTemplate.execute(new RedisCallback<Boolean>(){
			@Override
			public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.setNX(bkey,bvalue);
			}
		});
		return result;
	}

	public <T> List<T> getListCache(final String key, Class<T> targetClass) {
		byte[] result = null;
		try {
			result = redisTemplate_read.execute(new RedisCallback<byte[]>() {
				@Override
				public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.get(key.getBytes());
				}
			});
		} catch (Exception e) {
			result = redisTemplate.execute(new RedisCallback<byte[]>() {
				@Override
				public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.get(key.getBytes());
				}
			});
		}
		if (result == null) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserializeList(result, targetClass);
	}
	
	public <T> List<T> getListCache(final List<String> keys, Class<T> targetClass) {
		if (CollectionUtils.isEmpty(keys)) return null;
		
		List<byte[]> result = null;
		
		try {
			result = redisTemplate_read.execute(new RedisCallback<List<byte[]>>() {
				@Override
				public List<byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
					byte[][] keyArr = new byte[keys.size()][];
					int i = 0;
					for (String key : keys) {
						keyArr[i++] = key.getBytes();
					}
					return connection.mGet(keyArr);
				}
			});
		} catch (Exception e) {
			result = redisTemplate.execute(new RedisCallback<List<byte[]>>() {
				@Override
				public List<byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
					byte[][] keyArr = new byte[keys.size()][];
					int i = 0;
					for (String key : keys) {
						keyArr[i++] = key.getBytes();
					}
					return connection.mGet(keyArr);
				}
			});
		}
		
		if (result == null) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserializeList(result, targetClass);
	}
	
	/**
	 * 精确删除key
	 * @param keys
	 * @return
	 */
	public long del(final String... keys) {
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				long result = 0;
                for (int i = 0; i < keys.length; i++) {
                    result += connection.del(keys[i].getBytes());
                }
                return result;
			}
		});
		return result;        
    }
	
	public Map<String, String> hgetall(final String key) {
		Map<byte[], byte[]> result = null;
		
		try {
			result = redisTemplate_read.execute(new RedisCallback<Map<byte[], byte[]>>() {
				@Override
				public Map<byte[], byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.hGetAll(key.getBytes());
				}
			});
		} catch (Exception e) {
			result = redisTemplate.execute(new RedisCallback<Map<byte[], byte[]>>() {
				@Override
				public Map<byte[], byte[]> doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.hGetAll(key.getBytes());
				}
			});
		}
		
		return ProtoStuffSerializerUtil.deserializeList(result);
	}
	
	public Map<String,String> hScan(final String key, Integer count) {
		Cursor<Map.Entry<byte[],byte[]>> result = null;
		ScanOptions options = ScanOptions.scanOptions().count(count).build();
		try {
			result = redisTemplate_read.execute(new RedisCallback<Cursor<Map.Entry<byte[],byte[]>>>() {
				@Override
				public Cursor<Map.Entry<byte[],byte[]>> doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.hScan(key.getBytes(),options);
				}
			});
		} catch (Exception e) {
			result = redisTemplate.execute(new RedisCallback<Cursor<Map.Entry<byte[],byte[]>>>() {
				@Override
				public Cursor<Map.Entry<byte[],byte[]>> doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.hScan(key.getBytes(),options);
				}
			});
		}
		
		return ProtoStuffSerializerUtil.deserializeList(result);
	}
	
	public boolean hMSet(final String key, final Map<String, String> paramMap) {
		Boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hMSet(key.getBytes(), ProtoStuffSerializerUtil.serializeMap(paramMap));
				return true;
			}
		});
		
		return result;
	}

	/**
	 * 精确删除key
	 * 
	 * @param key
	 */
	public void deleteCache(String key) {
		redisTemplate.delete(key);
	}
	
	/**
	 * 模糊删除key
	 * 
	 * @param pattern
	 */
	public void deleteCacheWithPattern(String pattern) {
		Set<String> keys = redisTemplate.keys(pattern);
		redisTemplate.delete(keys);
	}

	/**
	 * 清空所有缓存
	 */
	public void clearCache() {
		deleteCacheWithPattern(RedisCache.CAHCENAME+"|*");
	}

	public String getStringCache(String key) {
		return this.getCache(key,String.class);
	}
/*
	public <T> Long lpushCache(String key, T obj) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = ProtoStuffSerializerUtil.serialize(obj);
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.lPush(bkey, bvalue);
			}
		});
		return result;
	}*/
	public <T> Long sAdd(String key, final T... obj){
		final byte[] bkey = key.getBytes();
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				long result = 0;
				for (T t : obj) {
					redisConnection.sAdd(bkey,ProtoStuffSerializerUtil.serialize(t));
					result ++;
				}
				return  result;
			}
		});
		return result;
	}

	public <T> Long sRem(String key, final T... obj){
		final byte[] bkey = key.getBytes();
		Long result = redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				long result = 0;
				for (T t : obj) {
					redisConnection.sRem(bkey,ProtoStuffSerializerUtil.serialize(t));
					result ++;
				}
				return  result;
			}
		});
		return result;
	}
	public <T> boolean sIsMember(String key, T obj){
		final byte[] setKey = key.getBytes();
		final byte[] targetValue = ProtoStuffSerializerUtil.serialize(obj);
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.sIsMember(setKey,targetValue);
			}
		});
		return result;
	}

	/*public <T> T execute(final String nosql,Class<T> targetClass){
		final byte[] bkey = nosql.getBytes();
		Object result = redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.execute(nosql);
			}
		});
		return (T)result;
	}*/

	public <T> List<T> sort(final String key, final SortParameters sortParameters, Class<T> targetClass){
		final byte[] bkey = key.getBytes();
		List<byte[]> list = redisTemplate.execute(new RedisCallback<List<byte[]>>() {
			@Override
			public List<byte[]> doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.sort(bkey,sortParameters);
			}
		});
		if(null ==  list || list.isEmpty()) return null;

		return ProtoStuffSerializerUtil.deserializeListContainsNull(list, targetClass);
	}

	public <T>Set<T> sMembers( String key,Class<T> targetClass){
		final byte[] bkey = key.getBytes();
		Set<byte[]> set = redisTemplate.execute(new RedisCallback<Set<byte[]>>(){

			@Override
			public Set<byte[]> doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.sMembers(bkey);
			}
		});
		return ProtoStuffSerializerUtil.deserializeSet(set,targetClass);
	}

	public <T> boolean zAdd(String key,final double score,T obj){
		final byte[] bkey = key.getBytes();
		final byte[] targetValue = ProtoStuffSerializerUtil.serialize(obj);
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>(){
			@Override
			public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {

				return redisConnection.zAdd(bkey,score,targetValue);
			}
		});
		return result;
	}


	/****
	 * min = 0;max =-1 表示获取全部的成员
	 * @param key
	 * @param min
	 * @param max
	 * @param targetClass
	 * @param <T>
	 * @return
	 */
	public  <T>Set<T>  zRange(String key,final long min,final long max,Class<T> targetClass){
		final byte[] bkey = key.getBytes();
		Set<byte[]> result = redisTemplate_read.execute(new RedisCallback<Set<byte[]>>(){
			@Override
			public Set<byte[]> doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.zRange(bkey,min,max);
			}
		});
		return ProtoStuffSerializerUtil.deserializeSet(result,targetClass);
	}
	public <T>Long  zRem(String key,T member){
		final byte[] bkey = key.getBytes();
		final byte[] bmember = ProtoStuffSerializerUtil.serialize(member);
		Long result = redisTemplate.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.zRem(bkey,bmember);
			}
		});
		return result;
	}

	public Long  zRemRangeByScore(String key,final double min,final double max){
		final byte[] bkey = key.getBytes();
		Long result = redisTemplate.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.zRemRangeByScore(bkey,min,max);
			}
		});
		return result;
	}
	public <T>Set<T>  zRangeByScore(String key,final double min,final double max,Class<T> targetClass){
		final byte[] bkey = key.getBytes();
		Set<byte[]> result = redisTemplate_read.execute(new RedisCallback<Set<byte[]>>(){
			@Override
			public Set<byte[]> doInRedis(RedisConnection redisConnection) throws DataAccessException {

				return redisConnection.zRangeByScore(bkey,min,max);
			}
		});
		return ProtoStuffSerializerUtil.deserializeSet(result,targetClass);
	}

	/***
	 * 判断member是否在有序集合key里面
	 *
	 * @param key
	 * @param member
	 * @return 返回null就是不存在
	 */
	public <T>Long  zRank(String key,T member){
		final byte[] bkey = key.getBytes();
		final byte[] bmember = ProtoStuffSerializerUtil.serialize(member);
		Long result = redisTemplate_read.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.zRank(bkey,bmember);
			}
		});
		return result;
	}

	public Long  incr(String key){
		final byte[] bkey = key.getBytes();
		Long result = redisTemplate.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.incr(bkey);
			}
		});
		return result;
	}

	public Long  decr(String key){
		final byte[] bkey = key.getBytes();
		Long result = redisTemplate.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.decr(bkey);
			}
		});
		return result;
	}

	public String getConnectionInfo() {
		return CONNECTION_INFO;
	}

	public  void setConnectionInfo(String connectionInfo) {
		CONNECTION_INFO = connectionInfo;
	}

	public Long lLen(String key){
		final byte[] bkey = key.getBytes();
		return  redisTemplate_read.execute(new RedisCallback<Long>(){
			@Override
			public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.lLen(bkey);
			}
		});
	}


    /***
     *  Redis Ltrim 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除。
     *  下标 0 表示列表的第一个元素，以 1 表示列表的第二个元素，以此类推。
     *  你也可以使用负数下标，以 -1 表示列表的最后一个元素， -2 表示列表的倒数第二个元素，以此类推
     * @param key
     * @param start
     * @param stop
     * @return
     */
	public boolean lTrim(String key,final long start ,final long stop){
		final byte[] bkey = key.getBytes();
		return  redisTemplate.execute(new RedisCallback<Boolean>(){
			@Override
			public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
				 redisConnection.lTrim(bkey,start,stop);
				 return  true;
			}
		});
	}

	public boolean exists(String key){
		final byte[] bkey = key.getBytes();
		return  redisTemplate_read.execute(new RedisCallback<Boolean>(){
			@Override
			public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.exists(bkey);
			}
		});
	}
	
	
	public boolean expire(String key,final long expireTime){
		final byte[] bkey = key.getBytes();
		boolean result = redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.expire(bkey, expireTime);
			}
		});
		return result; 
	}

	/***
	 * 判断value是否在redis为key的值中
	 * redis存的数据是list
	 * [v1,v2,v3]
	 * @param key
	 * @param value
	 */
	public   boolean isInCacheList(String key,String value){
		if(StringUtils.isEmpty(value)){
			return false;
		}
		String listStr = this.getCache(key, String.class);
		List<String> list = JSON.parseArray(listStr, String.class);
		if(null == list || list.isEmpty()){
			return false;
		}
		for(String str : list ){
			if(value.equals(str)){
				return true;
			}
		}
		return false;
	}

	/***
	 * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)。当 key 存在但不是字符串类型时，返回一个错误。
	 * @param key
	 * @param value
	 * @param targetClass
	 * @param <T>
	 * @return 返回给定 key 的旧值。 当 key 没有旧值时，也即是， key 不存在时，返回 nil 。
	 */
	public <T> T getSet(final String key, final String value, Class<T> targetClass) {
		byte[] result = null;
		result = redisTemplate.execute(new RedisCallback<byte[]>() {
			@Override
			public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.getSet(key.getBytes(),value.getBytes());
			}
		});
		if (result == null || result.length == 0) {
			return null;
		}
		return ProtoStuffSerializerUtil.deserialize(result, targetClass);
	}
	
	/**
	 * 从有序结合里面取出指定数量指定score范围的数据
	 * @param key
	 * @param score
	 * @param nums
	 * @param targetClass
	 * @return
	 */
	public <T>Set<T>  zRevRangeByScore(String key, Range score, Limit nums, Class<T> targetClass){
		final byte[] bkey = key.getBytes();
		Set<byte[]> result = redisTemplate_read.execute(new RedisCallback<Set<byte[]>>(){
			@Override
			public Set<byte[]> doInRedis(RedisConnection redisConnection) throws DataAccessException {
				return redisConnection.zRevRangeByScore(bkey,score,nums);
			}
		});
		return ProtoStuffSerializerUtil.deserializeSet(result,targetClass);
	}
	
	/**
	 * 获取锁并同时设置锁
	 * @param key
	 * @param value
	 * @param exptime
	 * @return
	 */
	public boolean setnxWithExptime( String key, String value, final long exptime) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = value.getBytes();		
	    Boolean b = (Boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
	        @Override
	        public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
	            Object obj = connection.execute("set", bkey, 
	            		            bvalue,
	                                SafeEncoder.encode("NX"),
	                                SafeEncoder.encode("EX"),
	                                Protocol.toByteArray(exptime));
	            return obj != null;
	        }
	    });
	    return b;
	}
	
	   /**
     * @param key
     * @return 获取自增id大小
     * @Title: generate
     * @Description: Atomically increments by one the current value.
     */
    public long generate(String key) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        return counter.incrementAndGet();
    }
    


    /**
     * @param key
     * @param increment
     * @return 设置自增id大小
     * @Title: generate
     * @Description: Atomically adds the given value to the current value.
     */
    public long generate(String key, int increment) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        return counter.addAndGet(increment);
    }
	
}