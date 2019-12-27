package com.hoc.model;

import java.util.List;

public class Result2 implements Result {
  public final List<Room> rooms;
  public final Examiner examiner;

  public Result2(List<Room> rooms, Examiner examiner) {
    this.rooms = rooms;
    this.examiner = examiner;
  }
}
