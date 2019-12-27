package com.hoc.model;

@FunctionalInterface
public interface CheckedFunction<T, R> {
  R apply(T t) throws Exception;

  default <U> CheckedFunction<T, U> andThen(CheckedFunction<R, U> other) {
    return t -> other.apply(apply(t));
  }
}