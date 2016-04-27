package com.github.dakusui.cmd;

import static java.lang.String.format;
import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dakusui.cmd.exceptions.CommandTimeoutException;

public class CommandRunnerTest {
	private static Logger LOGGER = LoggerFactory.getLogger(CommandRunnerTest.class);
	
	protected void assertCommandResult(String stdout, String stderr, String stdouterr, int exitCode, CommandResult result) {
		Assert.assertEquals(stdout,    result.stdout());
		Assert.assertEquals(stderr,    result.stderr());
		Assert.assertEquals(stdouterr, result.stdouterr());
		Assert.assertEquals(exitCode,  result.exitCode());
	}

	@Test
	public void runLocal_echo_hello() throws Exception {
		CommandResult result = CommandUtils.runLocal("echo hello");
		assertCommandResult("hello", "", "hello", 0, result);
	}

	@Test
	public void runLocal_echo_hello_$$_echo_hello() throws Exception {
		CommandResult result = CommandUtils.runLocal("echo hello && echo hello");
		assertCommandResult("hello\nhello", "", "hello\nhello", 0, result);
	}

	@Test
	public void runLocal_echo_hello_twice() throws Exception {
		CommandResult result;
		LOGGER.info("stage - 1");
		result = CommandUtils.runLocal("echo hello");
		assertCommandResult("hello", "", "hello", 0, result);
		LOGGER.info("stage - 2");
		result = CommandUtils.runLocal("echo hello");
		assertCommandResult("hello", "", "hello", 0, result);
	}

	@Test
	public void runLocal_execFailingCommand() throws Exception {
		CommandResult result;
		// non existing file "NNN"
		result = CommandUtils.runLocal("cat NNN");
		assertCommandResult(
				"", 
				"cat: NNN: No such file or directory", 
				"cat: NNN: No such file or directory", 
				1, 
				result
		);
	}

	@Test
	public void runLocal_echo_WORLD_tostderr() throws Exception {
		CommandResult result;
		LOGGER.info("test_03");
		result = CommandUtils.runLocal("echo WORLD >&2");
		assertCommandResult("", "WORLD", "WORLD", 0, result);
	}

	@Test(expected=CommandTimeoutException.class)
	public void runLocal_with_1000msec_timesout_expectedly() throws Exception {
		LOGGER.info("test-06");
		CommandResult result = CommandUtils.runLocal(1000, "sleep 10");
		LOGGER.debug("result={}", result);
	}

