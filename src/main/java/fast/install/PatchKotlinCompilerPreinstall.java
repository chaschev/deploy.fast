package fast.install;

import java.io.File;

public class PatchKotlinCompilerPreinstall implements Preinstall {
  @Override
  public void patch(File installationDir, File libDir) {
    File[] compilerJar = libDir.listFiles(
      pathname ->
      pathname.getName().matches("kotlin-compiler-.*.jar")
    );

    if(compilerJar == null || compilerJar.length == 0) {
      System.out.println("didn't patch kotlin compiler, jar was not found");
      return;
    }

    //todo refactor into process watch
    watchProcess(new String[]{"zip",
      "-d",
      "kotlin-compiler-1.2.0-rc-84.jar",
      "com/google/*"});

  }

  private void watchProcess(String[] command) {
    final ProcessBuilder pb = new ProcessBuilder(
      command)
      .inheritIO();

    try {
      final Process process = pb.start();
      final Thread watchedThread = Thread.currentThread();

      new Thread(() -> {
        try {
          for(int i = 0;i<50;i++) {
            if(!process.isAlive() || Thread.interrupted()) return;
            Thread.sleep(100);
          }

          watchedThread.interrupt();
          process.destroyForcibly();
        } catch (Exception e) {
          System.out.println("smth unexpected in watch thread");
          e.printStackTrace();
        }
      },"zip-watch");

      process.waitFor();

      int exitValue = process.exitValue();

      if(exitValue != 0) {
        System.out.println("WARN: deleted 0 entries");
      } else {
        System.out.println("successfully patched");
      }
    }  catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
