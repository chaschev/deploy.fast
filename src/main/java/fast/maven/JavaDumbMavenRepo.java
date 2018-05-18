package fast.maven;

import fast.install.VerySmartProperties;

public class JavaDumbMavenRepo implements MavenRepo {
  protected final String root;
  protected final String[] auth;

  public JavaDumbMavenRepo(String root) {
    int authIndex = root.indexOf('@');

    if(authIndex == -1) {
      this.root = root;
      auth = null;
    }else {
      this.root = root.substring(authIndex + 1);
      this.auth = root.substring(0, authIndex).split(":");

      final VerySmartProperties props = new VerySmartProperties(".honey/auth.properties");

      auth[0] = props.get(auth[0]);
      auth[1] = props.get(auth[1]);
    }
  }

  @Override
  public String root() {
    return root;
  }

  @Override
  public String resolveUrl(String group, String module, String version) {
    return root + "/" + group.replace('.', '/') + "/" + module + "/" + version + "/" + file(module, version);
  }

  @Override
  public String metadataUrl(String group, String module) {
    return root + "/" + group.replace('.', '/') + "/" + module + "/maven-metadata.xml";
  }

  @Override
  public String[] auth() {
    return auth;
  }
}
