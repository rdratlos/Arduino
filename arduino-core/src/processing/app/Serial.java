/*
  PSerial - class for serial port goodness
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry & Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.app;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException​;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static processing.app.I18n.format;
import static processing.app.I18n.tr;

public class Serial implements SerialPortDataListener {

  //PApplet parent;

  // properties can be passed in for default values
  // otherwise defaults to 9600 N81

  // these could be made static, which might be a solution
  // for the classloading problem.. because if code ran again,
  // the static class would have an object that could be closed

  private SerialPort port;

  private CharsetDecoder bytesToStrings;
  private static final int IN_BUFFER_CAPACITY = 128;
  private static final int OUT_BUFFER_CAPACITY = 128;
  private ByteBuffer inFromSerial = ByteBuffer.allocate(IN_BUFFER_CAPACITY);
  private CharBuffer outToMessage = CharBuffer.allocate(OUT_BUFFER_CAPACITY);

  public Serial() throws SerialException {
    this(PreferencesData.get("serial.port"),
      PreferencesData.getInteger("serial.debug_rate", 9600),
      PreferencesData.getNonEmpty("serial.parity", "N").charAt(0),
      PreferencesData.getInteger("serial.databits", 8),
      PreferencesData.getFloat("serial.stopbits", 1),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableRTS"),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableDTR"));
  }

  public Serial(int irate) throws SerialException {
    this(PreferencesData.get("serial.port"), irate,
      PreferencesData.getNonEmpty("serial.parity", "N").charAt(0),
      PreferencesData.getInteger("serial.databits", 8),
      PreferencesData.getFloat("serial.stopbits", 1),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableRTS"),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableDTR"));
  }

  public Serial(String iname, int irate) throws SerialException {
    this(iname, irate, PreferencesData.getNonEmpty("serial.parity", "N").charAt(0),
      PreferencesData.getInteger("serial.databits", 8),
      PreferencesData.getFloat("serial.stopbits", 1),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableRTS"),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableDTR"));
  }

  public Serial(String iname) throws SerialException {
    this(iname, PreferencesData.getInteger("serial.debug_rate", 9600),
      PreferencesData.getNonEmpty("serial.parity", "N").charAt(0),
      PreferencesData.getInteger("serial.databits", 8),
      PreferencesData.getFloat("serial.stopbits", 1),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableRTS"),
      !BaseNoGui.getBoardPreferences().getBoolean("serial.disableDTR"));
  }

  public static boolean touchForCDCReset(String iname) throws SerialException {
    SerialPort serialPort = null;
    try {
      serialPort = SerialPort.getCommPort(iname);
      if (serialPort.openPort()) {
          serialPort.setComPortParameters(1200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
          serialPort.clearDTR​();
          serialPort.closePort();
          return true;
      } else {
          throw new SerialException(format(tr("Error touching serial port ''{0}''."), iname));
      }
    } catch (SerialPortInvalidPortException​ e) {
      throw new SerialException(format(tr("Error touching serial port ''{0}''."), iname), e);
    } finally {
      if (serialPort != null) {
        serialPort.closePort();
      }
    }
  }

  protected Serial(String iname, int irate, char iparity, int idatabits, float istopbits, boolean setRTS, boolean setDTR) throws SerialException {
    //if (port != null) port.close();
    //this.parent = parent;
    //parent.attach(this);

    resetDecoding(StandardCharsets.UTF_8);

    int parity = SerialPort.NO_PARITY;
    if (iparity == 'E') parity = SerialPort.EVEN_PARITY;
    if (iparity == 'O') parity = SerialPort.ODD_PARITY;

    int stopbits = SerialPort.ONE_STOP_BIT;
    if (istopbits == 1.5f) stopbits = SerialPort.ONE_POINT_FIVE_STOP_BITS;
    if (istopbits == 2) stopbits = SerialPort.TWO_STOP_BITS;

    int flowControl;
    if (setRTS) {
        flowControl = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
    } else if (setDTR) {
        flowControl = SerialPort.FLOW_CONTROL_DTR_ENABLED | SerialPort.FLOW_CONTROL_DSR_ENABLED;
    } else {
        flowControl = SerialPort.FLOW_CONTROL_DISABLED;
    }
 
    // This is required for unit-testing
    if (iname.equals("none")) {
      return;
    }

    try {
      port = SerialPort.getCommPort(iname);
    } catch (SerialPortInvalidPortException​ e) {
      if (iname.startsWith("/dev")) {
        throw new SerialException(format(tr("Error opening serial port ''{0}''. Try consulting the documentation at http://playground.arduino.cc/Linux/All#Permission"), iname));
      } else {
        throw new SerialException(format(tr("Error opening serial port ''{0}''."), iname), e);
      }
    }

    if (port == null) {
      throw new SerialNotFoundException(format(tr("Serial port ''{0}'' not found. Did you select the right one from the Tools > Serial Port menu?"), iname));
    } else if (port.openPort()) {
        port.setComPortParameters(irate, idatabits, stopbits, parity);
        port.setFlowControl​(flowControl);
        port.addDataListener(this);
    } else {
        throw new SerialException(format(tr("Error opening serial port ''{0}''."), iname));
    }
  }

  public void setup() {
    //parent.registerCall(this, DISPOSE);
  }

  public void dispose() throws IOException {
    if (port != null) {
      if (port.isOpen()) {
        if (!port.closePort()) {  // close the port
          throw new IOException(format(tr("Error opening serial port ''{0}''."), port.toString()));
        }
      }
      port = null;
    }
  }

  @Override
  public synchronized int getListeningEvents() {
    return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
  }

  @Override
  public synchronized void serialEvent(SerialPortEvent serialEvent) {
    if (serialEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
      byte[] buf = serialEvent.getReceivedData();
        int next = 0;
        while(next < buf.length) {
          while(next < buf.length && outToMessage.hasRemaining()) {
            int spaceInIn = inFromSerial.remaining();
            int copyNow = buf.length - next < spaceInIn ? buf.length - next : spaceInIn;
            inFromSerial.put(buf, next, copyNow);
            next += copyNow;
            inFromSerial.flip();
            bytesToStrings.decode(inFromSerial, outToMessage, false);
            inFromSerial.compact();
          }
          outToMessage.flip();
          if(outToMessage.hasRemaining()) {
            char[] chars = new char[outToMessage.remaining()];
            outToMessage.get(chars);
            message(chars, chars.length);
          }
          outToMessage.clear();
        }
    }
  }

  public void processSerialEvent(byte[] buf) {
    int next = 0;
    // This uses a CharsetDecoder to convert from bytes to UTF-8 in
    // a streaming fashion (i.e. where characters might be split
    // over multiple reads). This needs the data to be in a
    // ByteBuffer (inFromSerial, which we also use to store leftover
    // incomplete characters for the nexst run) and produces a
    // CharBuffer (outToMessage), which we then convert to char[] to
    // pass onwards.
    // Note that these buffers switch from input to output mode
    // using flip/compact/clear
    while (next < buf.length || inFromSerial.position() > 0) {
      do {
        // This might be 0 when all data was already read from buf
        // (but then there will be data in inFromSerial left to
        // decode).
        int copyNow = Math.min(buf.length - next, inFromSerial.remaining());
        inFromSerial.put(buf, next, copyNow);
        next += copyNow;

        inFromSerial.flip();
        bytesToStrings.decode(inFromSerial, outToMessage, false);
        inFromSerial.compact();

        // When there are multi-byte characters, outToMessage might
        // still have room, so add more bytes if we have any.
      } while (next < buf.length && outToMessage.hasRemaining());

      // If no output was produced, the input only contained
      // incomplete characters, so we're done processing
      if (outToMessage.position() == 0)
        break;

      outToMessage.flip();
      char[] chars = new char[outToMessage.remaining()];
      outToMessage.get(chars);
      message(chars, chars.length);
      outToMessage.clear();
    }
  }

  /**
   * This method is intented to be extended to receive messages
   * coming from serial port.
   */
  protected void message(char[] chars, int length) {
    // Empty
  }


  /**
   * This will handle both ints, bytes and chars transparently.
   */
  public void write(int what) {  // will also cover char
    byte[] bytes = new byte[]{(byte)(what & 0xff)};
    if (port.writeBytes(bytes, 1) < 0) {
      errorMessage("write");
    }
  }


  public void write(byte bytes[]) {
    if (port.writeBytes(bytes, bytes.length) < 0) {
      errorMessage("write");
    }
  }


  /**
   * Write a String to the output. Note that this doesn't account
   * for Unicode (two bytes per char), nor will it send UTF8
   * characters.. It assumes that you mean to send a byte buffer
   * (most often the case for networking and serial i/o) and
   * will only use the bottom 8 bits of each char in the string.
   * (Meaning that internally it uses String.getBytes)
   * <p>
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   */
  public void write(String what) {
    write(what.getBytes());
  }

  public void setDTR(boolean state) {
    boolean res;
    if (state) {
      res = port.setDTR();
    } else {
      res = port.clearDTR();
    }
    if (!res) {
      errorMessage("setDTR");
    }
  }

  public void setRTS(boolean state) {
    boolean res;
    if (state) {
      res = port.setRTS();
    } else {
      res = port.clearRTS();
    }
    if (!res) {
      errorMessage("setDTR");
    }
  }

  /**
   * Reset the encoding used to convert the bytes coming in
   * before they are handed as Strings to {@Link #message(char[], int)}.
   */
  public synchronized void resetDecoding(Charset charset) {
    bytesToStrings = charset.newDecoder()
                      .onMalformedInput(CodingErrorAction.REPLACE)
                      .onUnmappableCharacter(CodingErrorAction.REPLACE)
                      .replaceWith("\u2e2e");
  }

  static public List<String> list() {
    return SerialPortList.getPortNames();
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  private static void errorMessage(String where) {
    System.err.println(format(tr("Error inside Serial.{0}()"), where));
  }
}
