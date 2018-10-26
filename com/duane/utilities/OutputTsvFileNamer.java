package com.duane.utilities;

import com.duane.utilities.OutputTsvFile;
import java.util.Date;

public class OutputTsvFileNamer
{
 private boolean Debug     = false;
 private String  LagSuffix = "";

 public void nameOutputTsvFiles(OutputTsvFile[] outputFiles, CollectionInterval collectionInterval)
 { // add FileIntervalStart to name

  Date   start  = collectionInterval.getStart();
  Date   stop   = collectionInterval.getStop ();
  String suffix = "_"+Utilities.toString(collectionInterval.getStart()).replace('/','_').replace(' ','_').replace(':','_');
  if (start.compareTo(stop) != 0)
        suffix += "_"+Utilities.toString(collectionInterval.getStop ()).replace('/','_').replace(' ','_').replace(':','_');
 
  for (OutputTsvFile outputFile : outputFiles)
      {
       String currentFileName       = outputFile.getFileName();
       int    pos                   = currentFileName.lastIndexOf('.');
       if (pos == -1)pos = currentFileName.length();
       String baseFileName          = currentFileName.substring(0, pos);
       String currentFileNameSuffix = currentFileName.substring(pos   );
                                                                        // Note LagSuffix starts out as "" so this will work unless new files are introduced.
       if (!baseFileName.endsWith(LagSuffix))Utilities.error("OutputTsvFileNamer.nameOutputFile current doesn't end with prior suffix{"
                                                            +"currentFileName="+currentFileName+"\t"
                                                            +"LagSuffix="      +LagSuffix      +"}");
       else                                  baseFileName = baseFileName.substring(0,baseFileName.length()-LagSuffix.length());
       String newFileName = baseFileName+suffix+currentFileNameSuffix;
       if (Debug)Utilities.debug("OutputTsvFileNamer.nameOutputFiles{"
                                +"currentFileName="+currentFileName+"\t"
                                +"LagSuffix="      +LagSuffix      +"\t"
                                +"newFileName="    +newFileName    +"\t"
                                +"suffix="         +suffix         +"}");
       outputFile.setFileName(newFileName);
      }
  LagSuffix = suffix;
 }
}

