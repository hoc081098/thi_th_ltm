package com.hoc.model;

import java.io.Serializable;

public class Buffer implements Serializable {
  public final byte[] bytes;

  public Buffer(byte[] bytes) {
    this.bytes = bytes;
  }
}
