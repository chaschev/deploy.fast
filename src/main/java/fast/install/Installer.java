package fast.install;

import fast.maven.JavaArtifactResolver;
import fast.maven.MavenRepo;
import fast.maven.ResolveResult;
import fast.maven.JavaDumbMavenRepo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ok  MavenMetadata, MavenMetadataParser, MavenMetadataResolver (I)
// ok  download the latest/specified version honey:badger:[version-optional]
// ok  read resources from the downloaded jar
// ok  go on with the old version
//todo make sure this folder is used as an installation folder only, required libraries are copied into the app folder, so we don't have to delete old jars
public class Installer {
  public static final String MAVEN_CENTRAL = "http://central.maven.org/maven2";
  public static final String MY_JAR = "build/libs/deploy-fast-0.1.0.jar";


  private File miniRepoDir() {
    return new File(getInstallationDir(),"mini-repo");
  }

  private File myJar;

  private ModuleDependencies deps = null;

  public Installer() {
  }

  public void downloadAndInstall(String art, @Nullable String repo, boolean forceUpdate) {
    if(repo == null) repo = MAVEN_CENTRAL;

    ResolveResult resolveResult = downloadJar(art, repo);

    myJar = resolveResult.jarFile;

    resolveAll(forceUpdate);
  }

  public MavenMetadata getMetadata(String art, String repo) {
    return new JavaArtifactResolver(Collections.singletonList(
      new JavaDumbMavenRepo(repo)
    )).resolveMetadata(art);
  }

  private ResolveResult downloadJar(String art, String repo) {
    JavaArtifactResolver resolver = new JavaArtifactResolver(Collections.singletonList(
      new JavaDumbMavenRepo(repo)
    ));

    resolver.setForceUpdate(true);

    int c = 0;

    //stupid java
    for(int i = 0; i < art.length();i++) if(art.charAt(i) == ':') c++;

    if(c == 1) {
      final MavenMetadata metadata = resolver.resolveMetadata(art);

      if(metadata == null) {
        throw new RuntimeException("couldn't find metadata for " + art + ". You can specify version directly");
      } else {
        System.out.println("latest version for " + art + " is " + metadata.release);
      }

      art = art + ":" + metadata.release;
    }

    return resolver.resolve(art, miniRepoDir());
  }

  public static void main(String[] args) throws Exception {

    new Installer().runAppInstaller(args);
  }

  private void runAppInstaller(String[] args) throws Exception {

    String art, repo;

    if(args.length == 0) {
      System.out.println("getting the artifact from jar");
      ModuleDependencies deps = new ModuleDependencies(
        SmartJavaResources.getMyJar(this.getClass(), MY_JAR)
      );

      art = deps.me;
      repo = deps.myRepo.root();
    } else {
      art = args[0];
      repo = args[1];
    }

    // first, download main jar

    downloadAndInstall(art, repo, false);

    // then, resolve all dependencies

    final File libDir = new File(getInstallationDir(), "lib");

    resolveDepsToLibFolder(libDir);

    // manually patch jars if needed. I.e. there is a shit problem with
    // Kotlin compiler which was lazy to shade Guava libs

    BuildProperties buildProps = ModuleDependencies.getBuildProperties(this.getClass(), MY_JAR);

    Runnable preinstall = (Runnable)Class.forName(buildProps.ext.get("honeyMouth.preinstallClass")).newInstance();

    preinstall.run();

    // next, build classpath and start Kotlin-based install

    final String classpath =  libDir.getAbsolutePath()+"/*";

    System.out.println("Using classpath: " + classpath);

    String[] command = {
      "java",
      "-cp",
      classpath,
      buildProps.ext.get("honeyMouth.installClass")
    };

    Process java = new ProcessBuilder(command)
      .inheritIO()
      .start();

    System.out.println("Waiting for child process and leaving...");

    for (int i = 0; i < 60000000; i++) {
      if (!java.isAlive()) {
        if (java.exitValue() != 0) {
          System.out.println("ERROR: child process ended with code " + java.exitValue());
        }

        System.exit(java.exitValue());
      }

      Thread.sleep(100);
    }
  }

  public void resolveDepsToLibFolder(File libDir) throws IOException {
    final Map<String, File> resolvedDeps = resolveAll(false);

    libDir.mkdirs();

    // copy all dependencies into the lib dir

    for (File file : libDir.listFiles()) {
      file.delete();
    }

    for (File file : resolvedDeps.values()) {
      Files.copy(file.toPath(), new File(libDir, file.getName()).toPath());
    }
  }

  private static File getInstallationDir() {
    String installationPath;

    {
      String temp = System.getenv("INSTALLATION_PATH");

      if(temp == null) temp = ".";

      installationPath = temp;
    }

    return new File(installationPath);
  }


  /**
   * @param forceUpdate is a little slower, but more precise. It will update sha1 for downloaded files.
   */
  public Map<String, File> resolveAll(boolean forceUpdate) {return _install(forceUpdate);}

  private Map<String, File> _install(boolean forceUpdate) {
    miniRepoDir().mkdirs();

//    May be next time will have his complexity
//    System.out.println("downloading runtime libraries...");
//
//    new JavaArtifactResolver(Arrays.asList(
//      new JavaDumbMavenRepo("http://dl.bintray.com/kotlin/kotlin-eap-1.2"),
//      new JavaDumbMavenRepo(MAVEN_CENTRAL))
//    ).resolveAll(libDir, Arrays.asList(artifacts));

    System.out.println("downloading app libraries...");

    final List<String> depStrings = getDeps().getDependencies(true);
    final List<MavenRepo> repoList = getDeps().getRepos(true);

    System.out.println("resolving " + depStrings.size() + " artifacts in " +
      repoList.size() +
      " repositories:\n " + String.join("\n", repoList.stream().map(MavenRepo::root).collect(Collectors.toList())
      ) +
      "...");

    return new JavaArtifactResolver(repoList)
      .setForceUpdate(forceUpdate)
      .resolveAll(miniRepoDir(), depStrings);
  }

  public synchronized ModuleDependencies getDeps() {
    if(deps == null) {
      if(myJar == null) {
        throw new RuntimeException("you need to set myJar to get dependencies");
      }
      deps = new ModuleDependencies(myJar);
    }
    return deps;
  }

  public String getVersion() {return getDeps().me.split(":")[2];}

  public Installer setMyJar(File myJar) {
    this.myJar = myJar;
    return this;
  }
}
