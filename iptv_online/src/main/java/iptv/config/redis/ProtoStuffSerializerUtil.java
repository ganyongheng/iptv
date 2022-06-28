package iptv.config.redis;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 序列话工具
 */
public class ProtoStuffSerializerUtil {
	private static Logger log = LoggerFactory.getLogger(ProtoStuffSerializerUtil.class);

	/**
	 * 序列化对象
	 * @param obj
	 * @return
	 */
	public static <T> byte[] serialize(T obj) {
		if (obj == null) {
			throw new RuntimeException("序列化对象(" + obj + ")!");
		}
		if ((obj instanceof String)
				|| (obj instanceof Long)
				|| (obj instanceof Integer)) {
			try {
				return (obj + "").getBytes("UTF-8");
			} catch (Exception e) {
				throw new RuntimeException("序列化(" + obj.getClass() + ")对象(" + obj + ")发生异常!", e);
			}			
		}
		@SuppressWarnings("unchecked")
		Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(obj.getClass());
		LinkedBuffer buffer = LinkedBuffer.allocate(1024 * 1024);
		byte[] protostuff = null;
		try {
			protostuff = ProtobufIOUtil.toByteArray(obj, schema, buffer);
		} catch (Exception e) {
			throw new RuntimeException("序列化(" + obj.getClass() + ")对象(" + obj + ")发生异常!", e);
		} finally {
			buffer.clear();
		}
		return protostuff;
	}

	/**
	 * 反序列化对象
	 * @param paramArrayOfByte
	 * @param targetClass
	 * @return
	 */
	public static <T> T deserialize(byte[] paramArrayOfByte, Class<T> targetClass) {
		if (paramArrayOfByte == null || paramArrayOfByte.length == 0) {
			throw new RuntimeException("反序列化对象发生异常,byte序列为空!");
		}
		
		if ((targetClass.getSimpleName().equals("String"))
				|| (targetClass.getSimpleName().equals("Long"))
				|| (targetClass.getSimpleName().equals("Integer"))) {
			try {
				return (T) new String(paramArrayOfByte, "UTF-8");
			} catch (Exception e) {
				throw new RuntimeException("反序列化过程中依据类型创建对象失败!", e);
			}			
		}
		
		T instance = null;

		try {
			instance = targetClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("反序列化过程中依据类型创建对象失败!", e);
		}
		Schema<T> schema = RuntimeSchema.getSchema(targetClass);
		ProtobufIOUtil.mergeFrom(paramArrayOfByte, instance, schema);
		
		return instance;
	}

	/**
	 * 序列化列表
	 * @param objList
	 * @return
	 */
	public static <T> byte[] serializeList(List<T> objList) {
		if (objList == null || objList.isEmpty()) {
			throw new RuntimeException("序列化对象列表(" + objList + ")参数异常!");
		}
		@SuppressWarnings("unchecked")
		Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(objList.get(0).getClass());
		LinkedBuffer buffer = LinkedBuffer.allocate(1024 * 1024);
		byte[] protostuff = null;
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			ProtobufIOUtil.writeListTo(bos, objList, schema, buffer);
			protostuff = bos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("序列化对象列表(" + objList + ")发生异常!", e);
		} finally {
			buffer.clear();
			try {
				if (bos != null) {
					bos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return protostuff;
	}

	/**
	 * 反序列化列表
	 * @param paramArrayOfByte
	 * @param targetClass
	 * @return
	 */
	public static <T> List<T> deserializeList(byte[] paramArrayOfByte, Class<T> targetClass) {
		if (paramArrayOfByte == null || paramArrayOfByte.length == 0) {
			throw new RuntimeException("反序列化对象发生异常,byte序列为空!");
		}

		Schema<T> schema = RuntimeSchema.getSchema(targetClass);
		List<T> result = null;
		try {
			result = ProtobufIOUtil.parseListFrom(new ByteArrayInputStream(paramArrayOfByte), schema);
		} catch (IOException e) {
			throw new RuntimeException("反序列化对象列表发生异常!", e);
		}
		return result;
	}
	
	public static <T> List<T> deserializeList(List<byte[]> paramArrayOfByte, Class<T> targetClass) {
		if (paramArrayOfByte == null || paramArrayOfByte.size() == 0) {
			throw new RuntimeException("反序列化对象发生异常,byte序列为空!");
		}
		
		List<T> result = new ArrayList();
		for (byte[] param : paramArrayOfByte) {
			try {
				if (param == null) continue;
				result.add(deserialize(param, targetClass));
			} catch (Exception e) {
				log.error("反序列化对象发生异常", e);
			}
		}

		return result;
	}
	
	public static Map<byte[], byte[]> serializeMap(Map<String, String> paramArrayOfByte) {
		Map<byte[], byte[]> result = new HashMap();
		
		try {
			String bvalue = null;
			for (String bkey : paramArrayOfByte.keySet()) {
				bvalue = paramArrayOfByte.get(bkey);
				if (bvalue == null) continue;
				
				result.put(bkey.getBytes("UTF-8"), bvalue.getBytes("UTF-8"));
			}
		} catch (Exception e) {
			throw new RuntimeException("反序列化hash对象列表发生异常!", e);
		}
		
		return result;
	}
	
	public static Map<String, String> deserializeList(Map<byte[], byte[]> paramArrayOfByte) {
		Map<String, String> result = new HashMap();
		
		try {
			byte[] bvalue = null;
			for (byte[] bkey : paramArrayOfByte.keySet()) {
				bvalue = paramArrayOfByte.get(bkey);
				if (bvalue == null) continue;
				
				result.put(new String(bkey, "UTF-8"), new String(bvalue, "UTF-8"));
			}
		} catch (Exception e) {
			throw new RuntimeException("反序列化hash对象列表发生异常!", e);
		}
		
		return result;
	}
	
	
	public static Map<String, String> deserializeList(Cursor<Map.Entry<byte[],byte[]>> paramArrayOfByte) {
		Map<String, String> result = new HashMap();
		
		try {
			while (paramArrayOfByte.hasNext()) {
				Map.Entry<byte[],byte[]> entry=paramArrayOfByte.next();			
				result.put(new String(entry.getKey(), "UTF-8"), new String(entry.getValue(), "UTF-8"));
			}
		} catch (Exception e) {
			throw new RuntimeException("反序列化hash对象列表发生异常!", e);
		}
		
		return result;
	}

	public static <T> Set<T> deserializeSet(Set<byte[]> paramArrayOfByte,Class<T> targetClass) {
		Set<T> set = new HashSet<>();
		try {
			for (byte[] bvalue : paramArrayOfByte) {
				set.add(deserialize(bvalue,targetClass));
			}
		} catch (Exception e) {
			throw new RuntimeException("反序列化hash对象列表发生异常!", e);
		}
		return set;
	}


	public static <T> List<T> deserializeListContainsNull(List<byte[]> paramArrayOfByte, Class<T> targetClass) {
		if (paramArrayOfByte == null || paramArrayOfByte.size() == 0) {
			throw new RuntimeException("反序列化对象发生异常,byte序列为空!");
		}

		List<T> result = new ArrayList();
		for (byte[] param : paramArrayOfByte) {
			try {
				if (param == null) {
					result.add(null);
				} else {
					result.add(deserialize(param, targetClass));
				}

			} catch (Exception e) {
				log.error("反序列化对象发生异常", e);
			}
		}
		return result;
	}

}
