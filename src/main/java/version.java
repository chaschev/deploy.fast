import fast.install.BuildProperties;

import static fast.install.Installer.MY_JAR;

//todo make sure this folder is used as an installation folder only, required libraries are copied into the app folder, so we don't have to delete old jars
public class version {
  public version() {
  }

  public static void main(String[] args) throws Exception {
    new version().run();
  }

  private void run()  {
    System.out.println(getVersion(this.getClass()));
  }

  public static String getVersion(Class<?> aClass) {
    BuildProperties buildProps = BuildProperties.getBuildProperties(aClass, MY_JAR);

    return String.format("%s version %s rev. %s %s", buildProps.name, buildProps.version, buildProps.revision, buildProps.timestamp);
  }
}
