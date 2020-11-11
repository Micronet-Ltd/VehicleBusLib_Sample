package com.micronet.vehiclebussample;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.support.constraint.Constraints.TAG;

public class ReadWriteFile {
    private static File Dir;
    private static File appLogDir = Environment.getExternalStorageDirectory();
    private Boolean oldFile;

    private static BufferedWriter bufferedWriter = null;
    private static FileWriter fileWriter = null;

    //Logging the Service activity date
    public void LogCsvToFile(String frame, String timestamp){
        String fileName = "Frames.csv";
        String header = "Time Stamp, Id, DataLength, Data, FrameCount,";
        String columnSep = ",";
        //String timestamp=(Utils.formatDate(System.currentTimeMillis())); //Getting current time stamp

        File file = new File(appLogDir + "/" + fileName);
        if(!file.exists()) {
            oldFile = false;
            Log.d(TAG, "Frames.csv: File Doesn't exist");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            oldFile = true;
        }
        try {
            fileWriter = new FileWriter(file.getAbsoluteFile(), true);
            bufferedWriter = new BufferedWriter(fileWriter);
            if(!oldFile){
                bufferedWriter.write(header);
                bufferedWriter.newLine();
            }
            bufferedWriter.write(timestamp + columnSep);
            bufferedWriter.write(frame + columnSep);
            bufferedWriter.newLine();

        }

        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            e.printStackTrace();
        }

        finally {
            try {
                if (bufferedWriter!=null)
                    bufferedWriter.close();
                if (fileWriter!=null)
                    fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkLogFolder(){
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File Root = Environment.getExternalStorageDirectory();
            ReadWriteFile.Dir = new File(Root.getAbsolutePath() + "/MicronetLogs");
            ReadWriteFile.appLogDir = new File(ReadWriteFile.Dir + "/VehicleBusLibrary");
            if (!ReadWriteFile.Dir.exists()) {
                try {
                    ReadWriteFile.Dir.mkdir();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(!ReadWriteFile.appLogDir.exists()){
                try {
                    ReadWriteFile.appLogDir.mkdir();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
