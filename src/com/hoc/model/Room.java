package com.hoc.model;

public class Room {
  public final String name;

  public Room(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "model.Room{" +
      "name='" + name + '\'' +
      '}';
  }
}
