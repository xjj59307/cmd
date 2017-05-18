cmdstreamer
=============

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

Enjoy.


