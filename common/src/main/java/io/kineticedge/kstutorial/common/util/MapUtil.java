package io.kineticedge.kstutorial.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

public final class MapUtil {

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

  private static final ObjectMapper objectMapper = JsonUtil.objectMapper();

  private MapUtil() {
  }

  public static <T> Map<String, Object> convert(final T object) {
    return objectMapper.convertValue(object, MAP_TYPE_REFERENCE);
  }

  public static Map<String, Object> readIntoMap(final String json) {
    try {
      return JsonUtil.objectMapper().readValue(json, MAP_TYPE_REFERENCE);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> removeKeyRecursively(Map<String, Object> map, String keyToRemove) {

    final Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Object> entry = iterator.next();

      // Remove the key if it matches
      if (entry.getKey().equals(keyToRemove)) {
        iterator.remove();
        continue;
      }

      // If the value is a Map, recurse
      if (entry.getValue() instanceof Map) {
        removeKeyRecursively((Map<String, Object>) entry.getValue(), keyToRemove);
      }

      // If the value is a List, iterate over it and apply recursion for Maps
      if (entry.getValue() instanceof Iterable) {
        for (Object item : (Iterable<?>) entry.getValue()) {
          if (item instanceof Map) {
            removeKeyRecursively((Map<String, Object>) item, keyToRemove);
          }
        }
      }
    }

    return map;
  }

}
