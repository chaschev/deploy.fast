package fast.install;

import java.io.File;

interface Preinstall {
  void patch(File installationDir, File libDir);
}
