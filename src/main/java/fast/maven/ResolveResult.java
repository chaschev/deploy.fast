package fast.maven;

import java.io.File;

public class ResolveResult {
    public final File jarFile;
    public final File sha1File;

    public ResolveResult(File jarFile, File sha1File) {
        this.jarFile = jarFile;
        this.sha1File = sha1File;
    }
}
