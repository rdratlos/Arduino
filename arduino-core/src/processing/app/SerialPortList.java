/* jSSC (Java Simple Serial Connector) - serial port communication library.
 * (C) Alexey Sokolov (scream3r), 2010-2014.
 * 
 * Patched for Arduino by Cristian Maglie.
 *
 * This file is part of jSSC.
 *
 * jSSC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jSSC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jSSC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you use jSSC in public project you can inform me about this by e-mail,
 * of course if you want it.
 *
 * e-mail: scream3r.org@gmail.com
 * web-site: http://scream3r.org | http://code.google.com/p/java-simple-serial-connector/
 */
package processing.app;

import java.io.File;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException​;

import processing.app.helpers.OSUtils;


/**
 *
 * @author rdratlos@nepomuc.de
 * @Todo   Add more OS to getPortLabels()
 *         Thoroughly test systems other than Windows and Linux for adequate
 *         serial port discovery by jSerialComm library. Systems that are correctly
 *         supported by jSerialComm should be added to the OS list in getPortNames()
 *         to use getPortLabels() for port dioscovery.
 */
public class SerialPortList {
    private static SerialPort[] ports;

    private static final Pattern PORTNAMES_REGEXP;
    private static final String PORTNAMES_PATH;
    
    static {
        ports = SerialPort.getCommPorts();
        switch (OSUtils.getOperatingSystemType()) {
            case OS_LINUX: {
                PORTNAMES_REGEXP = Pattern.compile("(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO)[0-9]{1,3}");
                PORTNAMES_PATH = "/dev/";
                break;
            }
            case OS_SOLARIS: {
                PORTNAMES_REGEXP = Pattern.compile("[0-9]*|[a-z]*");
                PORTNAMES_PATH = "/dev/term/";
                break;
            }
            case OS_MAC_OS_X: {
                PORTNAMES_REGEXP = Pattern.compile("(tty|cu)\\..*");
                PORTNAMES_PATH = "/dev/";
                break;
            }
            case OS_WINDOWS: {
                PORTNAMES_REGEXP = Pattern.compile("");
                PORTNAMES_PATH = "";
                break;
            }
            default: {
                PORTNAMES_REGEXP = null;
                PORTNAMES_PATH = null;
                break;
            }
        }
    }

    //since 2.1.0 -> Fully rewrited port name comparator
    private static final Comparator<String> PORTNAMES_COMPARATOR = new Comparator<String>() {

        @Override
        public int compare(String valueA, String valueB) {

            if(valueA.equalsIgnoreCase(valueB)){
                return valueA.compareTo(valueB);
            }

            int minLength = Math.min(valueA.length(), valueB.length());

            int shiftA = 0;
            int shiftB = 0;

            for(int i = 0; i < minLength; i++){
                char charA = valueA.charAt(i - shiftA);
                char charB = valueB.charAt(i - shiftB);
                if(charA != charB){
                    if(Character.isDigit(charA) && Character.isDigit(charB)){
                        int[] resultsA = getNumberAndLastIndex(valueA, i - shiftA);
                        int[] resultsB = getNumberAndLastIndex(valueB, i - shiftB);

                        if(resultsA[0] != resultsB[0]){
                            return resultsA[0] - resultsB[0];
                        }

                        if(valueA.length() < valueB.length()){
                            i = resultsA[1];
                            shiftB = resultsA[1] - resultsB[1];
                        }
                        else {
                            i = resultsB[1];
                            shiftA = resultsB[1] - resultsA[1];
                        }
                    }
                    else {
                        if(Character.toLowerCase(charA) - Character.toLowerCase(charB) != 0){
                            return Character.toLowerCase(charA) - Character.toLowerCase(charB);
                        }
                    }
                }
            }
            return valueA.compareToIgnoreCase(valueB);
        }

        /**
         * Evaluate port <b>index/number</b> from <b>startIndex</b> to the number end. For example:
         * for port name <b>serial-123-FF</b> you should invoke this method with <b>startIndex = 7</b>
         *
         * @return If port <b>index/number</b> correctly evaluated it value will be returned<br>
         * <b>returnArray[0] = index/number</b><br>
         * <b>returnArray[1] = stopIndex</b><br>
         *
         * If incorrect:<br>
         * <b>returnArray[0] = -1</b><br>
         * <b>returnArray[1] = startIndex</b><br>
         *
         * For this name <b>serial-123-FF</b> result is:
         * <b>returnArray[0] = 123</b><br>
         * <b>returnArray[1] = 10</b><br>
         */
        private int[] getNumberAndLastIndex(String str, int startIndex) {
            String numberValue = "";
            int[] returnValues = {-1, startIndex};
            for(int i = startIndex; i < str.length(); i++){
                returnValues[1] = i;
                char c = str.charAt(i);
                if(Character.isDigit(c)){
                    numberValue += c;
                }
                else {
                    break;
                }
            }
            try {
                returnValues[0] = Integer.valueOf(numberValue);
            }
            catch (Exception ex) {
                //Do nothing
            }
            return returnValues;
        }
    };
    //<-since 2.1.0
    
