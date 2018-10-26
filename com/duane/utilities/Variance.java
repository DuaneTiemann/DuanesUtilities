package com.duane.utilities;

public class Variance
{
 @ParmParserParm
 protected boolean Debug      = false;
 private   double  FirstValue = Double.NaN;   // for proper scaling for rounding error.
 private   long    N          = 0;
 private   double  SumX       = 0;
 private   double  SumX2      = 0;

 protected double getMean(){return getMean(N);}
 protected double getMean(long count)
 {
  if (count == 0)return Double.NaN;
  return FirstValue+SumX/count;
 }

 protected long getN(){return N;}

 public double getResult(){return getResult_(N);}

 public double getResult(long count){return getResult_(count);}

 private double getResult_(long count)  // in order to avoid calling children and recursing to death.
 {
  double eX     = Double.NaN;
  double eX2    = Double.NaN;
  double result = Double.NaN;
  if (count != 0)
     {
      eX     = SumX /count;
      eX2    = SumX2/count;
      result = eX2-eX*eX;
     }
  if (Debug)System.err.println("Variance.getResult{"
                              +"count="     +count          +"\t"
                              +"eX="        +eX             +"\t"
                              +"eX2="       +eX2            +"\t"
                              +"hashCode="  +hashCode()     +"\t"
                              +"N="         +N              +"\t"
                              +"result="    +result         +"\t"
                              +"SumX="      +SumX           +"\t"
                              +"SumX2="     +SumX2          +"}");
  return result;
 }

 public void noteValue(double value)
 {
  if (Debug)System.err.println("Variance.noteValue{"
                              +"value="+Utilities.toString(value)+"}");
  
  if (!Utilities.isNaN(value))
     {
      N++;
      if (Utilities.isNaN(FirstValue))FirstValue=value;
      value -= FirstValue;  // center around 1st value to avoid catastrophic cancellation
      SumX  += value;
      SumX2 += value*value;
     }
  if (Debug)System.err.println("Variance.noteFields note value{"
                              +"FirstValue="+FirstValue+"\t"
                              +"hashCode="  +hashCode()+"\t"
                              +"N="         +N         +"\t"
                              +"SumX="      +SumX      +"\t"
                              +"SumX2="     +SumX2     +"\t"
                              +"value="     +value     +"}");
 }

 public void reset()
 {
  FirstValue = Double.NaN;
  SumX2      = 0;
  SumX       = 0;
  N          = 0;
 }

 public Variance(){}
}


