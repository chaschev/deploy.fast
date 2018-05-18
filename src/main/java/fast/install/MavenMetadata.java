package fast.install;

import java.util.Date;
import java.util.List;

public class MavenMetadata {
  final public String release;
  final public List<String> versions;
  final public Date lastUpdated;

  public MavenMetadata(String release, List<String> versions, Date lastUpdated) {
    this.release = release;
    this.versions = versions;
    this.lastUpdated = lastUpdated;
  }
}
