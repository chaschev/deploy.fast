package fast.install;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

public class VerySmartProperties {
  final Properties props = new Properties();

  public VerySmartProperties(String path) {
    if(!new File(path).exists()) {
      throw new RuntimeException("props files does not exist: " + path);
    }

    try(Reader reader = new FileReader(path)) {
      props.load(reader);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String get(String name) {
    return props.getProperty(name);
  }
}
