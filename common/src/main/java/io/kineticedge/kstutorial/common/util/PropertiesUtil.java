package io.kineticedge.kstutorial.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


public final class PropertiesUtil {

    private static final Logger log = LoggerFactory.getLogger(PropertiesUtil.class);

    private PropertiesUtil() {
    }

    /**
     * Load a property file into a map where the key is a string, and the value is of type object.
     *
     * If file does not exist (or is not a file) will gracefully ignore, but an exception reading the
     * file will throw a runtime-exception.
     *
     */
    public static Map<String, Object> load(final String propertyFile) {

        try {
            final File file = new File(propertyFile);

            log.debug("loading properties from propertyFile={}, absolutePath={}", propertyFile, new File (propertyFile).getAbsolutePath());

            if (file.exists() && file.isFile()) {
                final Properties properties = new Properties();
                try (final FileInputStream is = new FileInputStream(file)) {
                    properties.load(is);
                }
                return new HashMap<>(properties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
            } else {
                if (file.isDirectory()) {
                    log.warn("propertyFile={} is a directory, ignored.", propertyFile);
                }
                return Collections.emptyMap();
            }
        } catch (final IOException e) {
            throw new RuntimeException("unable to read property file " + propertyFile + ".", e);
        }
    }
}
