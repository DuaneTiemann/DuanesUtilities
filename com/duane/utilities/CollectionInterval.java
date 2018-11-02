package com.duane.utilities;

import java.lang.Math;
import java.util.Date;
import java.util.TimeZone;

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
  this(new Date(),intervalMs,initial);
 }

 public CollectionInterval(Date date, long intervalMs, boolean initial) 
 {
  TimeZone timeZone    = TimeZone.getDefault();
  long     dateOffset  = timeZone.getOffset(date.getTime());

//     If DST is around, find out when it changes. 
//     Find candidate start time by converting to GMT, zapping, and back. 
//     If in different timezone, then adjust
//     Add interval to result. If in different timezone then adjust

//  Date timeZoneChange = findTimeZoneChange(date);
//  if (timeZoneChange == null)
//     {
//
//     }
      
  Date     gmtDate     = new Date(date.getTime()+dateOffset);
  Date     gmtStart    = new Date(Utilities.floor(gmtDate.getTime(),intervalMs));  // Zap back to interval in gmt space
  Date     start1      = new Date(gmtStart.getTime()-dateOffset);                  // Convert zapped back gmt time to host timezone
  long     startOffset = timeZone.getOffset(start1.getTime());                 

  Date     start2      = new Date(gmtStart.getTime()-startOffset);                   // Start may be in different time zone so apply earlier one.   ????
  Date     savestart2  = start2;
  if (start2.compareTo(date)>0)start2 = new Date(start1.getTime()-intervalMs);

  Date     gmtStop     = new Date(gmtStart.getTime() + intervalMs);
  Date     stop1       = new Date(gmtStop.getTime()-dateOffset);                   // Convert gmt time to host time zone
  long     stopOffset  = timeZone.getOffset(stop1.getTime());
  Date     stop2       = new Date(gmtStop.getTime()-stopOffset);                   // Convert gmt time to host time zone
  Date     savestop2   = stop2;
  if (stop2.compareTo(date)<0)stop2 = new Date(stop2.getTime()+intervalMs);

  Start = start2;
  Stop  = stop2;

//  Utilities.debug("CollectionInterval"                                                  +"\n"
//                 +"{"                                                                   +"\n"
//                 +" date="              +"\t"+Utilities.toString(date                  )+"\n"
//                 +" date.getTime="      +"\t"+Utilities.toString(getTime(date         ))+"\n"
//                 +" gmtDate="           +"\t"+Utilities.toString(gmtDate               )+"\n"
//                 +" gmtDate.getTime="   +"\t"+Utilities.toString(getTime(gmtDate      ))+"\n"
//                 +" gmtStart="          +"\t"+Utilities.toString(gmtStart              )+"\n"
//                 +" gmtStart.getTime="  +"\t"+Utilities.toString(getTime(gmtStart     ))+"\n"
//                 +" start1="            +"\t"+Utilities.toString(start1                )+"\n"
//                 +" start1.getTime="    +"\t"+Utilities.toString(getTime(start1       ))+"\n"
//                 +" startOffset="       +"\t"+Utilities.toString(getOffset(startOffset))+"\n"
//                 +" savestart2="        +"\t"+Utilities.toString(savestart2            )+"\n"
//                 +" savestart2.getTime="+"\t"+Utilities.toString(getTime(savestart2   ))+"\n"
//                 +" start2="            +"\t"+Utilities.toString(start2                )+"\n"
//                 +" start2.getTime="    +"\t"+Utilities.toString(getTime(start2       ))+"\n"
//                 +" gmtStop="           +"\t"+Utilities.toString(gmtStop               )+"\n"
//                 +" gmtStop.getTime="   +"\t"+Utilities.toString(getTime(gmtStop      ))+"\n"
//                 +" stop1="             +"\t"+Utilities.toString(stop1                 )+"\n"
//                 +" stop1.getTime="     +"\t"+Utilities.toString(getTime(stop1        ))+"\n"
//                 +" stopOffset="        +"\t"+Utilities.toString(getOffset(stopOffset ))+"\n"
//                 +" savestop2="         +"\t"+Utilities.toString(savestop2             )+"\n"
//                 +" savestop2.getTime=" +"\t"+Utilities.toString(getTime(savestop2    ))+"\n"
//                 +" stop2="             +"\t"+Utilities.toString(stop2                 )+"\n"
//                 +" stop2.getTime="     +"\t"+Utilities.toString(getTime(stop2        ))+"\n"
//                 +" Start="             +"\t"+Utilities.toString(Start                 )+"\n"
//                 +" Stop="              +"\t"+Utilities.toString(Stop                  )+"\n"
//                 +")"                                                                   +"\n");

  if (initial)Start = date;
 }

 private Date findTimeZoneChange(Date date,long intervalMs)
 {
  TimeZone timeZone    = TimeZone.getDefault();
  long     dateOffset  = timeZone.getOffset(date.getTime());
  long     earlyOffset = timeZone.getOffset(date.getTime()-intervalMs);
  long     lateOffset  = timeZone.getOffset(date.getTime()+intervalMs);
  if (earlyOffset == lateOffset)return null;

  long earlyStart = Utilities.floor(date.getTime()        ,3600000);
  long lateStop   = Utilities.floor(date.getTime()+3600000,3600000);

  for (long time = earlyStart;time <= lateStop;time+=3600000)
      {
       long offset = timeZone.getOffset(time);
       if (offset != dateOffset)return new Date(time);
      }

  Utilities.fatalError("CollectionInterval.findTimeZoneChange no time zone change found{"
                      +"date="      +Utilities.toString(date)+"\t"
                      +"intervalMs="+intervalMs              +"}");
  return null;
 }

 public long getTime(Date date)
 {
  return date.getTime()/3600000 - 428112;
 }

 public long getOffset(long offset)
 {
  return offset/3600000;
 }

 public String toString()
 {
  return "{CollectionInterval:"
        +"Start="+Utilities.toString(Start)+"\t"
        +"Stop=" +Utilities.toString(Stop )+"}";
 }
}
