package com.github.dakusui.cmd.io;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.streamablecmd.exceptions.Exceptions;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.charset.Charset;

public class BasicLineReader implements LineReader {
  /*
   * This library doesn't respect the second byte of EOL string of the platform.
   */
  static final char              EOL      = System.getProperty("line.separator").toCharArray()[0];
  private      InputStreamReader isReader = null;
  private      BufferedReader    bReader  = null;
  private      char[]            buf      = null;
  private int maxLineSize;

  /**
   * Creates an object of this class.
   *
   * @param cs          Character set that is assumed for the inputstream <code>is</code>.
   * @param maxLineSize
   * @param is
   */
  public BasicLineReader(Charset cs, int maxLineSize, InputStream is) {
    this.maxLineSize = maxLineSize;
    this.isReader = new InputStreamReader(new BufferedInputStream(is), cs);
    if (maxLineSize <= 0) {
      this.bReader = new BufferedReader(this.isReader);
    }
  }

  @Override
  public String read() throws CommandException {
    String ret = null;
    try {
      if (this.maxLineSize <= 0) {
        ////
        // BufferedReader mode.
        ret = bReader.readLine();
      } else {
        ////
        // Custom mode
        if (buf == null) {
          this.buf = new char[this.maxLineSize];
          int len = this.isReader.read(this.buf);
          if (len == -1)
            return ret;
          if (len < this.buf.length) {
            this.buf = ArrayUtils.subarray(this.buf, 0, len);
          }
        }
        ret = readFromBufferAndUpdate();
      }
    } catch (IOException e) {
      throw Exceptions.wrap(e);
    }
    return ret;
  }

  private String readFromBufferAndUpdate() {
    int indexOfEOL = indexOfEOL(this.buf);
    String ret = null;
    if (indexOfEOL == -1)
      indexOfEOL = this.buf.length;
    ret = new String(ArrayUtils.subarray(this.buf, 0, indexOfEOL));
    ////
    // If an EOL is at the end of the array, an empty string will be the
    // last element from the stream.
    if (indexOfEOL == this.buf.length) {
      this.buf = null;
    } else {
      this.buf = ArrayUtils.subarray(this.buf, indexOfEOL + 1, this.buf.length);
      if (this.buf.length == 0) {
        this.buf = null;
      }
    }
    return ret;
  }

  private int indexOfEOL(char[] buf) {
    return ArrayUtils.indexOf(buf, EOL);
  }

  @Override
  public void close() throws CommandException {
    if (this.bReader != null)
      try {
        bReader.close();
      } catch (IOException e) {
        throw Exceptions.wrap(e);
      } finally {
        if (this.isReader != null)
          try {
            isReader.close();
          } catch (IOException e) {
            throw Exceptions.wrap(e);
          }
      }
  }
}
