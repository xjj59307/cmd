package com.github.dakusui.cmd.io;

public class RingBufferedLineWriter implements LineWriter {

	private String[] ringBuffer;

	private int next;

	public RingBufferedLineWriter(int size) {
		if (size <= 0) throw new IllegalArgumentException(String.format("size must be greater than 0. (%d)", size));
		this.ringBuffer = new String[size];
		this.next = 0;
	}

	@Override
	public void write(String line) {
		this.ringBuffer[this.next] = line;
		this.next = (this.next + 1) % this.ringBuffer.length;
	}

	@Override
	public void finish() {}

	public String asString() {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < ringBuffer.length; i++) {
			String s = ringBuffer[(this.next + i) % ringBuffer.length];
			if (s != null) {
				b.append(s);
				// Elements but the last one are followed by '\n'.
				if (i != ringBuffer.length -1) b.append("\n");
			}
		}
		return b.toString();
	}
}
