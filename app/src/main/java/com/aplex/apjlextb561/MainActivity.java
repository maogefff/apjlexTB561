package com.aplex.apjlextb561;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aplex.apjlextb561.serial.SerialPort;
import com.aplex.apjlextb561.serial.SerialPortFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";

    Spinner serialPort;
    Spinner baudRate;
    Button openSerial;

    TextView currentOn;
    TextView currentOff;

    TextView inputVol;
    EditText maxVol;
    EditText minVol;
//    Spinner biosSelect;
    TextView biosSelect;
    Button volSet;

    Spinner DelayOn;
    Spinner DelayOff;
    Button delayOnSet;
    Button delayOffSet;

    private SerialPortFinder mSerialPortFinder;
    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;

    int delayOffTimeIndex;
    int delayOnTimeIndex;
    boolean serialConn = false;

    String baudrateVal;
    String serialportVal;

    String currentPowerOn;
    String currentPowerOff;
    String inputVoltage;
    String volMin;
    String volMax;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//        setContentView(R.layout.activity_evalute);
        setContentView(R.layout.activity_main);
        mSerialPortFinder = new SerialPortFinder();

        initView();
        initListener();
    }

    private void initView(){
        // Devices
        String[] entries = mSerialPortFinder.getAllDevices();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        for (String x : entries){
            Log.d(TAG, "entries="+x);
        }
        for (String x : entryValues){
            Log.d(TAG, "entryValues="+x);
        }

        serialPort = findViewById(R.id.serialPortID);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,android.R.id.text1,entries);
        serialPort.setAdapter(adapter);
        serialportVal = entryValues[0];

        baudRate = findViewById(R.id.baudRateID);
        baudRate.setSelection(2,true);
        String[] res = getResources().getStringArray(R.array.baudrates_value);
        baudrateVal = res[2];

        Log.d(TAG, "端口号："+serialportVal+"; 波特率："+baudrateVal);
        openSerial = findViewById(R.id.openSerialID);

        currentOn = findViewById(R.id.displayOnID);
        currentOff = findViewById(R.id.displayOffID);

        inputVol = findViewById(R.id.inputVal_id);
        maxVol = findViewById(R.id.volMaxID);  //
        minVol = findViewById(R.id.volMinVal);

        volSet = findViewById(R.id.volSetID);
        biosSelect = findViewById(R.id.biosSelectID);

        DelayOn = findViewById(R.id.delayOnID);
        DelayOff = findViewById(R.id.delayOffID);
        delayOnSet = findViewById(R.id.delayOnSetID);
        delayOffSet = findViewById(R.id.delayOffSetID);
    }

    private void initListener(){

        //第一行
        serialPort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] res = mSerialPortFinder.getAllDevicesPath();
                serialportVal = res[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        baudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] res = getResources().getStringArray(R.array.baudrates_value);
                baudrateVal = res[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        openSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(serialConn==true){
                    Log.d(TAG, "关闭串口");
                    mReadThread.isInterrupted();
                    mReadThread = null;
                    if (mSerialPort != null) {
                        mSerialPort.close();
                        mSerialPort = null;
                    }
                    openSerial.setText("Open");
                    serialConn = false;
                }else {
                    Log.d(TAG, "打开串口");
                    try {
                        mSerialPort = getSerialPort();
                        mOutputStream = mSerialPort.getOutputStream();
                        mInputStream = mSerialPort.getInputStream();

			            /* Create a receiving thread */
                        mReadThread = new ReadThread();
                        mReadThread.start();
                    } catch (SecurityException e) {
                        DisplayError(R.string.error_security);
                    } catch (IOException e) {
                        DisplayError(R.string.error_unknown);
                    } catch (InvalidParameterException e) {
                        DisplayError(R.string.error_configuration);
                    }
                    serialConn = true;
                    openSerial.setText("Close");
                }
            }
        });

        //第二行
        currentOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        currentOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        //第三行
        maxVol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        volSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] data = new byte[5];
                data[0]=0x02;

                volMax = maxVol.getText().toString();
                volMin = minVol.getText().toString();
                String[] maxTmp = volMax.split("\\.");
                String[] minTmp = volMin.split("\\.");

                data[1] = Byte.parseByte(maxTmp[0]);
                if(data[1]>36){
                    Toast.makeText(MainActivity.this, "More than 36V", Toast.LENGTH_SHORT).show();
                    maxVol.setText("0");
                    return;
                }
                if(maxTmp.length==2){
                    data[2] = Byte.parseByte(maxTmp[1]);
                }else {
                    data[2] = 0;
                }

                data[3] = Byte.parseByte(minTmp[0]);
                if(data[3]>data[1]){
                    Toast.makeText(MainActivity.this, "Min greater than Max", Toast.LENGTH_SHORT).show();
                    minVol.setText("0");
                    return;
                }
                if(minTmp.length==2){
                    data[4] = Byte.parseByte(minTmp[1]);
                }else {
                    data[4] = 0;
                }
                onDataSend(data);
            }
        });

        //第四行
        DelayOn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String[] res = getResources().getStringArray(R.array.powerOn_value);
                delayOnTimeIndex = Integer.parseInt(res[i]);
                Log.d(TAG, "进入DelayOn Spinner, 索引为："+delayOnTimeIndex);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        DelayOff.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] res = getResources().getStringArray(R.array.powerOn_value);
                delayOffTimeIndex = Integer.parseInt(res[i]);
                Log.d(TAG, "进入DelayOff Spinner, 索引为："+delayOffTimeIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        delayOnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] data = new byte[5];
                data[0] = 0x03;
                data[1] = (byte) delayOnTimeIndex;
                data[2] = 0x00;
                data[3] = 0x00;
                data[4] = 0x00;
                onDataSend(data);
            }
        });
        delayOffSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] data = new byte[5];
                data[0] = 0x04;
                data[1] = (byte) delayOffTimeIndex;
                data[2] = 0x00;
                data[3] = 0x00;
                data[4] = 0x00;
                onDataSend(data);
            }
        });
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();

            while(!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[3];

                    if (mInputStream == null) return;
                    if (mInputStream.available() > 0) {
                        size = mInputStream.read(buffer);
                        if (size > 0) {
                            Log.d(TAG, "接收："+Arrays.toString(buffer));
                            praseReceive(buffer);
                        }
                    }
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            }
        }
    }

    public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
			/* Read serial port parameters */
            String path = serialportVal;
            int baudrate = Integer.decode(baudrateVal);

			/* Check parameters */
            if ( (path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }
            Log.d(TAG, "路径为："+path+";  波特率为"+baudrate);
			/* Open the serial port */
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            Log.d(TAG, "SerialPort之后");
        }
        return mSerialPort;
    }

    private void DisplayError(int resourceId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(resourceId);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        b.show();
    }

    protected void praseReceive(byte[] buffer){

        switch (buffer[0]){
            case 0x01:
                inputVoltage = String.valueOf(buffer[1])+"."+String.valueOf(buffer[1]);
                Log.d(TAG, "inputVoltage="+inputVoltage);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        inputVol.setText(inputVoltage);
                    }
                });
                break;
            case 0x06:
                String[] resOn = getResources().getStringArray(R.array.powerOn_display);
                currentPowerOn = resOn[buffer[1]-1];
                String[] resOff = getResources().getStringArray(R.array.powerOff_display);
                currentPowerOff = resOff[buffer[2]-1];
                Log.d(TAG, "currentPowerOn="+currentPowerOn);
                Log.d(TAG, "currentPowerOff="+currentPowerOff);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentOn.setText(currentPowerOn);
                        currentOff.setText(currentPowerOff);
                    }
                });
                break;

            case 0x07:   //当前模式位
                if(buffer[1]==0x01)//固定格式
                {
                    switch (buffer[2]){
                        case 0x00:   //bios
                            Log.d(TAG, "进入Bios");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "更新UI为Bios");
                                    delayOnSet.setTextColor(getResources().getColor(R.color.gainsboro));
                                    delayOffSet.setTextColor(getResources().getColor(R.color.gainsboro));
                                    delayOnSet.setClickable(false);
                                    delayOffSet.setClickable(false);
                                    biosSelect.setText("Bios");
                                }
                            });
                            break;
                        case 0x01:   //hardware
                            Log.d(TAG, "进入hardware");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "更新UI为hardware");
                                    delayOnSet.setTextColor(getResources().getColor(R.color.black));
                                    delayOffSet.setTextColor(getResources().getColor(R.color.black));
                                    delayOnSet.setClickable(true);
                                    delayOffSet.setClickable(true);
                                    biosSelect.setText("Hardware");
                                }
                            });
                            break;
                        case 0x02:   //app
                            Log.d(TAG, "进入app");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "更新UI为APP");
                                    delayOnSet.setTextColor(getResources().getColor(R.color.black));
                                    delayOffSet.setTextColor(getResources().getColor(R.color.black));
                                    delayOnSet.setClickable(true);
                                    delayOffSet.setClickable(true);
                                    biosSelect.setText("APP");
                                }
                            });
                            break;
                        default:
                            break;
                    }
                }
                break;
            case 0x08:  //当前电压最大值
                volMax = String.valueOf(buffer[1])+"."+String.valueOf(buffer[2]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        maxVol.setText(volMax);
                    }
                });
                break;
            case 0x09:  //当前电压最小值
                volMin = String.valueOf(buffer[1])+"."+String.valueOf(buffer[2]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        minVol.setText(volMin);
                    }
                });
                break;
            default:break;
        }
    }


    protected void onDataSend(byte[] data){
        for (Byte x: data){
            Log.d(TAG, "发送数据为："+x);
        }
        if(serialConn==false){
            Toast.makeText(MainActivity.this, "please open serial", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
