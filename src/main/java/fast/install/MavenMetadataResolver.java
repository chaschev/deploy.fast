package fast.install;

import org.jetbrains.annotations.Nullable;

public interface MavenMetadataResolver {
  @Nullable
  MavenMetadata resolveMetadata(String art);
}

