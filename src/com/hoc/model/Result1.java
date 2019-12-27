package com.hoc.model;

public class Result1 implements Result {
  public final Examiner examiner;
  public final Room room;
  public final int role;

  public Result1(Examiner examiner, Room room, int role) {
    this.examiner = examiner;
    this.room = room;
    this.role = role;
  }
}
