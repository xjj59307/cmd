package com.github.dakusui.cmd.io;

/**
 * This class is not necessary anymore.
 *
 * @see com.github.dakusui.cmd.Cmd
 */
@Deprecated
public class RingBufferedLineWriter implements LineWriter {
  private String[] ringBuffer;
  private int      next;

  public RingBufferedLineWriter(int size) {
    if (size <= 0)
      throw new IllegalArgumentException("Size must be greater than 0 but given was: " + size);
    this.ringBuffer = new String[size];
    this.next = 0;
  }

  @Override
  public void write(String line) {
    this.ringBuffer[this.next] = line;
    this.next = (this.next + 1) % this.ringBuffer.length;
  }

  public String asString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < ringBuffer.length; i++) {
      String s = ringBuffer[(this.next + i) % ringBuffer.length];
      if (s != null) {
        b.append(s);
        ////
        // Elements but the last one are followed by '\n'.
        if (i != ringBuffer.length - 1)
          b.append("\n");
      }
    }
    return b.toString();
  }
}
