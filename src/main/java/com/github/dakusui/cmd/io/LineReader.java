package com.github.dakusui.cmd.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.lang3.ArrayUtils;

import com.github.dakusui.cmd.exceptions.CommandException;

public class LineReader {
	/*
	 * This library doesn't respect the second byte of EOL string of the platform.
	 */
	private static final char EOL = CommandSink.EOL[0]; 
	private InputStreamReader reader;
	private char[] buf = null;
	private int maxLineSize;
	/**
	 * Creates an object of this class.
	 * 
	 * @param cs Character set that is assumed for the inputstream <code>is</code>.
	 * @param maxLineSize
	 * @param is
	 */
	public LineReader(Charset cs, int maxLineSize, InputStream is) {
		this.maxLineSize = maxLineSize;
		this.reader = new InputStreamReader(new BufferedInputStream(is), cs);
	}
	
	public String read() throws CommandException {
		String ret = null;
		int wait = 0;
		////
		// On Mac, without this retry logic, this method blocks when we 
		// read a big data (> 10kb).
		while (wait <= 12 && (ret = read(wait)) == null) {
			wait += 4;
		}
		return ret;
	}
	
	private String read(int wait) throws CommandException {
		String ret = null;
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			throw new CommandException(e);
		}
		try {
			if (this.reader.ready()) ret = readLine(reader);
		} catch (IOException e) {
			throw new CommandException(e);
		}
		return ret;
	}
	
	private String readLine(InputStreamReader reader) throws CommandException {
		String ret = null;
		if (buf == null) {
			this.buf = new char[this.maxLineSize];
			try {
				reader.read(this.buf);
			} catch (IOException e) {
				throw new CommandException(e);
			}
		}
		ret = readFromBufferAndUpdate();
		return ret;
	}
	
	private String readFromBufferAndUpdate() {
		int indexOfEOL = indexOfEOL(this.buf);
		String ret = null;
		ret = new String(ArrayUtils.subarray(this.buf, 0, indexOfEOL));
		if (indexOfEOL == -1 || indexOfEOL == this.buf.length - 1) {
			this.buf = null;
		} else {
			this.buf = ArrayUtils.subarray(this.buf, indexOfEOL + 1, this.buf.length);
		}
		return ret;
	}
	
	private int indexOfEOL(char[] buf) {
		return ArrayUtils.indexOf(buf, EOL);
	}
}
