package fast.install;

import java.util.*;

public class BuildProperties {
  public final String artifact;
  public final String name;
  public final String version;
  public final String revision;
  public final Date buildTime;
  public final String timestamp;
  public final Map<String, String> ext;

  public BuildProperties(Class<?> aClass, String fallbackJarPath) {
    final Properties properties = SmartJavaResources.readResourceProperties(aClass, "/build.properties", fallbackJarPath);

    artifact = properties.getProperty("artifact");
    name = properties.getProperty("name");
    version = properties.getProperty("version");
    revision = properties.getProperty("revision");
    buildTime = new Date(Long.parseLong(properties.getProperty("buildTimeMillis")));
    timestamp = properties.getProperty("timestamp");

    final Map<String, String> temp = new HashMap<>();

    properties.forEach((k, v) -> {
      String key = k.toString();

      if (key.startsWith("ext.")) {
        temp.put(key.substring("ext.".length()), v.toString());
      }
    });

    ext = Collections.unmodifiableMap(temp);
  }

  @Override
  public String toString() {
    return name + " " + version + " (" + timestamp + ") rev. " + revision;
  }
}
