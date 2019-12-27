package com.hoc.model;

public class Pair<T, R> {
  public final T first;
  public final R second;

  public Pair(T t, R second) {
    this.first = t;
    this.second = second;
  }
}
