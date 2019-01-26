package com.example.qunqun.multithreading;

import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * welcome layout Activity
 * Created by VideoMedicine Group on 2018/11/12
 * 设置APP 多线程，该模块未开发更新
 * @author GqGAO
 */

public class ProcessVideoTask extends Thread {
    private BlockingDeque<byte []>blockingDeque = new LinkedBlockingDeque<>();
    private  boolean continueRun = true;
    public void put(byte[] bytes){
        try{
            blockingDeque.put(bytes);
        }catch (InterruptedException e){
        }
    }

    @Override
    public void run() {
        super.run();
        while (continueRun){
            try{
                byte [] bytes = blockingDeque.take();

                //处理数据
                String in =bytes.length+"";
                Log.d("LongOfBytes[]",in);
                //System.out.println("444444444444444444");


            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

}
