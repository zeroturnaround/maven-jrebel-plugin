package org.zeroturnaround.javarebel.maven.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Util class to ensure safe string representation for using in paths and URLs.
 */
public final class SystemUtils {

  private SystemUtils() {
  }

  private static final String[] DISALLOWED_PATH_NAMES = new String[] {
      "CON", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "LPT1", "LPT2", "LPT3", "LPT4", "LST", "KEYBD$", "SCREEN$", "$IDLE$", "CONFIG$"
  };

  public static String ensurePathAndURLSafeName(String source) {
    boolean disallowedName = false;

    if (source == null || source.length() == 0) {
      return "_";
    }

    for (String s : DISALLOWED_PATH_NAMES) {
      if (s.equalsIgnoreCase(source)) {
        disallowedName = true;
        break;
      }
    }

    if (disallowedName) {
      return "_" + source.replace("$", "_24") + "_";
    }

    StringBuilder builder = new StringBuilder(source.length());
    try {
      final String urlEncoded = URLEncoder.encode(source, "UTF-8");
      for (char ch : urlEncoded.toCharArray()) {
        switch (ch) {
          case '%':
            builder.append('_');
            break;
          case '[':
            builder.append("_5B");
            break;
          case ']':
            builder.append("_5D");
            break;
          case '+':
            builder.append("__");
            break;
          case '~':
            builder.append("_7E");
            break;
          case '*':
            builder.append("_2A");
            break;
          default:
            builder.append(ch);
            break;
        }
      }
    }
    catch (UnsupportedEncodingException ex) {
      throw new RuntimeException("Can't find UTF-8 charset!", ex);
    }
    return builder.toString();
  }
}
