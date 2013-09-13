package com.github.dakusui.cmd.io;

public class Example {
	public static void main(String[] args) {
		for (char c : CommandSink.EOL) {
			int i = c;
			System.out.printf("[%d]\n", i);
		}
	}
}
