package io.kineticedge.kstutorial.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.TimeZone;

public final class XmlUtil {

    private static final ObjectMapper OBJECT_MAPPER =
            new XmlMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setTimeZone(TimeZone.getDefault())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .registerModule(new JavaTimeModule())
                    .registerModule(new SimpleModule("instant-module", new Version(1, 0, 0, null, "", ""))
                            .addSerializer(Instant.class, new InstantSerializer())
                            .addDeserializer(Instant.class, new InstantDeserializer())
                    )
            ;

  private XmlUtil() {
  }

  /**
   * ObjectMapper is not immutable, but we are "trusting" that it is treated as being immutable for anyone that uses this.
   */
  public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

}
