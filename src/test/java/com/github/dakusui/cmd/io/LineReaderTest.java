package com.github.dakusui.cmd.io;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.junit.Test;

import com.github.dakusui.cmd.exceptions.CommandException;

public class LineReaderTest {

	protected BasicLineReader createLineReader(int bufferSize, String input) {
		return createLineReader(
				bufferSize, 
				new ByteArrayInputStream(
						input.getBytes(Charset.defaultCharset())
				)
		);
	}
	
	protected BasicLineReader createLineReader(int bufferSize, InputStream is) {
		return new BasicLineReader(
				Charset.defaultCharset(), 
				bufferSize, 
				is);
	}
	
	@Test public void readLines() throws CommandException {
		LineReader lineReader = createLineReader(20, "Hello, world\nHi");
		assertEquals("Hello, world", lineReader.read());
		assertEquals("Hi", lineReader.read());
		assertEquals(null, lineReader.read());
	}
	
	@Test public void readOneLine() throws CommandException {
		LineReader lineReader = createLineReader(20, "Hello, world");
		assertEquals("Hello, world", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test public void readOneLineWhoseLengthIsSameAsBufferLength() throws CommandException {
		LineReader lineReader = createLineReader(20, "12345678901234567890");
		assertEquals("12345678901234567890", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test public void readOneLineWhoseLengthIsLongerBufferLength() throws CommandException {
		////
		// BasicLineReader splits a line longer than its buffer.
		LineReader lineReader = createLineReader(20, "123456789012345678901");
		assertEquals("12345678901234567890", lineReader.read());
		assertEquals("1", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test public void readOneLineWhoseLengthIsLongerThannBufferLength() throws CommandException {
		////
		// BasicLineReader splits a line longer than its buffer.
		LineReader lineReader = createLineReader(20, "12345678901234567890123456789012345678901");
		assertEquals("12345678901234567890", lineReader.read());
		assertEquals("12345678901234567890", lineReader.read());
		assertEquals("1", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test public void readOneLineFromStdoutOfShellCommandWithCustomReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		LineReader lineReader = createLineReader(20, proc.getInputStream());
		assertEquals("hello", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test public void readOneLineFromStdoutOfShellCommandWithBufferedReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		LineReader lineReader = createLineReader(0, proc.getInputStream());
		assertEquals("hello", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	
	@Test public void test() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		InputStreamReader isr = new InputStreamReader(proc.getInputStream());
		int c;
		while ((c = isr.read()) != -1) {
			System.out.printf("[%s(%d)]", (char)c, c);
		}
		System.out.println();
		System.out.println((int)'\n');
	}

	@Test public void testBufferedReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		System.out.println("[" + br.readLine() + "]");
		System.out.println("[" + br.readLine() + "]");
	}
}
