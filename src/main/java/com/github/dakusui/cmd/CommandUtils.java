package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;

import java.util.stream.Stream;

public enum CommandUtils {
	;
	private static final String[] LOCAL_SHELL = new String[]{"sh", "-c"};

	public static CommandResult run(int timeout, String[] execShell, String command) throws CommandException {
		return new Command(execShell, command).exec(timeout);
	}

	public static Stream<String> runAsnyc(String command) throws CommandException {
		return new Command(LOCAL_SHELL, command).execAsync();
	}

	public static CommandResult runLocal(int timeout, String command) throws CommandException {
		return run(timeout, LOCAL_SHELL, command);
	}

	public static CommandResult runLocal(String command) throws CommandException {
		return runLocal(-1, command);
	}

	public static CommandResult runRemote(String userName, String hostName, String privKeyFile, String command) throws CommandException {
		return runRemote(-1, userName, hostName, privKeyFile, command);
	}

	public static CommandResult runRemote(int timeout, String userName, String hostName, String privKeyFile, String command) throws CommandException {
		if (privKeyFile == null) {
			return run(
					timeout,
					new String[]{
							"ssh", "-o", "StrictHostKeyChecking=no",
							String.format("%s@%s", userName, hostName)
					},
					command
			);
		}

		return run(
				timeout,
				new String[]{
						"ssh", "-i", privKeyFile,
						"-o", "StrictHostKeyChecking=no",
						String.format("%s@%s", userName, hostName)
				},
				command
		);
	}
}
