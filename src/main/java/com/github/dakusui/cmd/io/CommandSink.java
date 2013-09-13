package com.github.dakusui.cmd.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dakusui.cmd.Command.SourceType;

public class CommandSink {
	private static Logger LOGGER = LoggerFactory.getLogger(CommandSink.class);
	static char[] EOL = System.getProperty("line.separator").toCharArray();
	private int numLines = 0;
	Line[] ring;
	int    cur;

	public CommandSink(int i) {
		ring = new Line[i];
		cur = 0;
	}
	
	public void write(Line l) {
		LOGGER.trace("{} {}", l.sourceType, l.body);
		writeEngine(l);
		numLines++;
	}
	
	public void writeEngine(Line l) {
		ring[this.cur++] = l;
		this.cur = this.cur % ring.length;
	}
	
	
	public String asString(SourceType sourceType) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < this.ring.length; i++) {
			Line l = this.ring[(this.cur + i) % this.ring.length];
			if (l == null) continue;
			if (sourceType == null || l.sourceType == sourceType) {
				b.append(l.body);
				if (i != this.ring.length - 1) {
					b.append(EOL);
				}
			}
		}
		return b.toString();
	}
	
	public int numLines() {
		return this.numLines;
	}
	
	public String toString() {
		return asString(null);
	}
}