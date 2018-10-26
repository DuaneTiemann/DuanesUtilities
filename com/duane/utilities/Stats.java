package com.duane.utilities;
import com.duane.utilities.CV;
import com.duane.utilities.Max;
import com.duane.utilities.Mean;
import com.duane.utilities.Min;
import com.duane.utilities.N;
import com.duane.utilities.Utilities;
 
public class Stats
{
 private CV   CV   = new CV  ();
 private Max  Max  = new Max ();
 private Mean Mean = new Mean();
 private Min  Min  = new Min ();
 private N    N    = new N   ();

 public CV   getCV  (){return CV  ;}
 public Max  getMax (){return Max ;}
 public Mean getMean(){return Mean;}
 public Min  getMin (){return Min ;}
 public N    getN   (){return N   ;}

 public void noteValue(double value)
 {
  CV  .noteValue(value);
  Max .noteValue(value);
  Mean.noteValue(value);
  Min .noteValue(value);
  N   .noteValue(value);
 }

 public void reset()
 {
  CV  .reset();
  Max .reset();
  Mean.reset();
  Min .reset();
  N   .reset();
 }
}


