package com.github.dakusui.cmd.exceptions;

abstract class BaseException extends RuntimeException {
  BaseException(String message) {
    super(message);
  }
}
