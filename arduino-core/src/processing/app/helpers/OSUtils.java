package processing.app.helpers;

import java.util.Locale;

public class OSUtils {

  /**
   * types of Operating Systems
   */
  public static enum OSType {
    OS_WINDOWS, OS_MAC_OS_X, OS_LINUX, OS_SOLARIS, OS_OTHER
  };

  // cached result of OS detection
  protected static OSType detectedOS;

  /**
   * @return true if running on windows.
   */
  static public boolean isWindows() {
    //return PApplet.platform == PConstants.WINDOWS;
    return System.getProperty("os.name").contains("Windows");
  }

  /**
   * @return true if running on linux.
   */
  static public boolean isLinux() {
    //return PApplet.platform == PConstants.LINUX;
    return System.getProperty("os.name").contains("Linux");
  }

  /**
   * @return true if Processing is running on a Mac OS X machine.
   */
  static public boolean isMacOS() {
    //return PApplet.platform == PConstants.MACOSX;
    return (OSUtils.getOperatingSystemType() == OSType.OS_MAC_OS_X);
  }

  static public boolean hasMacOSStyleMenus() {
    return OSUtils.isMacOS() && "true".equals(System.getProperty("apple.laf.useScreenMenuBar"));
  }

  static public String version() {
    return System.getProperty("os.version");
  }

  /**
   * detect the operating system from the os.name System property and cache
   * the result
   * 
   * @return - the operating system detected
   */
  public static OSType getOperatingSystemType() {
    if (detectedOS == null) {
      String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
      if ((OS.contains("mac")) || (OS.contains("darwin"))) {
        detectedOS = OSType.OS_MAC_OS_X;
      } else if (OS.contains("win")) {
        detectedOS = OSType.OS_WINDOWS;
      } else if (OS.contains("nux")) {
        detectedOS = OSType.OS_LINUX;
      } else if (OS.contains("sun os")
	     || OS.contains("sunos")
	     || OS.contains("solaris")) {
        detectedOS = OSType.OS_SOLARIS;
      } else {
        detectedOS = OSType.OS_OTHER;
        
      }
    }
    return detectedOS;
  }
}
