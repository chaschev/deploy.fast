package fast.install;

import fast.maven.VeryOldJavaMethods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SmartJavaResources {
  public static String getText(File jar, String path) {
    if (path.startsWith("/")) path = path.substring(1);

    try (ZipFile zip = new ZipFile(jar)) {
      final ZipEntry entry = zip.getEntry(path);
      if (entry == null) {
        throw new RuntimeException("coudn't find " + path + " in " + jar);
      }
      try (InputStream is = zip.getInputStream(entry)) {
        return VeryOldJavaMethods.streamToString(is);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String readResource(Class<?> aClass, String resourcePath) {
    try {
      final InputStream stream = aClass.getResourceAsStream(resourcePath);

      if (stream == null) return null;

      return VeryOldJavaMethods.streamToString(stream);
    } catch (Exception e) {
      return null;
    }
  }

  public static File getMyJar(Class<?> aClass, String fallbackJarPath) {
    try {
      File file = new File(aClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

      if (file.isDirectory()) {
        final RuntimeException exception = new RuntimeException("can't find my jar. you need to specify fallback jar for dev purposes");

        if(fallbackJarPath == null) throw exception;

        file = new File(fallbackJarPath);

        if (!file.exists() || file.isDirectory() || !file.getName().endsWith(".jar")) {
          throw exception;
        }
      }

      return file;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }


  public static Properties readResourceProperties(Class<?> aClass, String path, String fallbackJarPath) {
    File myJar;

    //if can't load jar and no fallbackJarPath provided, read directly from resource
    try {
      myJar = getMyJar(aClass, fallbackJarPath);
    } catch (Exception e) {
      myJar = null;
    }

    final Properties props = new Properties();

    try {
      if(myJar != null)
        props.load(new StringReader(getText(myJar, path)));
      else
        props.load(aClass.getResourceAsStream(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return props;
  }
}
