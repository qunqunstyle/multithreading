package com.example.qunqun.multithreading;

import android.Manifest;
import android.app.Activity;


import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
/**
 * welcome MainActivity
 * Created by VideoMedicine Group on 2017/9/3.
 * 设置APP 主界面dd
 * @author GqGAO
 */

public class MainActivity extends Activity implements Camera.PreviewCallback {

    private final static String TAG_CAMERA_ACTIVITY = "MainActivity";
    private Camera camera;
    private MySurfaceView surfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private Button button;
    private CameraProgressBar mProgressbar;
    private Camera.Parameters mParams;
    private MediaRecorder mediaRecorder;     //录制视频类
    protected boolean isPreview = false;             //摄像区域是否准备良好  
    private boolean isRecording = true;           // true表示没有录像，点击开始；false表示正在录像，点击暂停  
    private File mRecVideoPath;
    private File mRecAudioFile;
    private static TextView Prompt;
    private static TextView remark;
    private static final int MAX_RECORD_TIME = 15 * 1000;
    private static final int PLUSH_PROGRESS = 100;
    private final int max = MAX_RECORD_TIME / PLUSH_PROGRESS;
    private RadioGroup radioGroup;
    private RadioButton face;
    private RadioButton finger;
    int  cameraType =1;
    private Chronometer ch ;
    private int cnt = 0;
    private float[] pulse_raw=new float[400];
    boolean saved=false; //标记是否已保存数据
    boolean flag=true; //标记是否点击了确认
    int fs;
    float fr;
    int duration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecVideoPath = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/CameraDemo/video/ErrorVideo/");
        Log.i(TAG_CAMERA_ACTIVITY,"..."+mRecVideoPath.getAbsolutePath());
        if (!mRecVideoPath.exists()) {
            mRecVideoPath.mkdirs();
            Log.i(TAG_CAMERA_ACTIVITY,"创建文件");
        }
        face =(RadioButton)findViewById(R.id.top_rg_a);
        ch = (Chronometer)findViewById(R.id.chronometer);
        finger=(RadioButton)findViewById(R.id.top_rg_b);
        mProgressbar = (CameraProgressBar)findViewById(R.id.camera_ProgressBar);
        Prompt = (TextView)findViewById(R.id.Prompt);
        Prompt.setText("请点击圆框录制视频");
        remark = (TextView)findViewById(R.id.mark);
        remark.setVisibility(View.VISIBLE);
        mProgressbar.setMaxProgress(max);
        button = (Button) findViewById(R.id.buttonPanel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Prompt.setText("开始采集视频帧");
                ch.setBase(SystemClock.elapsedRealtime());//计时器打开
                ch.start();
                saved = false;
                flag = false;
                cnt = 0;
                camera.setPreviewCallback(MainActivity.this);
                //camera.setPreviewCallbackWithBuffer(MainActivity.this);
                //camera.setOneShotPreviewCallback(MainActivity.this);
                surfaceView.setClickable(false);
                radioGroup.setClickable(false);
            }
        });
        radioGroup =(RadioGroup)findViewById(R.id.main_top_rg);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if(checkedId==face.getId()){
                    cameraType=1;
                    remark.setText("请将脸部放置在圆形区域中，并尽量保持不动");
                    Prompt.setText("请点击圆框开始");
                    remark.setVisibility(View.VISIBLE);
                    if (camera != null) {
                        //如果已经初始化过，就先释放
                        releaseCamera();
                    }
                    initView();
                    isPreview = true;
                }

                else if(checkedId==finger.getId()){
                    cameraType=0;
                    remark.setText("请将手指覆盖住摄像头，并尽量保持不动");
                    Prompt.setText("请点击圆框开始");
                    remark.setVisibility(View.VISIBLE);
                    if (camera != null) {
                        //如果已经初始化过，就先释放
                        releaseCamera();
                    }
                    initView();
                    isPreview = true;
                }

            }
        });
        surfaceView = (MySurfaceView) findViewById(R.id.camera_mysurfaceview);
        cameraSurfaceHolder = surfaceView.getHolder();
        cameraSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                cameraSurfaceHolder = holder;
                    initView();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                cameraSurfaceHolder = holder;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressbar.setVisibility(View.VISIBLE);
                Prompt.setVisibility(View.VISIBLE);
                remark.setVisibility(View.VISIBLE);
                Prompt.setText("正在录制...");
                surfaceView.setClickable(false);
                radioGroup.setClickable(false);

                 /*
                 *此处摄像头的准备工作为mediaRecorder的前置操作，开启录像
                 */
                if (isRecording) {
                    if (isPreview) {
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                    }
                    if (null == mediaRecorder) {
                        mediaRecorder = new MediaRecorder();
                    } else {
                        mediaRecorder.reset();
                    }

                    //判断类型开启前后置相机
                    if(cameraType==1){
                        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    }
                    else {
                        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    }
                    camera.lock();
                    Camera.Parameters parameters = camera.getParameters();
                    if(cameraType==0)
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setDisplayOrientation(90);
                    camera.enableShutterSound(false);
                    parameters.setPreviewFrameRate(25);
                    camera.setParameters(parameters);
                    camera.unlock();
                    mediaRecorder.setCamera(camera);
                    mediaRecorder.setOrientationHint(270);
                    mediaRecorder.setPreviewDisplay(cameraSurfaceHolder.getSurface());
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    mediaRecorder.setVideoSize(640, 480);
                    mediaRecorder.setVideoEncodingBitRate(8*1024*1024);

                    Log.i(TAG_CAMERA_ACTIVITY, "mediaRecorder set sucess");

                    try {
                        mRecAudioFile = File.createTempFile("Vedio", ".avi", mRecVideoPath);
                        Log.i(TAG_CAMERA_ACTIVITY,"11111111");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG_CAMERA_ACTIVITY, "..." + mRecAudioFile.getAbsolutePath());
                    mediaRecorder.setOutputFile(mRecAudioFile.getAbsolutePath());
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isRecording = !isRecording;
                    Log.i(TAG_CAMERA_ACTIVITY, "=====开始录制视频=====");
                }

                //在app build 中compile一下，是一个第三方的库
                Observable.interval(100,
                        TimeUnit.MILLISECONDS,
                        AndroidSchedulers.mainThread()).take(max).subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                        Camera.Parameters parameters = camera.getParameters();
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        /*
                        * 录像的关闭和资源释放
                        */
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        FormatUtil.videoRename(mRecAudioFile);
                        Log.e(TAG_CAMERA_ACTIVITY, "=====录制完成，已保存=====");
                        Prompt.setText("录制完成");
                        isRecording = !isRecording;
                        mProgressbar.reset();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                    @Override
                    public void onNext(Long aLong) {
                        mProgressbar.setProgress(mProgressbar.getProgress() + 1);
                    }
                });
            }
        });

    }

    private void initView() {
        // 初始化摄像头  ，设置为前置相机
        try {
            if(cameraType==1){
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            else {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFrameRate(30);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(cameraSurfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 释放摄像头资源
     */
    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.lock();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    List<String> mPermissionList = new ArrayList<>();

    // private ImageView welcomeImg = null;
    private static final int PERMISSION_REQUEST = 1;
    // 检查权限

    private void checkPermission() {
        mPermissionList.clear();

        //判断哪些权限未授予
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        /**
         * 判断是否为空
         */
        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了

        } else {//请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
        }
    }

    /**
     * 响应授权
     * 这里不管用户是否拒绝，都进入首页，不再重复申请权限
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST:

                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    //下面的对视频帧的处理是我从项目中截出来的
    // 没啥具体用处，如果是需要保存图片的话可以直接在得到Bitmap后保存即可
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        System.out.println("调用帧。。。");

        //返回一帧图像的G通道信号
        float[] single_channel_data = getSingleChannelData(data, 2);
        //点击事件中将flag置为false，挡位true的时候什么都不做
        if (flag) {
            //DO NOTHING
            System.out.println("DO NOTHING。。。 " );
        } else if (cnt < 400) {
            //取400帧
            //System.out.println("取帧。。。 " );
            Camera.Parameters params = camera.getParameters();
            //获取帧率，算了400次，取的是最后一次的值
            fs = params.getPreviewFrameRate();
            //将一帧图像像素平均成一个值，形成一个长度为400的一维数组
            pulse_raw[cnt] = calculateAvg(single_channel_data);
            cnt++;
        } else if (saved == false) {
            //cnt 达到400后跳出上一个进入这里计算
            ch.stop();
            System.out.println("计算。。。 " );
            String content = ch.getText().toString();
            String[] split = content.split(":");
            duration = Integer.parseInt(split[1]);  //计时器获取秒数
            System.out.println("duration = "+duration );
            fr = (float)(400.0 / duration);
            System.out.println("fr = "+fr );
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            Prompt.setText("400帧数据采集完成");
            camera.setPreviewCallback(null);
            surfaceView.setClickable(true);
            radioGroup.setClickable(true);
        }



    }

    //转成Bitmap获取G通道数据
    float[] getSingleChannelData(byte[] data, int channel) {

        //转NV21格式的byte数组为Bitmap图
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, 960, 720, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, 960, 720), 80, baos);
        byte[] jdata = baos.toByteArray();
        Bitmap mBitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
        //获取长宽
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        //像素点个数，输出数组长度
        int[] pixels = new int[width * height];
        float[] output = new float[width * height];
        //获取像素值
        mBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            //与操作 取相应位
            int red = (clr & 0x00ff0000) >> 16;
            int green = (clr & 0x0000ff00) >> 8;
            int blue = clr & 0x000000ff;
            if (channel == 1) { //red
                output[i] = red; // 取高两位
            }
            if (channel == 2) { //green
                output[i] = green; // 取中两位
            }
            if (channel == 3) { //blue
                output[i] = blue; // 取低两位
            }
            if (channel == 4) {    //亮度Y
                output[i] =(float) (0.229 * red + 0.587 * green + 0.114 * blue);
            }
            if (channel == 5) {    //色度U
                output[i] =(float) (-0.169 * red - 0.331 * green + 0.5 * blue);
            }
            if (channel == 6) {    //饱和度V
                output[i] =(float) (0.5 * red - 0.419 * green - 0.081 * blue);
            }
        }
        return output;
    }

    float calculateAvg(float[] array) {
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        float avg = sum / array.length;
        return avg;
    }


}
