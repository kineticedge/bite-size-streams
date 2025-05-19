package io.kineticedge.kstutorial.common.config;

import org.apache.commons.lang3.BooleanUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RecordUtil {

  private static final Pattern PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

  private static final String PREFIX = "";

//  public static void x() {
//    JCommander jCommander = JCommander.newBuilder()
//            .addCommand()
//            .addObject(options)
//            .build();
//
//  }

  public static <T> T createRecord(Class<T> recordClass) {

    if (!recordClass.isRecord()) {
      throw new IllegalArgumentException("Provided class is not a record");
    }

    try {

      final Constructor<T> constructor = recordClass.getDeclaredConstructor(
              Stream.of(recordClass.getRecordComponents())
                      .map(RecordComponent::getType)
                      .toArray(Class[]::new));

      final Object[] args = Stream.of(recordClass.getRecordComponents())
              .map(RecordUtil::convert)
              .toArray(Object[]::new);

      return constructor.newInstance(args);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to create record instance", e);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Enum<?> create(final Class<?> type, final String value) {
    return Enum.valueOf((Class<Enum>) type, value);
  }

  private static String getEnvironmentVariable(final String string) {
    return PREFIX + PATTERN.matcher(string).replaceAll(match -> "_" + match.group()).toUpperCase();
  }

  private static Object convert(RecordComponent component) {

    final String environment = getEnvironmentVariable(component.getName());
    final String value = System.getenv(environment);

    if (value == null) {
      return null;
    }

    final Class<?> type = component.getType();

    if (String.class.equals(type)) {
      return value;
    } else if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
      return BooleanUtils.toBoolean(value);
    } else if (Integer.TYPE.equals(type) || Integer.class.equals(type)) {
      return Integer.parseInt(value);
    } else if (Long.TYPE.equals(type) || Long.class.equals(type)) {
      return Long.parseLong(value);
    } else if (Enum.class.isAssignableFrom(type)) {
      return create(type, value);
    } else {
      throw new IllegalArgumentException("unsupported type " + type);
    }
  }

}