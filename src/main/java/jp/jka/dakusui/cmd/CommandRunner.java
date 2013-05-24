package jp.jka.dakusui.cmd;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class CommandRunner {
	private static final String[] LOCAL_SHELL = new String[]{"sh", "-c"};

	private static List<CommandListener> listeners = new LinkedList<CommandListener>();
	
	private CommandRunner() {
	}
	
	public static CommandResult run(int timeout, String[] execShell, String[] stopShell, String command, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Command cmd = new Command(execShell, command, stopShell, listeners);
		return cmd.exec(timeout, stdins);
	}
	public static CommandResult runLocal(int timeout, String command, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		return run(timeout, LOCAL_SHELL, LOCAL_SHELL, command, stdins);
	}
	
	public static CommandResult runLocal(String command, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException { 
		return runLocal(-1, command, stdins);
	}
	
	public static CommandResult runRemote(String userName, String hostName, String privKeyFile, String command, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		return runRemote(-1, userName, hostName, privKeyFile, command, stdins);
	}

	public static CommandResult runRemote(int timeout, String userName, String hostName, String privKeyFile, String command, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		if (privKeyFile == null) {
			return run(
					timeout, 
					new String[]{
							"ssh", "-o", "StrictHostKeyChecking=no", 
							String.format("%s@%s", userName, hostName)
					},
					LOCAL_SHELL,
					command, 
					stdins
			);
		}
		
		return run(
				timeout, 
				new String[]{
						"ssh", "-i", privKeyFile, 
						"-o", "StrictHostKeyChecking=no", 
						String.format("%s@%s", userName, hostName)
				},
				LOCAL_SHELL,
				command, 
				stdins
		);
	}
	
	public static void addListener(CommandListener l) {
		listeners.add(l);
	}
	
	public static void removeListener(int i) {
		listeners.remove(i);
	}
	
	public static CommandListener getListener(int i) {
		return listeners.get(i);
	}
	
	public static int numListeners() {
		return listeners.size();
	}
	
	public static void removeAllListeners() {
		listeners.clear();
	}
}
