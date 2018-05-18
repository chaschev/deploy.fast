package fast.maven;

import fast.install.MavenMetadataParser;
import fast.install.MavenMetadataResolver;
import fast.install.MavenMetadata;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JavaArtifactResolver implements MavenMetadataResolver {
  protected List<MavenRepo> repos;
  protected boolean forceUpdate = false;
  private boolean useExternalCaches = true;

  public JavaArtifactResolver(List<MavenRepo> repos) {
    this.repos = repos;
  }

  public Map<String, File> resolveAll(File cacheFolder, List<String> arts) {
    Map<String, File> map = new HashMap<>();

    for (String art : arts) {
      final ResolveResult r = resolve(art, cacheFolder);

      if (r == null) {
        throw new RuntimeException("couldn't resolve " + art);
      }

      map.put(art, r.jarFile);
    }

    return map;
  }

  public JavaArtifactResolver setForceUpdate(boolean forceUpdate) {
    this.forceUpdate = forceUpdate;
    return this;
  }


  @Nullable
  public ResolveResult resolve(String art, File cacheFolder) {
    String group, module, version;

    {
      String[] r = art.split(":");
      group = r[0];
      module = r[1];
      version = r[2];
    }

    for (MavenRepo repo : repos) {
      String file = repo.file(module, version);
      String url = repo.resolveUrl(group, module, version);

      File jarFile = new File(cacheFolder, file + ".jar");
      File sha1File = new File(cacheFolder, file + ".jar.sha1");


      if (forceUpdate || !sha1File.exists()) {
        try {
          String sha1 = repo.downloadSha1(url);
          VeryOldJavaMethods.writeFile(sha1File, sha1);
        } catch (Exception e) {
          continue;
        }
      }

      ResolveResult result = new ResolveResult(jarFile, sha1File);
      ResolveResult isCached = isCached(sha1File, jarFile, file, result);

      if(isCached != null) return isCached;

      final String sha1 = VeryOldJavaMethods.readFile(sha1File);

      if (useExternalCaches) {
        File mavenCandidate = new File(MessageFormat.format("{0}/.m2/repository/{3}/{1}/{2}/{1}-{2}.jar", System.getenv("HOME"), module, version, group));

        // .gradle/caches/modules-2/files-2.1/org.redisson/redisson/3.5.7/b5a44165d9e7b904edc9263c0c38262d1e8baa2a/redisson-3.5.7.jar
        File gradleCandidate = new File(MessageFormat.format("{0}/.gradle/caches/modules-2/files-2.1/{3}/{1}/{2}/{4}/{1}-{2}.jar", System.getenv("HOME"), module, version, group, sha1));

        File existingFile = null;

        if (mavenCandidate.exists()) {
          System.out.println("got from maven cache: " + mavenCandidate.getPath());
          existingFile = mavenCandidate;
        } else if (gradleCandidate.exists()) {
          System.out.println("got from gradle cache: " + gradleCandidate.getPath());
          existingFile = gradleCandidate;
        }

        if(existingFile != null) {
          try {
            Runtime.getRuntime().exec(new String[] {"cp", existingFile.getAbsolutePath(), jarFile.getAbsoluteFile().getAbsolutePath()} );
          } catch (IOException e) {
            System.out.println("error copying " + existingFile + " from external cache " + ": " + e);
          }
        }
      }

      isCached = isCached(sha1File, jarFile, file, result);

      if (isCached != null) return isCached;

      try {
        System.out.printf("GET %s.jar... ", url);

        repo.downloadJar(url, jarFile);

        String actualSha1 = VeryOldJavaMethods.getSha1(jarFile.toPath());

        if (!Objects.equals(sha1, actualSha1)) {
          System.out.println("downloaded a file, and sha1 didn't match: " + actualSha1 + " (actual) vs " + sha1 + " (expected)");
          //sha1 file can be corrupt (404)
          continue;
        }

        System.out.println("ok");

        return result;
      } catch (Exception e) {
        throw new RuntimeException("can't download url " + url + ".jar for artifact " + art);
      }
    }

    return null;
  }

  public static String extractSha1(String line) {
    line = line.trim();

    int indexOfSpace = line.indexOf(" ");

    String sha1 = indexOfSpace == -1 ? line : line.substring(0, indexOfSpace);

    validateSha1(sha1);

    return sha1;
  }


  private static void validateSha1(String sha1) {
    if (sha1.length() > 100 || sha1.length() < 16 ||
      !sha1.matches("^[0-9a-f]{16,100}$")) throw new RuntimeException("something is not ok about sha1");
  }

  private static ResolveResult isCached(File sha1File, File jarFile, String file, ResolveResult result) {
    ResolveResult cached;

    if (sha1File.exists() && jarFile.exists()) {
      String sha1 = extractSha1(VeryOldJavaMethods.readFile(sha1File.toPath()));
      String actualSha1 = VeryOldJavaMethods.getSha1(jarFile.toPath());

      if (!sha1.equals(actualSha1)) {
        System.out.printf("downloaded a file, and sha1 didn't match: %s (actual) vs %s (expected)%n", actualSha1, sha1);

        cached = null;
      } else {
        System.out.println("cached: " + file);

        cached = result;
      }
    } else {
      cached = null;
    }

    return cached;
  }

  public void install() {
    File libDir = new File("lib");

    libDir.mkdir();

    System.out.println("downloading runtime libraries...");
  }

  @Override
  public MavenMetadata resolveMetadata(String art) {
    String group, module;

    {
      String[] r = art.split(":");
      group = r[0];
      module = r[1];
    }

    for (MavenRepo repo : repos) {
      try {
        String xml = VeryOldJavaMethods.downloadAsString(repo.metadataUrl(group, module));
        return new MavenMetadataParser().parse(xml);
      } catch (Exception e) {
        //ignore
      }
    }

    return null;
  }

  public JavaArtifactResolver setUseExternalCaches(boolean useExternalCaches) {
    this.useExternalCaches = useExternalCaches;
    return this;
  }
}
