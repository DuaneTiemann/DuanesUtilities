package com.duane.utilities;

import java.lang.Math;
import java.util.Date;

public class CollectionInterval 
{
 private Date Start = null;
 private Date Stop  = null;

 public Date getStart(){return Start;}
 public Date getStop (){return Stop ;}

 public CollectionInterval(long intervalMs)
 {
  this(intervalMs,false);
 }

 // initial determines whether or not to use the current time as interval start time.
 // That would be good for file names, but not for the first entry in a log file.

 public CollectionInterval(long intervalMs, boolean initial) 
 {
  Date now = new Date();
  Start    = new Date(initial?now.getTime():Utilities.floor(now.getTime()           ,intervalMs));
  Stop     = new Date(                      Utilities.floor(now.getTime()+intervalMs,intervalMs));
 }
}
