package io.kineticedge.kstutorial.common.config;

import com.beust.jcommander.IStringConverter;
import io.kineticedge.kstutorial.common.main.TopologyBuilder;

public class TopologyConverter implements IStringConverter<TopologyBuilder> {

  @Override
  public TopologyBuilder convert(String className) {
    try {
      final Class<?> clazz = Class.forName(className);
      if (!TopologyBuilder.class.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException("Class " + className + " does not implement TopologyBuilder interface.");
      }
      return (TopologyBuilder) clazz.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("class not found: " + className, e);
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException("error while instantiating class: " + className, e);
    }
  }
}