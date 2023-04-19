package qupath.lib.extension.monailabel.commands.utils;

import java.util.UUID;

public class StringGenerator {
  public static String generateName() {
    String uuid = UUID.randomUUID().toString();
    return uuid.replaceAll("-", "");
  }
}
