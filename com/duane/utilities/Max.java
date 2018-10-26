package com.duane.utilities;

public class Max extends Statistic
{
 public Max(){}

 public void noteValue(double value)
 {
  if (Utilities.isNaN(Value))Value = value;
  Value=Value>=value?Value:value;
 }
}


