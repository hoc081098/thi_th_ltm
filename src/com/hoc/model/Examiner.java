package com.hoc.model;

public class Examiner {
  public final String id;
  public final String name;
  public final String birthday;
  public final String unit;

  public Examiner(String id, String name, String birthday, String unit/*, String note*/) {
    this.id = id;
    this.name = name;
    this.birthday = birthday;
    this.unit = unit;
  }

  @Override
  public String toString() {
    return "model.Examiner{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", birthday='" + birthday + '\'' +
      ", unit='" + unit + '\'' +
      '}';
  }
}
