package com.github.dakusui.streamablecmd.exceptions;

abstract class BaseException extends RuntimeException {
  BaseException(String message) {
    super(message);
  }
}
