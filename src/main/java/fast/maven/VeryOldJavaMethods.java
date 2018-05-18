package fast.maven;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Formatter;

public class VeryOldJavaMethods {
  public static String readFile(File file) {
    return readFile(file.toPath());
  }

  @NotNull
  public static String readFile(Path path) {
    try {
      byte[] encoded = Files.readAllBytes(path);

      return new String(encoded);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static void writeFile(File file, String value) {
    try {
      Files.write(file.toPath(), value.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getSha1(Path path) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.reset();
      digest.update(Files.readAllBytes(path));

      return byteToHex(digest.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String byteToHex(final byte[] hash) {
    try (Formatter formatter = new Formatter()) {
      for (byte b : hash) {
        formatter.format("%02x", b);
      }

      return formatter.toString();
    }
  }

  public static void downloadJavaWay(String url, File dest) {
    try (OutputStream os = new FileOutputStream(dest)) {
      downloadJavaWay(url, os);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void downloadJavaWay(String url, OutputStream os) {
    URL webUrl;

    //ok java
    try {
//      System.out.println("trying " + url);
      webUrl = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    String message = "ok";

    try (InputStream is = webUrl.openStream()) {
      copyStream(is, os);
    } catch (IOException e) {
//      message = "error: " + e.toString();

//      System.out.println(message);

      throw new RuntimeException(e);
    } finally {
//      System.out.println(message);
    }
  }

  public static String downloadAsString(String url, String[] auth, String requestMethod) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(8 * 1024)) {
      downloadWithAuth(url, auth, os, requestMethod);
      return os.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String downloadAsString(String url) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(8 * 1024)) {
      downloadJavaWay(url, os);
      return os.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void downloadFileWithAuth(
    String url,
    String[] auth,
    File dest,
    String requestMethod
  ) {
    try (OutputStream os = new FileOutputStream(dest)) {
      downloadWithAuth(url, auth, os, requestMethod);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void downloadWithAuth(
    String urlString,
    String[] auth,
    OutputStream os,
    String requestMethod
  ) {
    try {
      System.out.println("POST " + urlString);

      URL url = new URL(urlString);
      String encoding = Base64.getEncoder().encodeToString((auth[0] + ":" + auth[1]).getBytes());

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(requestMethod);
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", "Basic " + encoding);
      try (InputStream is = connection.getInputStream()) {
        copyStream(is, os);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[8 * 1024];
    int len;
    while ((len = in.read(buffer)) > 0) {
      out.write(buffer, 0, len);
    }
  }

  public static String streamToString(InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
