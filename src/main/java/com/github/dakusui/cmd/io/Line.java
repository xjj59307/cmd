package com.github.dakusui.cmd.io;

import com.github.dakusui.cmd.Command.SourceType;

public class Line {
	SourceType sourceType;
	String body;
	public Line(SourceType sourceType, String body) {
		this.sourceType = sourceType;
		this.body = body;
	}
	
	public String toString() {
		return String.format("%s %s", this.sourceType, body);
	}
}