    /**
     * Get sorted array of serial ports in the system using default settings:<br>
     *
     * <b>Search path</b><br>
     * Windows - ""(always ignored)<br>
     * Linux - "/dev/"<br>
     * Solaris - "/dev/term/"<br>
     * MacOSX - "/dev/"<br>
     *
     * <b>RegExp</b><br>
     * Windows - ""<br>
     * Linux - "(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm)[0-9]{1,3}"<br>
     * Solaris - "[0-9]*|[a-z]*"<br>
     * MacOSX - "tty.(serial|usbserial|usbmodem).*"<br>
     *
     * 
     * @return String array. If there is no ports in the system String[]
     * with <b>zero</b> length will be returned (since jSSC-0.8 in previous versions null will be returned)
     */
    public static List<String> getPortNames() {
        switch (OSUtils.getOperatingSystemType()) {
            case OS_LINUX:
            case OS_WINDOWS:
              return Arrays.asList(getPortLabels(PORTNAMES_COMPARATOR));
            default:
              return Arrays.asList(searchPortLabels(PORTNAMES_PATH, PORTNAMES_REGEXP, PORTNAMES_COMPARATOR));
        }
    }

    /**
     * Get serial port names in Windows
     *
     * @param comparator Comparator for sotring port names <b>(not null)</b>
     * @return String array. If there is no ports in the system String[]
     * 
     * @since 2.3.0
     */
    private static String[] getPortLabels(Comparator<String> comparator) {
        if(ports == null){
            return new String[]{};
        }
        
        String[] portNames = new String[ports.length];
        int i = 0;
        for(SerialPort port : ports) {
            portNames[i++] = port.getSystemPortName();
        }
        TreeSet<String> retPorts = new TreeSet<>(comparator);
        for(String portName : portNames){
          retPorts.add(portName);
        }
        return retPorts.toArray(new String[retPorts.size()]);
    }

    /**
     * Universal method for getting port names of _nix based systems
     * 
     * @param searchPath Path for searching serial ports <b>(not null)</b><br>
     * The default search paths:<br>
     * Linux, MacOSX: <b>/dev/</b><br>
     * Solaris: <b>/dev/term/</b><br>
     * Windows: <b>this parameter ingored</b>
     * @param pattern RegExp pattern for matching port names <b>(not null)</b>
     * @param comparator Comparator for sotring port names <b>(not null)</b>
     *
     * @return String array. If there is no ports in the system String[]
     */
    private static String[] searchPortLabels(String searchPath, Pattern pattern, Comparator<String> comparator) {
        searchPath = (searchPath.equals("") ? searchPath : (searchPath.endsWith("/") ? searchPath : searchPath + "/"));
        String[] returnArray = new String[]{};
        File dir = new File(searchPath);
        if(dir.exists() && dir.isDirectory()){
            File[] files = dir.listFiles();
            if(files.length > 0){
                TreeSet<String> portsTree = new TreeSet<>(comparator);
                for(File file : files){
                    String fileName = file.getName();
                    if(!file.isDirectory() && !file.isFile() && pattern.matcher(fileName).find()){
                        SerialPort discoverPort;
                        // Check for non-functional ports
                        try {
                          discoverPort = SerialPort.getCommPort(searchPath + fileName);
                        } catch (SerialPortInvalidPortException​ e) {
                          discoverPort = null;
                        }
                        if (discoverPort != null) {
                          portsTree.add(discoverPort.getSystemPortName());
                        }
                    }
                }
                returnArray = portsTree.toArray(returnArray);
            }
        }
        return returnArray;
    }
}
