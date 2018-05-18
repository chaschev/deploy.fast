import fast.install.Installer;
import fast.install.ModuleDependencies;
import fast.install.SmartJavaResources;
import fast.maven.JavaArtifactResolver;
import fast.maven.MavenRepo;
import fast.maven.VeryOldJavaMethods;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ok  MavenMetadata, MavenMetadataParser, MavenMetadataResolver (I)
// ok  download the latest/specified version honey:badger:[version-optional]
// ok  read resources from the downloaded jar
// ok  go on with the old version
//todo make sure this folder is used as an installation folder only, required libraries are copied into the app folder, so we don't have to delete old jars
public class getJars {
  public static final String MAVEN_CENTRAL = "http://central.maven.org/maven2";

  String miniRepoDirPath = "mini-repo";
  String destDirPath = "libs";

  private File myJar;

  private ModuleDependencies deps = null;

  public getJars() {
  }


  public static void main(String[] args) throws Exception {
    new getJars().runAppInstaller(args);
  }

  private void runAppInstaller(String[] args) throws Exception {

    String art = null, repo = null;

    boolean forceUpdate = false;
    boolean useCaches = true;

//    if(args.length == 0)
//      printUsage("", 1);


    for (String arg : args) {
      if(!arg.startsWith("--") && ! arg.contains("=")) {
        //--artifact=a:b:my.jar --repo=http://my_repo_url
        printUsage("", 1);
      }
    }

    for (String arg : args) {
      int t = arg.indexOf('=');
      String name = arg.substring(2, t);
      String value = arg.substring(t + 1);

      switch (name) {
        case "artifact": art = value; break;
        case "repo":  repo = value; break;
        case "cache-dir": miniRepoDirPath = value; break;
        case "dest-dir": destDirPath = value; break;
        case "force-update": forceUpdate = Boolean.parseBoolean(value); break;
        case "use-external-cache": useCaches = Boolean.parseBoolean(value); break;
        default:
          printUsage("don't know arg " + name, 2);
      }
    }

    if(art == null) {
      System.out.println("getting the artifact from jar");

      myJar = SmartJavaResources.getMyJar(this.getClass(), Installer.MY_JAR);
    }



    resolveDepsToLibFolder(new File(destDirPath), forceUpdate, useCaches);
  }

  private void printUsage(String s, int i) {
    System.out.println("USAGE: getJars  --cache-dir=mini-repo --dest-dir=libs");
    if (s != null && !s.isEmpty()) System.out.println(s);
    System.exit(i);
  }

  public void resolveDepsToLibFolder(File libDir, boolean forceUpdate, boolean useExternalCaches) throws IOException {
    final Map<String, File> resolvedDeps = resolveAll(forceUpdate, useExternalCaches);

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
   * @param useExternalCaches
   */
  public Map<String, File> resolveAll(boolean forceUpdate, boolean useExternalCaches) {
    return _install(forceUpdate, useExternalCaches);
  }

  private Map<String, File> _install(boolean forceUpdate, boolean useExternalCaches) {
    File miniRepoDir = new File(miniRepoDirPath);

    miniRepoDir.mkdirs();

    System.out.println("copying app jar...");

//    BuildProperties buildProps = ModuleDependencies.getBuildProperties(this.getClass(), myJar.getAbsolutePath());

    try {
      Runtime.getRuntime().exec(new String[] {
        "cp", "-f", myJar.getAbsolutePath(), new File(miniRepoDir, myJar.getName()).getAbsolutePath()
      });

      String sha1 = VeryOldJavaMethods.getSha1(myJar.toPath());
      VeryOldJavaMethods.writeFile(new File(miniRepoDir, myJar.getName() + ".sha1"), sha1);
    } catch (IOException e) {
      System.out.println("failed to copy own jar: " + myJar);
      System.exit(3);
    }

    final List<String> depStrings = getDeps().getDependencies(true);
    final List<MavenRepo> repoList = getDeps().getRepos(true);

    System.out.println("resolving " + depStrings.size() + " artifacts in " +
      repoList.size() +
      " repositories:\n " + String.join("\n", repoList.stream().map(MavenRepo::root).collect(Collectors.toList())
      ) +
      "...");

    return new JavaArtifactResolver(repoList)
      .setUseExternalCaches(useExternalCaches)
      .setForceUpdate(forceUpdate)
      .resolveAll(miniRepoDir, depStrings);
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

  public getJars setMyJar(File myJar) {
    this.myJar = myJar;
    return this;
  }
}
