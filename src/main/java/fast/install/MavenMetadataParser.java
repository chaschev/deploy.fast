package fast.install;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MavenMetadataParser {
  public MavenMetadata parse(String xml) {
    final int startAt = 0;
    final int[] pos = {0};

    String release = getValue(xml, "release", startAt, pos);

    List<String> versions = new ArrayList<>();
    String version;

    while((version = getValue(xml, "version", pos[0], pos)) != null) {
      versions.add(version);
    }

    Date lastUpdated = new Date(Long.parseLong(getValue(xml, "lastUpdated", pos[0], pos)));

    return new MavenMetadata(release, versions, lastUpdated);
  }

  @Nullable
  private String getValue(String xml, final String tag, int startAt, int[] newPos) {
    int indexOfOpening = xml.indexOf("<" + tag + ">", startAt);

    if(indexOfOpening == -1) return null;

    int indexOfEnding = xml.indexOf("</" + tag + ">", indexOfOpening + tag.length() + 2);

    newPos[0] = indexOfEnding + tag.length() + 3;

    return xml.substring(indexOfOpening + tag.length() + 2, indexOfEnding);
  }
}
