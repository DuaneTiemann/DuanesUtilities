package com.duane.utilities;

public class StdDev extends Variance
{
 @Override
 public double getResult()
 {
  return getResult_(getN());
 }

 @Override
 public double getResult(long count){return getResult_(count);}

 private double getResult_(long count)
 {
  return Math.sqrt(super.getResult(count));
 }

 public StdDev(){super();}
}

