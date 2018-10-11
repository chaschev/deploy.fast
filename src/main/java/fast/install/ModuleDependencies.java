package fast.install;

import fast.maven.MavenRepo;
import fast.maven.JavaDumbMavenRepo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleDependencies {
  public final String me;
  public final MavenRepo myRepo;
  private final List<MavenRepo> repos = new ArrayList<>();
  private final List<String> dependencies = new ArrayList<>();

  public ModuleDependencies(File jar) {
    String jarsString = SmartJavaResources.getText(jar, "/mySpecialPom");

    if(jarsString == null) {
      throw new RuntimeException("couldn't read app classpath in /mySpecialPom");
    }

    int meStartIndex = jarsString.indexOf("# Me");
    int myRepoStartIndex = jarsString.indexOf("# My Repo");
    int reposStartIndex = jarsString.indexOf("# Repos");
    int artsStartIndex = jarsString.indexOf("# Artifacts");

    me = jarsString.substring(meStartIndex + "# Me".length()).trim().split("\n")[0];
    myRepo = new JavaDumbMavenRepo(jarsString.substring(myRepoStartIndex + "# My Repo".length()).trim().split("\n")[0]);
    String[] reposUrls = jarsString.substring(reposStartIndex + "# Repos".length(), artsStartIndex).trim().split("\n");

    final String[] tempDeps = jarsString.substring(artsStartIndex + "# Artifacts".length()).trim().split("\n");

    repos.add(new JavaDumbMavenRepo(Installer.MAVEN_CENTRAL));

    for (String reposUrl : reposUrls) {
      repos.add(new JavaDumbMavenRepo(reposUrl));
    }

    Collections.addAll(dependencies, tempDeps);
  }

  public List<MavenRepo> getRepos() {
    return repos;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public List<MavenRepo> getRepos(boolean includeMe) {
    final ArrayList<MavenRepo> r = new ArrayList<>(repos);
    if(includeMe) r.add(myRepo);
    return r;
  }

  public List<String> getDependencies(boolean includeMe) {
    final ArrayList<String> r = new ArrayList<>(dependencies);
    if(includeMe) r.add(me);
    return r;
  }

}
