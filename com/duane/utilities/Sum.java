package com.duane.utilities;

public class Sum extends Statistic
{
 public void noteValue(double value)
 {
  if (Debug)System.err.println("Sum.noteValue{"
                              +"value=" +value                     +"}");
  if (Utilities.isNaN(Value)) Value =value;
  else                        Value+=value;
  if (Debug)System.err.println("Sum.noteValue note value{"
                              +"value="         +value              +"\t"
                              +"Value="         +Value              +"}");
 }
 public Sum(){super();}
}

