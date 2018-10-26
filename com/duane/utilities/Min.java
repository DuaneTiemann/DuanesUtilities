package com.duane.utilities;

public class Min extends Statistic
{
 public Min(){super();}

 public void noteValue(double value)
 {
  if (Debug)System.err.println("Min.noteValue{"
                              +"value="+value+"}");
  if (Utilities.isNaN(Value))Value=value;
  Value=Value<=value?Value:value;
 }
}

