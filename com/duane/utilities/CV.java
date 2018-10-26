package com.duane.utilities;

public class CV extends StdDev
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
  double mean = getMean(count);
  if (Utilities.isNaN(mean))return Double.NaN;
  if (mean == 0            )return Double.NaN;
  double stddev = super.getResult(count);
  double result = stddev/Math.abs(mean);
  if (Debug)System.err.println("CV.getResult note mean{"
                              +"mean="  +mean  +"\t"
                              +"result="+result+"\t"
                              +"stddev="+stddev+"}");
  return result;
 }

 public CV(){super();}
}


