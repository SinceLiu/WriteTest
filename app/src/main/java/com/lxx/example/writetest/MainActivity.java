package com.lxx.example.writetest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private Button fill, stop, clear, check;
    private TextView totalSizeTv, freeSizeTv, filledSizeTv, inputSizeTv, fillCountTv;
    private TextView filledCountTv, fillTimeTv, startTimeTv, endTimeTv, fillSpeedTv, exceptionCountTv;
    private double nTotalBlocks, nAvailaBlock, nBlocSize;
    private int nSDTotalSize, nSDFreeSize;
    private File file;
    private String path;
    private String startTimeStr, endTimeStr, averageTimeStr;
    private long startTime, endTime, averageTime;
    private double startSize, endSize, fillSpeed;
    private final String fileName = "writeTest.txt";
    private MyThread myThread;
    private MyHandler myHandler;
    private int filledSize, inputSize, fillCount;
    private long filledCount;
    private boolean exit;
    private int exceptionCount;
    private List<ExceptionBean> exceptionList;

    public final static String TAG = "WriteTest_Log";
    public final static int SET_SIZE = 0;
    public final static int SET_RESULT = 1;
    public final static int SET_BUTTON_FILL_ENABLE = 2;
    public final static int SET_EXCEPTION = 3;


    //读写权限 android 6.0后需要动态申请
    private final static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final static int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //常亮
        totalSizeTv = findViewById(R.id.size_total);
        freeSizeTv = findViewById(R.id.size_free);
        filledSizeTv = findViewById(R.id.size_fill);
        inputSizeTv = findViewById(R.id.size_input);
        fillCountTv = findViewById(R.id.fill_count);
        filledCountTv = findViewById(R.id.filled_count);
        fillTimeTv = findViewById(R.id.fill_time);
        startTimeTv = findViewById(R.id.start_time);
        endTimeTv = findViewById(R.id.end_time);
        fillSpeedTv = findViewById(R.id.fill_speed);
        exceptionCountTv = findViewById(R.id.exception_count);
        fill = findViewById(R.id.btn_fill);
        stop = findViewById(R.id.btn_stop);
        clear = findViewById(R.id.btn_clear);
        check = findViewById(R.id.btn_exception);
        myHandler = new MyHandler(this);
        filledCount = 0;
        exceptionCount = 0;
        exceptionList = new ArrayList<ExceptionBean>();
        check.setEnabled(false);
        exceptionCountTv.setTextColor(getResources().getColor(R.color.black));

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {  //Android 6.0后要动态申请权限
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

        path = Environment.getExternalStorageDirectory().getPath();
        file = new File(path + "/" + fileName);
        Log.e(TAG, "address：" + file.toString());
        getSize();
        if (nSDFreeSize > 100) {
            inputSizeTv.setText(String.valueOf(nSDFreeSize - 100));
        } else {
            inputSizeTv.setText(String.valueOf(0));
        }
        if (!file.exists()) {
            try {
                Log.e(TAG, "创建文件：" + file.createNewFile());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "创建文件异常");
            }
        } else {
            Log.e(TAG, "文件已经存在!");
        }

        fill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String size = inputSizeTv.getText().toString();
                String count = fillCountTv.getText().toString();
                if (size.equals("")) {
                    Toast.makeText(MainActivity.this, "请输入填充大小", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    inputSize = Integer.valueOf(size);
                    if(inputSize>=nSDFreeSize){
                        Toast.makeText(MainActivity.this,"请输入小于剩余容量的值",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                initResult();
                myThread = new MyThread();
                if (!count.equals("")) {
                    fillCount = Integer.valueOf(count);
                    if (fillCount == 0) {
                        fillCount = 1;
                    }
                } else {
                    fillCount = Integer.MAX_VALUE;
                }
                Log.e(TAG, "填充大小：" + inputSize + "   填充次数：" + fillCount);
                exit = false;
                fill.setEnabled(false);
                Log.e(TAG, "开始填充");
                myThread.start();
                startTime = System.currentTimeMillis();
                startTimeStr = new Date().toLocaleString();
                startTimeTv.setText(startTimeStr);
                startSize = filledSize;
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "点击停止填充");
                exit = true;
                fill.setEnabled(true);
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit = true;
                clear();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (nSDFreeSize > 100) {
                            inputSizeTv.setText(String.valueOf(nSDFreeSize - 100));
                        } else {
                            inputSizeTv.setText(String.valueOf(0));
                        }
                        inputSize = Integer.valueOf(inputSizeTv.getText().toString());
                    }
                }, 100);   //设置延迟，确保先getSize()
                fill.setEnabled(true);
            }
        });

        check.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                ExceptionBean.setExceptionList(exceptionList);
                Intent intent = new Intent(MainActivity.this, ExceptionListActivity.class);
                startActivity(intent);
            }
        });
    }

    class MyThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < fillCount; i++) {
                Log.e(TAG, "第" + (i + 1) + "次填充开始     filledCount : "+filledCount);
                if (i > 0) {
                    clear();
                    if (exit) {
                        Log.e(TAG, "填充结束");
                        return;
                    }
                }
                if (!exit) {
                    for (int j = 0; j < inputSize; j++) {
                        if (exit) {
                            setResult();
                            Log.e(TAG, "填充结束");
                            return;
                        }
                        add1MB();
                    }
                }
                setResult();
            }
            myHandler.sendMessage(myHandler.obtainMessage(SET_BUTTON_FILL_ENABLE));
            Log.e(TAG, "填充结束");
        }
    }

    public void add1MB() {
        try {
            byte[] bytes = new byte[1024 * 1024];  //1MB
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = '1';
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            exceptionCount++;
            Log.e(TAG, "捕获异常次数：" + exceptionCount);
            exceptionList.add(new ExceptionBean(exceptionCount, new Date().toLocaleString(), "填充数据异常：" + e.toString()));
            myHandler.sendMessage(myHandler.obtainMessage(SET_EXCEPTION));
            return;
        }
        myHandler.sendMessage(myHandler.obtainMessage(SET_SIZE));
    }

    public void clear() {
        Log.e(TAG, "清除填充");
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("");
            fileWriter.close();
        } catch (Exception e) {
            exceptionCount++;
            exceptionList.add(new ExceptionBean(exceptionCount, new Date().toLocaleString(), "清除填充异常：" + e.toString()));
            e.printStackTrace();
            Log.e(TAG, "捕获异常次数：" + exceptionCount);
            myHandler.sendMessage(myHandler.obtainMessage(SET_EXCEPTION));
        }
        myHandler.sendMessage(myHandler.obtainMessage(SET_SIZE));
    }

    public void initResult() {
        filledCount = 0;
        exceptionCount = 0;
        startSize = filledSize;
        endSize = 0;
        endTimeTv.setText("");
        filledCountTv.setText(String.valueOf(filledCount));
        exceptionCountTv.setText(String.valueOf(exceptionCount));
        exceptionCountTv.setTextColor(getResources().getColor(R.color.black));
        fillTimeTv.setText("");
        fillSpeedTv.setText("");
        check.setEnabled(false);
        if (ExceptionBean.getExceptionList() != null) {
            ExceptionBean.clearExceptionList();
        }
    }

    public void getSize() {
        File root = Environment.getDataDirectory();
        StatFs statfs = new StatFs(root.getPath());
        nTotalBlocks = statfs.getBlockCount();
        nBlocSize = statfs.getBlockSize();
        nAvailaBlock = statfs.getAvailableBlocks();
        nSDTotalSize = (int) Math.round(nTotalBlocks * nBlocSize / 1024 / 1024);
        nSDFreeSize = (int) Math.round(nAvailaBlock * nBlocSize / 1024 / 1024);
        totalSizeTv.setText(nSDTotalSize + " MB");
        freeSizeTv.setText(nSDFreeSize + " MB");
        filledSize = Math.round(file.length() / 1024 / 1024);
        double percent = (double) Math.round(filledSize * 100 * 10 / (nSDFreeSize + filledSize)) / 10;
        filledSizeTv.setText(filledSize + " MB" + "(" + percent + "%)");
    }

    public void setResult() {
        endTime = System.currentTimeMillis();
        endTimeStr = new Date().toLocaleString();
        endSize = endSize + filledSize;
        filledCount++;
        averageTime = Math.round((endTime - startTime) / filledCount);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        averageTimeStr = formatter.format(averageTime);
        fillSpeed = Math.round(10 * 1000 * (endSize - startSize) / (endTime - startTime)) / 10.0;
        myHandler.sendMessage(myHandler.obtainMessage(SET_RESULT));
    }

    static class MyHandler extends Handler {
        private WeakReference<MainActivity> reference;

        private MyHandler(MainActivity activity) {
            reference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = reference.get();
            switch (msg.what) {
                case SET_SIZE:
                    activity.getSize();
                    break;

                case SET_RESULT:
                    activity.endTimeTv.setText(activity.endTimeStr);
                    activity.filledCountTv.setText(String.valueOf(activity.filledCount));
                    activity.fillTimeTv.setText(activity.averageTimeStr);
                    activity.fillSpeedTv.setText(String.valueOf(activity.fillSpeed) + "MB/s");
                    activity.inputSizeTv.setText(String.valueOf(activity.nSDFreeSize - 100));
                    activity.inputSize = Integer.valueOf(activity.inputSizeTv.getText().toString());
                    break;

                case SET_BUTTON_FILL_ENABLE:
                    activity.fill.setEnabled(true);
                    break;

                case SET_EXCEPTION:
                    activity.exceptionCountTv.setText(String.valueOf(activity.exceptionCount));
                    activity.exceptionCountTv.setTextColor(activity.getResources().getColor(R.color.red));
                    activity.check.setEnabled(true);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.e(TAG, "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
                if (grantResults[i] == -1) {   //权限选否直接退出
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
        if (isFinishing()) {
            exit = true;
            myHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        exit = true;
        myHandler.removeCallbacksAndMessages(null);
    }
}

