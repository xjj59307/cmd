# cmdstreamer

A library for Java 8 to run a shell command using a stream easily on Unix platforms. 

Creating a program that executes a shell command from Java is a tedious task but 
there are a lot of pitfalls.

This library does it on behalf of you. To run '''echo hello''', you can simply do

```java
    Cmd.run(Shell.local(), "echo hello").forEach(System.out::println);
```

To do it over ```ssh```, do

```java
    Cmd.run(Shell.ssh("yourName", "yourHost"), "echo hello").forEach(System.out::println);
```

If you want to specity an identity file (ssh key), you can do

```java
    Cmd.run(Shell.ssh("yourName", "yourHost", "/home/yourName/.ssh/id_rsa"), "echo hello").forEach(System.out::println);
```

Enjoy.

# Installation

**cmdstreamer** requires Java SE8 or later. Following is a maven coordinate for it. 

```xml

  <dependency>
    <groupId>com.github.dakusui</groupId>
    <artifactId>cmdstreamer</artifactId>
    <version>[0.2.0,)</version>
  </dependency>
```

# Usage

```
   +--------------------+
   | Cmd                |       +-------+
   +--------------------+<>---->| Shell |
   |run():Stream<String>|  1  1 +-------+
   +--------------------+

```

## Redirection
(t.b.d.)

## Writing to a process's stdin
A ```cmd```
(t.b.d)

# Design
(t.b.d.)

# Future works
* Implement a timeout feature
* Provide a compatibility with ```commandrunner``` library

# References
* [0] "commandrunner"

[0]: https://github.com/xjj59307/commandrunner


