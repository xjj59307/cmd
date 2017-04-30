package com.github.dakusui.cmd.io;

import com.github.dakusui.cmd.exceptions.CommandException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class LineReaderTest {

	private BasicLineReader createLineReader(String input) {
		return new BasicLineReader(
				Charset.defaultCharset(),
				new ByteArrayInputStream(
						input.getBytes(Charset.defaultCharset())
				)
		);
	}

	private BasicLineReader createLineReader(InputStream inputStream) {
		return new BasicLineReader(Charset.defaultCharset(), inputStream);
	}

	@Test
	public void readLines() throws CommandException {
		LineReader lineReader = createLineReader("Hello, world\nHi");
		assertEquals("Hello, world", lineReader.read());
		assertEquals("Hi", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test
	public void readOneLine() throws CommandException {
		LineReader lineReader = createLineReader("Hello, world");
		assertEquals("Hello, world", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test
	public void readOneLineWhoseLengthIsSameAsBufferLength() throws CommandException {
		LineReader lineReader = createLineReader("12345678901234567890");
		assertEquals("12345678901234567890", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test
	public void readOneLineFromStdoutOfShellCommandWithCustomReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		LineReader lineReader = createLineReader(proc.getInputStream());
		assertEquals("hello", lineReader.read());
		assertEquals(null, lineReader.read());
	}

	@Test
	public void readOneLineFromStdoutOfShellCommandWithBufferedReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		LineReader lineReader = createLineReader(proc.getInputStream());
		assertEquals("hello", lineReader.read());
		assertEquals(null, lineReader.read());
	}


	@Test
	public void test() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		InputStreamReader isr = new InputStreamReader(proc.getInputStream());
		int c;
		while ((c = isr.read()) != -1) {
			System.out.printf("[%s(%d)]", (char) c, c);
		}
		System.out.println();
		System.out.println((int) '\n');
	}

	@Test
	public void testBufferedReader() throws Exception {
		Process proc = Runtime.getRuntime().exec("echo hello");
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		System.out.println("[" + br.readLine() + "]");
		System.out.println("[" + br.readLine() + "]");
	}
}
