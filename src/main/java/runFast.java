import fast.install.ModuleDependencies;
import fast.install.SmartJavaResources;
import fast.maven.VeryOldJavaMethods;

import java.io.File;
import java.lang.reflect.Field;

import static fast.install.Installer.MY_JAR;
import sun.misc.Unsafe;

//todo make sure this folder is used as an installation folder only, required libraries are copied into the app folder, so we don't have to delete old jars
public class runFast {
  public runFast() {
  }

  public static void main(String[] args) throws Exception {
    new runFast().run(args);
  }

  private void run(String[] args)  {
    int pid = runFast(this.getClass(), args, null);
    VeryOldJavaMethods.writeFile(new File("pid"), Integer.toString(pid) + "\n");
  }

  public static int runFast(Class<?> aClass, String[] args, String[] checkDirs) {
    disableJavaOldFashionedWarnings();

    ModuleDependencies deps = new ModuleDependencies(SmartJavaResources.getMyJar(aClass, MY_JAR));

    String[] dirs = checkDirs == null ? new String[]{"libs", "../libs", "../../libs"} : checkDirs;

    File libsDir = null;

    for (String dirPath : dirs) {
      final File dir = new File(dirPath);

      if(dir.exists()) {
        libsDir = dir;
        break;
      }
    }

    if(libsDir == null) {
      System.out.println("could not locate libs dir");
      System.exit(0);
    }

    String[] command = new String[3 + args.length];

    command[0] = "java";
    command[1] = "-cp";
    command[2] = libsDir.getPath() + "/*";

    System.arraycopy(args, 0, command, 3, args.length);

    try {
//      final Process process = Runtime.getRuntime().exec(command);
      final Process process = new ProcessBuilder(command).
        redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start();

      final Class cls = process.getClass();

      final String clsSimpleName = cls.getSimpleName();

      if(clsSimpleName.equals("UNIXProcess") || clsSimpleName.equals("ProcessImpl")) {
        final Field pidField = cls.getDeclaredField("pid");
        pidField.setAccessible(true);

        final int pid = (int) pidField.get(process);

        return pid;
      }

    } catch (Exception e) {
      System.out.println("unable to start process: " + e);
      System.exit(1);
    }

    return 0;
  }

  public static void disableJavaOldFashionedWarnings() {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      Unsafe u = (Unsafe) theUnsafe.get(null);

      Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
      Field logger = cls.getDeclaredField("logger");
      u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
    } catch (Exception e) {
      // ignore
    }
  }
}


