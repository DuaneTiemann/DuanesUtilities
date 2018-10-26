package com.duane.utilities;

public class Statistic
{
 protected boolean Debug = false;
 protected double  Value = Double.NaN;

 public double getResult(){return Value;}

 public void reset(){Value = Double.NaN;}

 public Statistic(){}

 public String toString()
 {
  return "{Statistic:"
         +super.toString()+"\t"
         +"Value="+Value  +"}";
 }
}


