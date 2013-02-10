package jp.jka.cmd;

import jp.jka.cmd.CommandResult;
import jp.jka.cmd.CommandRunner;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class CommandRunnerTest {
	private static Logger LOGGER = LoggerFactory.getLogger(CommandRunnerTest.class);
	
	@Before
	public void before() {
		CommandRunner.removeAllListeners();
	}
	
	@Test
	public void test_00() throws Exception {
		LOGGER.info("test_00");
		TestCase.assertEquals("hello", CommandRunner.runLocal("echo hello").stdout());
	}

	@Test
	public void test_01() throws Exception {
		LOGGER.info("test_01 - 1");
		TestCase.assertEquals("hello", CommandRunner.runLocal("echo hello").stdout());
		LOGGER.info("test_01 - 2");
		TestCase.assertEquals("hello", CommandRunner.runLocal("echo hello").stdout());
	}

	@Test
	public void test_02() throws Exception {
		LOGGER.info("test_02");
		CommandResult result = CommandRunner.runLocal("cat NNN"); // non existing file
		TestCase.assertEquals("", result.stdout());
		TestCase.assertEquals("cat: NNN: No such file or directory", result.stderr());
		TestCase.assertEquals(1, result.exitCode());
	}

	@Test
	public void test_03() throws Exception {
		LOGGER.info("test_03");
		CommandResult result = CommandRunner.runLocal("echo WORLD");
		TestCase.assertEquals("WORLD", result.stdout());
	}
	
	@Test
	public void test_04() throws Exception {
		LOGGER.info("test_03");
		CommandResult result = CommandRunner.runLocal("echo WORLD >&2");
		TestCase.assertEquals("WORLD", result.stderr());
	}

	@Test(timeout=1000)
	public void test_05() throws Exception {
		LOGGER.info("test-05");
		CommandResult result = CommandRunner.runLocal("ftp", new String[]{"bye"}, new String[]{"bye"}); 
		TestCase.assertEquals("", result.stdout());
	}
	
	@Test(expected=TimeoutException.class)
	public void test_06() throws Exception {
		LOGGER.info("test-06");
		CommandResult result = CommandRunner.runLocal(1000, "sleep 10");
		LOGGER.debug("result={}", result);
	}

	@Test
	public void test_07() throws Exception {
		LOGGER.info("test-07");
		CommandResult result = CommandRunner.runLocal(2000, "sleep 1 && echo hi");
		TestCase.assertEquals("hi", result.asString());
	}
	
	
	@Test
	public void test_08() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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
		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_09() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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

		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_10() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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

		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_11() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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

		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test
	public void test_12() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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

		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=10000)
	public void test_13() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-13");
		String cmd = "cat /dev/zero | head -c 10000 | base64 -b 80";

		String expected = buildExpectedData(80, 54);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		CommandResult result = CommandRunner.runLocal(cmd);
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=5000)
	public void test_14() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-14");
		String cmd = "cat /dev/zero | head -c 100000 | base64 -b 80";

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandRunner.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}
	
	@Test(timeout=5000)
	public void test_15() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-15");
		String cmd = "cat /dev/zero | head -c 100000 | base64 -b 80 >&2";

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandRunner.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals("", result.stdout());
		TestCase.assertEquals(expected, result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=5000)
	public void test_16() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-16");
		String cmd = "cat /dev/zero | head -c 100000 | base64 -b 80 >&2 && cat /dev/zero | head -c 100000 | base64 -b 80";

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandRunner.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals(expected, result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=5000)
	public void test_17() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-17");
		String cmd = "cat /dev/zero | head -c 100000 | base64 -b 80 && cat /dev/zero | head -c 100000 | base64 -b 80 >&2";

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandRunner.runLocal(cmd);
		// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		// 123456789012345678901234567890123456789012345678901234
		TestCase.assertEquals("", result.stdout());
		TestCase.assertEquals(expected, result.stderr());
		TestCase.assertEquals(0,  result.exitCode());
	}

	@Test(timeout=60000)
	public void test_18() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-18");
		String cmd = "cat /dev/zero | head -c 10000000 | base64 -b 80";

		String expected = buildExpectedData(80, 54);
		CommandResult result = CommandRunner.runLocal(cmd);
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
	
	@Ignore
	@Test
	public void test_19() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-19");
		boolean finished = false;
		try {
			String userName = CommandRunner.runLocal("whoami").stdout();
			String hostName = CommandRunner.runLocal("hostname").stdout();
			String privKey  = String.format("%s/.ssh/id_rsa", CommandRunner.runLocal("echo $HOME").stdout());
			
			CommandResult result = CommandRunner.runRemote(userName, hostName, privKey, "echo hello");
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

	@Ignore
	@Test
	public void test_20() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		LOGGER.info("test-20");
		String userName = CommandRunner.runLocal("whoami").stdout();
		String hostName = CommandRunner.runLocal("hostname").stdout();
		
		CommandResult result = CommandRunner.runRemote(userName, hostName, null, "echo hello");
		TestCase.assertEquals("hello", result.stdout());
		TestCase.assertEquals("", result.stderr());
		TestCase.assertEquals(0, result.exitCode());
	}

	static int PID;
	
	
	@Test(expected=TimeoutException.class)
	public void test_21() throws Exception {
		LOGGER.info("test-21");
		PID = -1;
		CommandRunner.addListener(new CommandListener() {
			@Override
			public void exec(Command command) {
				int pid = command.pid();
				LOGGER.debug("pid={}", pid);
				PID = pid;
			}
		});
		try {
			CommandRunner.runLocal(1000, "sleep 10");
			TestCase.fail("The command didn't time out in 1 sec!");
		} finally {
			Thread.sleep(100);
			CommandResult result;
			result = CommandRunner.runLocal(String.format("ps -o pid= -p %s", PID));
			TestCase.assertEquals("", result.stdout()); // make sure the process is killed.
			TestCase.assertEquals(1, result.exitCode());
		}
	}

	@Ignore
	@Test(expected=TimeoutException.class)
	public void test_22() throws Exception {
		LOGGER.info("test-22");
		PID = -1;
		CommandRunner.addListener(new CommandListener() {
			@Override
			public void exec(Command command) {
				int pid = command.pid();
				LOGGER.debug("pid={}", pid);
				PID = pid;
			}
		});
		try {
			String userName = CommandRunner.runLocal("whoami").stdout();
			String hostName = CommandRunner.runLocal("hostname").stdout();
			
			CommandRunner.runRemote(1000, userName, hostName, null, "sleep 10");
			TestCase.fail("The command didn't time out in 1 sec!");
		} finally {
			Thread.sleep(100);
			CommandResult result;
			result = CommandRunner.runLocal(String.format("ps -o pid= -p %s", PID));
			TestCase.assertEquals("", result.stdout()); // make sure the process is killed.
			TestCase.assertEquals(1, result.exitCode());
		}
	}
}