	@Test
	public void runLocal_sleep1_$$_echo_hi_MakeSureLongCommandWorksCorrectly() throws Exception {
		LOGGER.info("test-07");
		CommandResult result = CommandUtils.runLocal("sleep 1 && echo hi");
		assertCommandResult("hi", "", "hi", 0, result);
	}
	
	
	@Test
	public void test_08() throws Exception {
		LOGGER.info("test-08");
		String cmd = "";
		for (int i = 100; i > 0; i--) {
			cmd += "echo " + i;
			if (i != 1) {
				cmd += " && ";
			}
		}
		
		String expected = "";
		for (int i = 100; i > 0; i--) {
			expected += i;
			if (i != 1) {
				expected += System.getProperty("line.separator");
			}
		}
		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_09() throws Exception {
		LOGGER.info("test-09");
		String cmd = "";
		for (int i = 101; i > 0; i--) {
			cmd += "echo " + i;
			if (i != 1) {
				cmd += " && ";
			}
		}
		
		String expected = "";
		for (int i = 100; i > 0; i--) {
			expected += i;
			if (i != 1) {
				expected += System.getProperty("line.separator");
			}
		}

		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_10() throws Exception {
		LOGGER.info("test-10");
		String cmd = "";
		for (int i = 99; i > 0; i--) {
			cmd += "echo " + i;
			if (i != 1) {
				cmd += " && ";
			}
		}
		
		String expected = "";
		for (int i = 99; i > 0; i--) {
			expected += i;
			if (i != 1) {
				expected += System.getProperty("line.separator");
			}
		}

		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_11() throws Exception {
		LOGGER.info("test-11");
		String cmd = "";
		for (int i = 200; i > 0; i--) {
			cmd += "echo " + i;
			if (i != 1) {
				cmd += " && ";
			}
		}
		
		String expected = "";
		for (int i = 100; i > 0; i--) {
			expected += i;
			if (i != 1) {
				expected += System.getProperty("line.separator");
			}
		}

		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_12() throws Exception {
		LOGGER.info("test-12");
		String cmd = "";
		for (int i = 201; i > 0; i--) {
			cmd += "echo " + i;
			if (i != 1) {
				cmd += " && ";
			}
		}
		
		String expected = "";
		for (int i = 100; i > 0; i--) {
			expected += i;
			if (i != 1) {
				expected += System.getProperty("line.separator");
			}
		}

		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=10000)
	public void test_13() throws Exception {
		LOGGER.info("test-13");
		String cmd = format("cat /dev/zero | head -c 10000 | %s 80", base64());

		String expected = buildExpectedData(80, 54);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		CommandResult result = CommandUtils.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=5000)
	public void test_14() throws Exception {
		LOGGER.info("test-14");
		String cmd = format("cat /dev/zero | head -c 100000 | %s 80", base64());

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandUtils.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=5000)
	public void test_15() throws Exception {
		LOGGER.info("test-15");
		String cmd = format("cat /dev/zero | head -c 100000 | %s 80 >&2", base64());

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandUtils.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals("", result.stdout());
		TestCase.assertEquals(expected, result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=5000)
	public void runLocal_outputLargeDataToBothStdoutAndStderr_1() throws Exception {
		String cmd = format("cat /dev/zero | head -c 100000 | %s 80 >&2 && cat /dev/zero | head -c 100000 | %s 80", base64(), base64());
		System.out.println(cmd);
		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandUtils.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals(expected, result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=5000)
	public void runLocal_outputLargeDataToBothStdoutAndStderr_2() throws Exception {
		String cmd = format("cat /dev/zero | head -c 100000 | %s 80 && cat /dev/zero | head -c 100000 | %s 80 >&2", base64(), base64());

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandUtils.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals(expected, result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=60000)
	public void runLocal_output10MdataToStdout() throws Exception {
		LOGGER.info("test-18");
		String cmd = format("cat /dev/zero | head -c 10000000 | %s 80", base64());

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandUtils.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	private String buildExpectedData(int numCharsPerOneLine, int numCharsInLastLine) {
		StringBuffer b = new StringBuffer();
		for (int i = 1; i < 100; i++) {
			b.append(buildExpectedDataOneLine(numCharsPerOneLine));
			b.append(System.getProperty("line.separator"));
		}
		b.append(buildExpectedDataOneLine(numCharsInLastLine));
		b.append("==");
		return b.toString();
	}
	
	private String buildExpectedDataOneLine(int numChars) {
		StringBuffer b = new StringBuffer(numChars * 2);
		for (int i = 0; i < numChars; i++) {
			b.append("A");
		}
		return b.toString();
	}
	
	@Test
	public void test_19() throws Exception {
		LOGGER.info("test-19");
		boolean finished = false;
		try {
			String userName = CommandUtils.runLocal("whoami").stdout();
			String hostName = CommandUtils.runLocal("hostname").stdout();
			String privKey  = String.format("%s/.ssh/id_rsa", CommandUtils.runLocal("echo $HOME").stdout());
			
			CommandResult result = CommandUtils.runRemote(userName, hostName, privKey, "echo hello");
			TestCase.assertEquals("hello", result.stdout());
			TestCase.assertEquals("", result.stderr());
			TestCase.assertEquals(0, result.exitCode());
			finished = true;
		} finally {
			if (!finished) {
				LOGGER.error("In order to make this test pass, you have to set up your passphraseless ssh key");
			}
		}
	}

	@Test
	public void test_20() throws Exception {
		LOGGER.info("test-20");
		String userName = CommandUtils.runLocal("whoami").stdout();
		String hostName = CommandUtils.runLocal("hostname").stdout();
		
		CommandResult result = CommandUtils.runRemote(userName, hostName, null, "echo hello");
		TestCase.assertEquals("hello", result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0, result.exitCode());
	}

	static int PID;
	
	
	@Test(expected=CommandTimeoutException.class)
	public void test_21() throws Exception {
		LOGGER.info("test-21");
		PID = -1;
		try {
			CommandUtils.runLocal(1000, "sleep 10");
			TestCase.fail("The command didn't time out in 1 sec!");
		} finally {
			Thread.sleep(100);
			CommandResult result;
			result = CommandUtils.runLocal(String.format("ps -o pid= -p %s", PID));
			TestCase.assertEquals("", result.stdout()); // make sure the process is killed.
			TestCase.assertEquals(1, result.exitCode());
		}
	}

	@Test(expected=CommandTimeoutException.class)
	public void test_22() throws Exception {
		LOGGER.info("test-22");
		PID = -1;
		try {
			String userName = CommandUtils.runLocal("whoami").stdout();
			String hostName = CommandUtils.runLocal("hostname").stdout();
			
			CommandUtils.runRemote(1000, userName, hostName, null, "sleep 10");
			TestCase.fail("The command didn't time out in 1 sec!");
		} finally {
			Thread.sleep(100);
			CommandResult result;
			result = CommandUtils.runLocal(String.format("ps -o pid= -p %s", PID));
			TestCase.assertEquals("", result.stdout()); // make sure the process is killed.
			TestCase.assertEquals(1, result.exitCode());
		}
	}
	
	static String systemName() {
		return System.getProperty("os.name");
	}
	
	static String base64() {
		String systemName = systemName();
		String ret;
		if ("Linux".equals(systemName)) {
			ret = "base64 -w";
		} else if ("Mac OS X".equals(systemName)) {
			ret = "base64 -b";
		} else {
			throw new RuntimeException(String.format("%s is not a supported platform.", systemName));
		}
		return ret;
	}
}

