package com.github.dakusui.cmd;

import java.net.MalformedURLException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.github.dakusui.cmd.io.LineReaderTest;
import com.github.dakusui.cmd.io.RingBufferedLineWriterTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	LineReaderTest.class,
	RingBufferedLineWriterTest.class,
	CommandRunnerTest.class
})public class All {
	@BeforeClass public static void loadAllClassesUnderTests() throws MalformedURLException {
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage("com.github.dakusui"))
				.setScanners(new SubTypesScanner(false))
		);
		Set<?> modules = reflections.getSubTypesOf(Object.class);
		System.out.printf("Loaded %d classes:\n", modules.size());
		for (Object klass : modules) {
			System.out.println("    " + klass);
		}
	}
}
