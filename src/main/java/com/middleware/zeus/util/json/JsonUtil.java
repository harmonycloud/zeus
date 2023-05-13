package com.middleware.zeus.util.json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
	private static final ObjectMapper mapper = new ObjectMapper();

	private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
	
	/**
	 * 将对象转换成json字符串
	 * @param object
	 * @return
	 */
	public static String objectToJson(Object object){
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			logger.warn("objectToJson失败", e);
		}
		return null;
	}
	
	/**
	 * 将json结果集转化为对象
	 * @param json
	 * @param beanType
	 * @return
	 */
	public static <T> T jsonToPojo(String json,Class<T> beanType){
		if(json == null || "".equals(json)){
			return null;
		}
		try {
			T t = mapper.readValue(json,beanType);
			return t;
		} catch (JsonParseException e) {
			logger.warn("jsonToPojo失败", e);
		} catch (JsonMappingException e) {
			logger.warn("jsonToPojo失败", e);
		} catch (IOException e) {
			logger.warn("jsonToPojo失败", e);
		}
		return null;
	}
	
	/**
	 * 将json数据转换成pojo对象list
	 * @param jsonData
	 * @param beanType
	 * @return
	 */
	 public static <T>List<T> jsonToList(String jsonData, Class<T> beanType) {
	    	JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
	    	try {
	    		List<T> list = mapper.readValue(jsonData, javaType);
	    		return list;
			} catch (Exception e) {
				logger.warn("jsonToList失败", e);
			}
	    	return null;
	    }
	 /**
	  * 将json数据转换成map
	  * @param json
	  * @return
	  */
	
	public static Map<String,Object> jsonToMap(String json){
		 Map<String,Object> map = null;
		try {
			map = mapper.readValue(json, new TypeReference<Map<String,Object>>() {});
		} catch (JsonParseException e) {
			logger.warn("jsonToMap失败", e);
		} catch (JsonMappingException e) {
			logger.warn("jsonToMap失败", e);
		} catch (IOException e) {
			logger.warn("jsonToMap失败", e);
		}
		return map;
	 }
	
	/**
	 * 将json数据转换成list(map<String,object>)
	 * @param json
	 * @return
	 */
	public static List<Map<String, Object>> JsonToMapList(String json) {
        List<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
        if (json != null) {
            try {
                retList = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            } catch (IOException e) {
				logger.warn("jsonToMapList失败", e);
            }
        }
        return retList;
    }

	public static String convertToJson(Object value) {
		if (mapper.canSerialize(value.getClass())) {
			try {
				return mapper.writeValueAsString(value);
			} catch (IOException e) {
				logger.warn("Error while serializing " + value + " to JSON", e);
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("Value of type " + value.getClass().getName() +
					" can not be serialized to JSON.");
		}
	}

	/**
	 * 将json数据转换成pojo对象list 且过滤null
	 * @param jsonData
	 * @param beanType
	 * @return
	 */
	public static <T>List<T> jsonToListNonNull(String jsonData, Class<T> beanType) {
		mapper.setSerializationInclusion(Include.NON_NULL);
		JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
		try {
			List<T> list = mapper.readValue(jsonData, javaType);
			return list;
		} catch (Exception e) {
			logger.warn("jsonToList失败", e);
		}
		return null;
	}

	public static String convertToJsonNonNull(Object value) {
		mapper.setSerializationInclusion(Include.NON_NULL);
		if (mapper.canSerialize(value.getClass())) {
			try {
				return mapper.writeValueAsString(value);
			} catch (IOException e) {
				logger.warn("Error while serializing " + value + " to JSON", e);
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("Value of type " + value.getClass().getName() +
					" can not be serialized to JSON.");
		}
	}

	public static Map<String, Object> convertJsonToMap(String json) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		if (json != null) {
			try {
				retMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				logger.warn("Error while reading Java Map from JSON response: " + json, e);
			}
		}
		return retMap;
	}

	/**
	 * 将json字符串转成yaml字符串
	 * @param jsonString
	 * @return
	 * @throws IOException
	 */
	public static String toYaml(String jsonString) throws IOException {
		// parse JSON
		JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
		// save it as YAML
		String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
		return jsonAsYaml;
	}

	/**
	 * 是否json对象
	 *
	 * @param jsonString json字符串
	 * @return
	 */
	public static boolean isJsonObject(String jsonString) {
		return jsonString.startsWith("{") && jsonString.endsWith("}");
	}

	/**
	 * 是否json数组
	 *
	 * @param jsonString json字符串
	 * @return
	 */
	public static boolean isJsonArray(String jsonString) {
		return jsonString.startsWith("[") && jsonString.endsWith("]");
	}
}
