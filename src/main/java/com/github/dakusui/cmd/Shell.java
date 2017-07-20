package com.github.dakusui.cmd;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Shell {
  String program();

  List<String> options();

  default String format() {
    return String.format("%s %s", program(), String.join(" ", options()));
  }

  static Shell local() {
    return new Builder.ForLocal().build();
  }

  static Shell ssh(String user, String host) {
    return ssh(user, host, null);
  }

  static Shell ssh(String user, String host, String identity) {
    return new Builder.ForSsh(host).userName(user).identity(identity).build();
  }

  class Impl implements Shell {
    private final String       program;
    private final List<String> options;

    Impl(String program, List<String> options) {
      this.program = program;
      this.options = options;
    }

    public String program() {
      return program;
    }

    public List<String> options() {
      return options;
    }

    public String toString() {
      return format();
    }
  }

  @SuppressWarnings("WeakerAccess")
  abstract class Builder<B extends Builder> {
    private String program;
    private List<String> options = new LinkedList<>();

    @SuppressWarnings("unchecked")
    public B withProgram(String program) {
      this.program = Objects.requireNonNull(program);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B clearOptions() {
      this.options.clear();
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addOption(String option) {
      this.options.add(option);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addOption(@SuppressWarnings("SameParameterValue") String option, String value) {
      this.options.add(option);
      this.options.add(value);
      return (B) this;
    }

    String getProgram() {
      return this.program;
    }

    List<String> getOptions() {
      return this.options;
    }

    public Shell build() {
      Objects.requireNonNull(this.program);
      return new Impl(getProgram(), this.getOptions());
    }

    @SuppressWarnings("unchecked")
    public B addAllOptions(List<String> options) {
      options.forEach(this::addOption);
      return (B) this;
    }

    @SuppressWarnings("WeakerAccess")
    public static class ForLocal extends Builder<Builder.ForLocal> {
      public ForLocal() {
        this.withProgram("sh")
            .addOption("-c");
      }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ForSsh extends Builder<Builder.ForSsh> {
      private       String userName;
      private final String hostName;
      private String identity = null;

      public ForSsh(String hostName) {
        this.hostName = Objects.requireNonNull(hostName);
        this.withProgram("ssh")
            .addOption("-o", "PasswordAuthentication=no")
            .addOption("-o", "StrictHostkeyChecking=no");
      }

      public ForSsh userName(String userName) {
        this.userName = userName;
        return this;
      }

      public ForSsh identity(String identity) {
        this.identity = identity;
        return this;
      }

      List<String> getOptions() {
        return Stream.concat(
            super.getOptions().stream(),
            Stream.concat(
                composeIdentity().stream(),
                Stream.of(
                    composeAccount()
                )
            )
        ).collect(toList());
      }

      List<String> composeIdentity() {
        return identity == null ?
            Collections.emptyList() :
            asList("-i", identity);
      }

      String composeAccount() {
        return userName == null ?
            hostName :
            String.format("%s@%s", userName, hostName);
      }
    }
  }
}
