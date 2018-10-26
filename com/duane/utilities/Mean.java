package com.duane.utilities;

import java.util.ArrayList;

public class Mean extends Sum
{
 private long N = 0;

 public Mean(){super();}

 @Override
 public double getResult(){return getResult_(N);}

 public double getResult(long count){return getResult_(count);}

 private double getResult_(long count)
 {
  if (Debug)System.err.println("Mean.getResult{"
                              +"count="+count+"\t"
                              +"Value="+Value+"}");
  if (count == 0) return Double.NaN;
  return Value / count;
 }

 @Override
 public void noteValue(double value)
 {
  super.noteValue(value);
  N ++;
  if (Debug)System.err.println("Mean.noteValue{"
                              +"N="      +N                          +"\t"
                              +"Value="  +Value                      +"\t"
                              +"value="  +value                      +"}");
 }

 @Override
 public void reset()
 {
  super.reset();
  N = 0;
 }
}


