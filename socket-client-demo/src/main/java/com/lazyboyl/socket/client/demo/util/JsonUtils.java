package com.lazyboyl.socket.client.demo.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linzf
 * @since 2019-04-25
 * 类描述：json转换通用工具类
 */
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MapType map = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
    private static final MapType stringMap = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class);
    private static final JavaType listMap = mapper.getTypeFactory().constructParametricType(ArrayList.class, map);

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
    }

    public static <T> T map2object(Map map,Class<T> beanType) {
        try {
            return mapper.readValue( mapper.writeValueAsString(map), beanType);
        } catch (Exception e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }


    public static String objToJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static <T> T jsonToPojo(String json, Class<T> beanType) {
        try {
            return mapper.readValue(json, beanType);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static <T> T objToPojo(Object obj, Class<T> valueType) {
        try {
            return mapper.readValue(mapper.writeValueAsString(obj), valueType);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static <T> ArrayList<T> objToList(Object obj, Class<T> valueType) {
        JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
        try {
            return mapper.readValue(mapper.writeValueAsString(obj), listType);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static <T> ArrayList<T> jsonToList(String json, Class<T> valueType) {
        JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
        try {
            return mapper.readValue(json, listType);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static HashMap<String, Object> objToMap(Object obj) {
        try {
            return mapper.readValue(mapper.writeValueAsString(obj), map);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static HashMap<String, Object> jsonToMap(String json) {
        try {
            return mapper.readValue(json, map);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static HashMap<String, String> jsonToStringMap(String json) {
        try {
            return mapper.readValue(json, stringMap);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static ArrayList<HashMap<String, Object>> objToMapList(Object obj) {
        try {
            return mapper.readValue(mapper.writeValueAsString(obj), stringMap);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }

    public static ArrayList<HashMap<String, Object>> jsonToMapList(String json) {
        try {
            return mapper.readValue(json, stringMap);
        } catch (IOException e) {
            throw new RuntimeException("Jackson处理出现错误", e);
        }
    }
}
