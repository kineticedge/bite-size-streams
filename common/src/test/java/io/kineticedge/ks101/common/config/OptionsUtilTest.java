package io.kineticedge.ks101.common.config;

import com.beust.jcommander.Parameter;
import io.kineticedge.kstutorial.common.config.BaseOptions;
import io.kineticedge.kstutorial.common.config.OptionsUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OptionsUtilTest {

  public enum WindowType {TUMBLING, HOPPING, SLIDING, SESSION}

  public static class Options extends BaseOptions {
    @Parameter(names = {"--window-type"}, description = "")
    private WindowType windowType = WindowType.TUMBLING;

    @Parameter(names = {"--name"}, description = "")
    private String name = "foo";

    public WindowType getWindowType() {
      return windowType;
    }

    public void setWindowType(WindowType windowType) {
      this.windowType = windowType;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  public void byDefault() {

    final String[] args = {};

    Options options = OptionsUtil.parse(Options.class, args);

    assertNotNull(options);
    assertEquals(options.getWindowType(), WindowType.TUMBLING);
    assertEquals(options.getName(), "foo");
  }

  @Test
  public void byArguments() {

    final String[] args = {"--window-type", "HOPPING", "--name", "bar"};

    Options options = OptionsUtil.parse(Options.class, args);

    assertNotNull(options);
    assertEquals(options.getWindowType(), WindowType.HOPPING);
    assertEquals(options.getName(), "bar");
  }

  @Disabled
  @Test
  //@SetEnvironmentVariable(key = "WINDOW_TYPE", value = "SESSION")
  //@SetEnvironmentVariable(key = "NAME", value = "foobar")
  public void byEnvironment() {

    final String[] args = {};

    Options options = OptionsUtil.parse(Options.class, args);

    assertNotNull(options);
    assertEquals(options.getWindowType(), WindowType.SESSION);
    assertEquals(options.getName(), "foobar");

  }

}