package ru.egorch.ploblue;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ru.egorch.ploblue.wav.WavFileException;

public class MainActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_CODE_LOC = 1;

    private static final int REQ_ENABLE_BT  = 10;
    public static final int BT_BOUNDED      = 21;
    public static final int BT_SEARCH       = 22;
    private static final long DEVICE_DELAY = 10;

    private FrameLayout frameMessage;
    private LinearLayout frameControls;

    private RelativeLayout frameGraphControls;
    private Button btnDisconnect;
    private EditText etConsole;
    private ImageView indicator;

    private Switch switchEnableBt;
    private Button btnEnableSearch;
    private ProgressBar pbProgress;
    private ListView listBtDevices;

    private BluetoothAdapter bluetoothAdapter;
    private BtListAdapter listAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private ProgressDialog progressDialog;

    private GraphView gvGraph;
    private LineGraphSeries series;
    private final int maxDataPointsOnGraph = 50;

    private String lastSensorValues = "";

    private Handler handler;
    private Runnable timer;

    private float xLastValue = 0;

    private Button btnSerialOn;
    private Button btnSerialOff;


    //Запись образца/////
    //private FileWriter saverWave;
    private List<Double> wave;
    private RecordStatus recordStatus = RecordStatus.END;
    private double recordEndTime = 0.0;
    private double recordDelay = 0.0;
    private long startTimeDelay = 0;
    private long startTimeRecord = 0;
    ///////
    private EditText etRecordName;
    private EditText etRecordDelay;
    private EditText etRecordEndTime;
    private EditText etRecordCurrentTime;
    private Button recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameMessage     = findViewById(R.id.frame_message);
        frameControls    = findViewById(R.id.frame_control);

        switchEnableBt   = findViewById(R.id.switch_enable_bt);
        btnEnableSearch  = findViewById(R.id.btn_enable_search);
        pbProgress       = findViewById(R.id.pb_progress);
        listBtDevices    = findViewById(R.id.lv_bt_device);

        frameGraphControls = findViewById(R.id.frameMessage);
        btnDisconnect    = findViewById(R.id.btn_disconnect);
        etConsole        = findViewById(R.id.et_console);

        //ИНИЦИАЛИЗАЦИЯ ГРАФИКА
        gvGraph          = findViewById(R.id.gv_graph);
        series    =  new LineGraphSeries();

        //ПАРАМЕТРЫ ГРАФИКА
        gvGraph.addSeries(series);
        gvGraph.getViewport().setMinX(0.0);
        gvGraph.getViewport().setMaxX(maxDataPointsOnGraph);
        gvGraph.getViewport().setMinY(0.0);
        gvGraph.getViewport().setMaxY(100.0);
        gvGraph.getViewport().setXAxisBoundsManual(true);

        //ИНИЦИАЛИЗАЦИЯ ИНДИКАТОРА РАБОТЫ
        indicator = findViewById(R.id.indicator);

        //Инициализация кнопок упровлением записи
        btnSerialOn = findViewById(R.id.serial_on);
        btnSerialOff = findViewById(R.id.serial_off);
        btnSerialOn.setOnClickListener(this);
        btnSerialOff.setOnClickListener(this);

        switchEnableBt.setOnCheckedChangeListener(this);
        btnEnableSearch.setOnClickListener(this);
        listBtDevices.setOnItemClickListener(this);

        btnDisconnect.setOnClickListener(this);

        bluetoothDevices = new ArrayList<>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.connecting));
        progressDialog.setMessage(getString(R.string.please_wait));

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: " + getString(R.string.bluetooth_not_supported));
            finish();
        }

        if (bluetoothAdapter.isEnabled()) {
            showFrameControls();
            switchEnableBt.setChecked(true);
            setListAdapter(BT_BOUNDED);
        }

        //Инициализация массива содержащего волну
        wave = new ArrayList<>();
        etRecordName = findViewById(R.id.et_record_name);
        etRecordDelay = findViewById(R.id.et_delay_record);
        etRecordEndTime = findViewById(R.id.et_end_record);
        etRecordCurrentTime = findViewById(R.id.et_timer_record);
        recordButton = findViewById(R.id.record_btn);
        recordButton.setText("ON");
    }

    /**
     * Возобновить работу приложения
     */
    @Override
    protected void onPause() {
        super.onPause();

        cancelTimer();
    }

    /**
     * Поставить в режим ожидания
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (connectedThread != null) {
            startTimer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelTimer();

        unregisterReceiver(receiver);

        if (connectThread != null) {
            connectThread.cancel();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    /**
     * Метод для отслеживания нажатия кнопок
     * @param v
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        if (v.equals(btnEnableSearch)) {
            enableSearch();
        } else if (v.equals(btnDisconnect)) {
            cancelTimer();

            if (connectedThread != null) {
                connectedThread.cancel();
            }

            if (connectThread != null) {
                connectThread.cancel();
            }

            showFrameControls();
        } else if (v.equals(btnSerialOn)) {
            setSerialStatusOnDevice(true);
        } else if (v.equals(btnSerialOff)) {
            setSerialStatusOnDevice(false);
        } else if (v.equals(recordButton)) {
            Toast.makeText(MainActivity.this, "__PREPARE PHASE__", Toast.LENGTH_SHORT).show();
            prepareFromRecordingToWav();
        }
    }

    /**
     * Отслеживает нажатие на элемент списка устройств (отслежтвает выбор)
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(listBtDevices)) {
            BluetoothDevice device = bluetoothDevices.get(position);
            if (device != null) {
                connectThread = new ConnectThread(device);
                connectThread.start();

                startTimer();
            }
        }
    }

    /**
     * Методя для отслежтвания состояния переключателей
     * @param buttonView
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.equals(switchEnableBt)) {
            enableBt(isChecked);

            if (!isChecked) {
                showFrameMessage();
            }
        }
    }

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK && bluetoothAdapter.isEnabled()) {
                showFrameControls();
                setListAdapter(BT_BOUNDED);
            } else if (resultCode == RESULT_CANCELED) {
                enableBt(true);
            }
        }
    }

    /**
     *  Показать панель для сообщения от устройства
     */
    private void showFrameMessage() {
        frameMessage.setVisibility(View.VISIBLE);
        frameGraphControls.setVisibility(View.GONE);
        frameControls.setVisibility(View.GONE);
    }

    /**
     * Показать панель для отображения доступных bluetooth подключений
     */
    private void showFrameControls() {
        frameMessage.setVisibility(View.GONE);
        frameGraphControls.setVisibility(View.GONE);
        frameControls.setVisibility(View.VISIBLE);
    }

    /**
     *  Показать панель для отображения графика
     */
    private void showFrameGraphControls() {
        frameGraphControls.setVisibility(View.VISIBLE);
        frameMessage.setVisibility(View.GONE);
        frameControls.setVisibility(View.GONE);
    }

    /**
     *  Метод для включения и отключения
     */
    private void enableBt(boolean flag) {
        if (flag) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);
        } else {
            bluetoothAdapter.disable();
        }
    }

    /**
     * Метод для отображения статуса устройтв (для понимания доступны ли сейчас они)
     * @param type
     */
    private void setListAdapter(int type) {

        bluetoothDevices.clear();
        int iconType = R.drawable.ic_bluetooth_bounded_device;

        switch (type) {
            case BT_BOUNDED:
                bluetoothDevices = getBoundedBtDevices();
                iconType = R.drawable.ic_bluetooth_bounded_device;
                break;
            case BT_SEARCH:
                iconType = R.drawable.ic_bluetooth_search_device;
                break;
        }
        listAdapter = new BtListAdapter(this, bluetoothDevices, iconType);
        listBtDevices.setAdapter(listAdapter);
    }

    /**
     * Возвращает список доступных устройств
     * @return
     */
    private ArrayList<BluetoothDevice> getBoundedBtDevices() {
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> tmpArrayList = new ArrayList<>();
        if (deviceSet.size() > 0) {
            for (BluetoothDevice device: deviceSet) {
                tmpArrayList.add(device);
            }
        }

        return tmpArrayList;
    }


    /**
     * Активировать поиск устройств или отключить
     */
    private void enableSearch() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        } else {
            accessLocationPermission();
            bluetoothAdapter.startDiscovery();
        }
    }

    /**
     * Явная реализация итерфейса для отслеживания статуса подключения
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    btnEnableSearch.setText(R.string.stop_search);
                    pbProgress.setVisibility(View.VISIBLE);
                    setListAdapter(BT_SEARCH);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    btnEnableSearch.setText(R.string.start_search);
                    pbProgress.setVisibility(View.GONE);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        bluetoothDevices.add(device);
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    /**
     * Запрос на разрешение данных о местоположении (для Marshmallow 6.0)
     */
    private void accessLocationPermission() {
        int accessCoarseLocation = this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int accessFineLocation   = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> listRequestPermission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            this.requestPermissions(strRequestPermission, REQUEST_CODE_LOC);
        }
    }


    /**
     * Метод для обработки результата запроса разрешений от устройства
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOC:

                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        // Check if request is granted or not
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                }
                break;
            default:
                return;
        }
    }

    /**
     * Класс для подключеня к устройству по bluetooth
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private boolean success = false;

        public ConnectThread(BluetoothDevice device) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);

                progressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                success = true;

                progressDialog.dismiss();
            } catch (IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "CONNECTION FAIL!",Toast.LENGTH_SHORT).show();
                    }
                });

                cancel();
            }

            if (success) {
                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showFrameGraphControls();
                    }
                });
            }
        }

        /**
         * Проверить состояние bluetooth соединения
         * @return
         */
        public boolean isConnect() {
            return bluetoothSocket.isConnected();
        }

        /**
         * Закрыть сооденение bluetooth
         */
        public void cancel() {
            try {
                Log.d(TAG, "cancel: " + this.getClass().getSimpleName());
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Класс для работы с уже подключенным соединением  bluetooth
     */
    private class ConnectedThread  extends  Thread {

        private final InputStream inputStream;
        private final OutputStream outputStream;
        private boolean isConnected = false;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.inputStream = inputStream;
            this.outputStream = outputStream;
            isConnected = true;
        }

        /**
         * Запусить поток отслеживания сигнала bluetooth
         */
        @Override
        public void run() {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            StringBuffer buffer = new StringBuffer();
            final StringBuffer sbConsole = new StringBuffer();

            while (isConnected) {
                try {
                    int bytes = bis.read();
                    buffer.append((char) bytes);
                    int eof = buffer.indexOf("\r\n");

                    if (eof > 0) {
                        sbConsole.append(buffer.toString());
                        lastSensorValues = buffer.toString();
                        buffer.delete(0, buffer.length());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                bis.close();
                cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Отправить сообщение по bluetooth
         */
        public void write(String command) {
            byte[] bytes = command.getBytes();
            if (outputStream != null) {
                try {
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Закрыть все потоки bluetooth
         */
        public void cancel() {
            try {
                isConnected = false;
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Включить или выключить запись сигнала датчиков
     * @param status
     */
    private void setSerialStatusOnDevice(boolean status) {
        if (connectedThread != null && connectThread.isConnect()) {
            String command;

            command = (status) ? "1" : "0";

            //C учетом зажержки итераций устройства
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < DEVICE_DELAY * 2){
                connectedThread.write(command);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    /**
     * Включить запись
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void record(){
        boolean isDelay = decrementRecordDelay();
        if(isDelay){
            Toast.makeText(MainActivity.this, "__DELAY PHASE__", Toast.LENGTH_SHORT).show();
        } else {
            boolean isRecord = incrementRecordTime();
            if(isRecord) {
                Toast.makeText(MainActivity.this, "__RECORD PHASE__", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "__RECORD END__", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Подготовиться к записи образца
     */
    private void prepareFromRecordingToWav(){
        try {
            String temp = String.valueOf(etRecordEndTime.getText());
            recordEndTime = Double.parseDouble(temp);
            etRecordCurrentTime.setText("0.0");

            temp = String.valueOf(etRecordDelay.getText());
            recordDelay = Double.parseDouble(temp);
            etRecordDelay.setEnabled(false);
            startTimeDelay = System.currentTimeMillis();

            recordStatus = RecordStatus.PREPARE;
        } catch (NumberFormatException e){
            Toast.makeText(MainActivity.this, "PARAMETERS RECORD ERROR!!!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Декремент таймера задержки записи образца
     */
    private boolean decrementRecordDelay(){
        if(recordStatus == RecordStatus.PREPARE){
            try {
                long time = (System.currentTimeMillis() - startTimeDelay);
                int second = (int) (time / 1000);
                int millisecond = (int) (time % 1000);
                String temp = second + "." + millisecond;

                double currentDelayTimeValue = recordDelay - Double.parseDouble(temp);
                if(currentDelayTimeValue >= 0.0){
                    etRecordDelay.setText(currentDelayTimeValue + "");
                } else {
                    recordStatus = RecordStatus.PROCESS;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "PROCESS DELAY RECORD ERROR!!!", Toast.LENGTH_SHORT).show();
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Икремент таймера записи образца
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean incrementRecordTime(){
        if(recordStatus == RecordStatus.PROCESS){
            try {
                long time = (System.currentTimeMillis() - startTimeRecord);
                int second = (int) (time / 1000);
                int millisecond = (int) (time % 1000);
                String temp = second + "." + millisecond;

                double currentTimeValue = Double.parseDouble(temp);
                if(currentTimeValue <= recordEndTime){
                    etRecordDelay.setText(currentTimeValue + "");
                } else {
                    recordStatus = RecordStatus.END;
                    etRecordDelay.setText(recordEndTime + "");

                    boolean statusSave = saveWave();
                    if(statusSave){
                        Toast.makeText(MainActivity.this, "SUCCESSFULLY SAVE!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "FAILED SAVE!", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "PROCESS TIME RECORD ERROR!!!", Toast.LENGTH_SHORT).show();
            }

            return true;
        } else {
            return false;
        }
    }


    /**
     * Сохранить образец
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean saveWave(){
        String pathParent = Environment.getExternalStorageDirectory() + "/MRecord/samples/";
        String pathChild = (etRecordName.getText() + LocalDateTime.now().toString() + ".wav")
                .replaceAll(":", "T")
                .replaceAll("\\.", "T");

        try {
            WavSaver.save(wave, recordEndTime, pathParent, pathChild);

            return true;
        } catch (IOException | WavFileException e) {
            return false;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Распарсить bluetooth сообщение для дальнейшей обработки
     * @param data
     * @return parsed bluetooth message
     */
    private HashMap<String, String> parseData(String data) {  //(VAL:3.56|TIME:23.40)
        HashMap<String, String> map = new HashMap();

        //Если это сообщение с сигналом
        if (data.indexOf('(') == 0 && data.indexOf(')') > 0) {
            String[] pairs = data
                    .replaceAll("\\(", "")
                    .replaceAll("\\)", "")
                    .split("\\|");

            for (String pair: pairs) {
                String[] keyValue = pair.split(":");
                map.put(keyValue[0], keyValue[1]);
            }

            return map;
        } else
            //Если это статусное сообщение
            if(lastSensorValues.indexOf('<') == 0 && lastSensorValues.indexOf('>') > 0){
            char[] tempArr = data
                    .replaceAll("<", "")
                    .replaceAll(">", "")
                    .toCharArray();

            //Для того чтобы убрать нежелательные символы которые трудно отследить
            String value = "";
            for(char t : tempArr){
                int temp = t;
                if(temp > 32){
                    value += t;
                }
            }

            map.put("STATUS", value);

            return map;
        }

        return null;
    }


    /**
     * Запуск потока таймера для обновления данных на GUI
     */
    private void startTimer() {
        cancelTimer();
        handler = new Handler();
        final MovementMethod movementMethod = new ScrollingMovementMethod();
        handler.post(timer = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                etConsole.setText(lastSensorValues);
                etConsole.setMovementMethod(movementMethod);

                //TODO Сделать оповещение о разрыве связи
                //Toast.makeText(MainActivity.this, "CONNECTION LOST!", Toast.LENGTH_SHORT).show();

                HashMap<String, String> dataSensor = parseData(lastSensorValues);
                if (dataSensor != null) {
                    if (dataSensor.containsKey("VAL") && dataSensor.containsKey("TIME")) {
                        float value = Float.parseFloat(dataSensor.get("VAL").toString());
                        float time = Float.parseFloat(dataSensor.get("TIME").toString());
                        if(time > xLastValue){
                            series.appendData(new DataPoint(time, value), true, maxDataPointsOnGraph * 100);
                        }

                        if(recordStatus == RecordStatus.PREPARE || recordStatus == RecordStatus.PROCESS){
                            record();
                        }

                        xLastValue = time;
                    } else if(dataSensor.containsKey("STATUS")){
                        String statusValue = dataSensor.get("STATUS");
                        //Код статуса ON
                        if(statusValue.equals("ON")){
                            indicator.setImageResource(R.drawable.indicatoron);

                        } else
                            //Код статуса OFF
                            if(statusValue.equals("OFF")){
                                indicator.setImageResource(R.drawable.indicatoroff);

                                if(recordStatus == RecordStatus.PREPARE){
                                    startTimeDelay = System.currentTimeMillis();
                                } else if(recordStatus == RecordStatus.PROCESS){
                                    startTimeRecord =  System.currentTimeMillis();
                                }
                            }
                        }
                    }

                handler.post(this);
            }
        });
    }

    /**
     * Остановка таймера
     */
    private void cancelTimer() {
        if (handler != null) {
            handler.removeCallbacks(timer);
        }
    }

}
