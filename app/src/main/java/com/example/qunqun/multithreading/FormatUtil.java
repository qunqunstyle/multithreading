package com.example.qunqun.multithreading;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * welcome layout Activity
 * Created by VideoMedicine Group on 2017/9/3.
 * 设置APP video存储
 * @author GqGAO
 */

public class FormatUtil {

    public static final int MAX_LENGTH = 400;
    public static String videoPath;
    private static String TAG_FORMATUTIL = "FormatUtil";
    public static String videoName;
    private static String dataPath;
    private static String dataName;

    private static final int MSG_PROGRESS_UPDATE = 0x110;
    // 文件存储
    public static  void videoRename(File recAudioFile) {
        // 获取存储路径
        videoPath = Environment.getExternalStorageDirectory()
                .getAbsolutePath()+  "/CameraDemo/video/"+ "SucessVideo" + "/";
        // 以日期+时间作为文件名
        videoName = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date()) + ".avi";
        File out = new File(videoPath);
        if (!out.exists()){
            out.mkdirs();
        }
        out = new File(videoPath, videoName);
        if(recAudioFile.exists())
            recAudioFile.renameTo(out);
    }

    /**
     * @brief this method used to calculate the heart rate value.
     * @return return the value of heart rate of the input video.
     */
    public static float[] getHeartRate(){
        float[] data = new float[MAX_LENGTH];
        String content;
        videoPath = videoPath + videoName;
        Log.d("TRANSCODEC","videoPath"+videoPath);
        content =Float.toString(data[0])+",";
        for(int i=1;i<data.length;i++){
            content += Float.toString(data[i])+",";
        }
        Log.i(TAG_FORMATUTIL, "content:  " +content);
        writeTxtToFile(content);
        //delete file
        deleteFile(videoPath);
        Log.i(TAG_FORMATUTIL, "data: "+ data[0]);
        return data;
    }

    public static void writeTxtToFile(String content ){

        dataPath= Environment.getExternalStorageDirectory()
                .getAbsolutePath()+  "/CameraDemo/"+"data"+"/" ;
        dataName = "data.txt";
        content = content +"              "+new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss").format(new Date())+ "\r\n";
        try{
            File file = new File(dataPath,dataName);
            if (!file.exists()) {

                Log.d("TestFile", "Create the file:" + dataPath);
                file.getParentFile().mkdir();
                file.createNewFile();
                System.out.println(file.createNewFile());
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(content.getBytes());
            raf.close();
        }catch (Exception e){
            Log.e("File","Error on write File:"+ e);
        }
    }
    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            Log.i(TAG_FORMATUTIL,"delete sucess..");
            file.delete();
            return true;
        }
        return false;
    }
}
