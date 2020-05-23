package com.example.sqlserverquery;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Connection m_con = null;

    private List<String> fileList = new ArrayList<>();
    private String file_right;
    ArrayList<Float> PositionList=new ArrayList<Float>();
    public static final int UPDATE_TEXT = 1;
    public static final int UPDATE_DATA = 2;
    Timer timer = null;
    Timer tt = null;
    final sqlDB sql = new sqlDB();
    Button bt1,bt2;
   // EditText filename;
    TextView screen;
    private PowerManager.WakeLock wakeLock = null;

    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_TEXT:
                    ListView listView = (ListView) findViewById(R.id.list_view);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            MainActivity.this, android.R.layout.simple_list_item_1, (List<String>) msg.obj
                    );
                    fileList =  (List<String>) msg.obj;//将文件名传出new thread
                    listView.setAdapter(adapter);//operation
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                        @Override
                        public void onItemClick(AdapterView<?> parent,View view,
                                                 int position, long id){
                            file_right = fileList.get(position);
                            //choose one file and prepare for query
                            Toast.makeText(MainActivity.this,file_right,Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;
                case UPDATE_DATA:
                    float[] Pdata = (float[]) msg.obj;//将new thread里的数据传出来
                    StringBuilder result = new StringBuilder();
                    result.append("x坐标： ");
                    result.append(Pdata[0]);
                    result.append("\n");
                    result.append("y坐标： ");
                    result.append(Pdata[1]);
                    result.append("\n");
                    result.append("z坐标： ");
                    result.append(Pdata[2]);
                    result.append("\n");
                    screen.setText(result.toString());
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bt1 = (Button) findViewById(R.id.query);
       // filename = (EditText) findViewById(R.id.edit_text);
        screen = (TextView) findViewById(R.id.screen);
        bt2 = (Button) findViewById(R.id.stop);
        Button bt3 = (Button)findViewById(R.id.refresh);

        bt1.setOnClickListener(this);
        bt2.setOnClickListener(this);
        bt3.setOnClickListener(this);

        bt2.setEnabled(false);

        //获取电源锁
        acquire_wakelock();


        new Thread() {
            public void run() {
                try {

                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    Log.d("加载驱动", "成功");//ok
                    m_con = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ "129.211.151.223" + ":1433/" + "BOOK" ,"sa", "azsxdcfv123,");

                    if(m_con!=null)
                        Log.d("sqlserver", "数据库连接成功");  //上传调用
                    m_con.setAutoCommit(false);
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();


        if(tt==null){
            tt = new Timer();
        }
        tt.schedule(new TimerTask()
        {
            @Override
            public void run() {

                new Thread() {
                    public void run() {
                        try {
                            //fileList.clear();
                            Message message = new Message();
                            message.what = UPDATE_TEXT;

                            message.obj = sql.update(m_con);//update the file name
                            handler.sendMessage(message);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }.start();

            }
        },10,60000);

    }

    private  void acquire_wakelock(){
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, getClass()
                .getCanonicalName());
        wakeLock.acquire(1*60*1000L /*1 minutes*/);
        if(wakeLock != null){
            Log.d("wakelock", "成功");
        }
    }

    private void releaseWakeLock() {
        if (null != wakeLock && wakeLock.isHeld()) {
            Log.i("call releaseWakeLock", "成功");
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.query:
                if (timer == null) {timer = new Timer();}

                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run() {
                        new Thread() {
                            public void run() {
                                try {
                                    float[] PData =new float[3];
                                    PData = sql.query(file_right,m_con);
                                    PositionList.add(PData[0]);
                                    PositionList.add(PData[1]);
                                    PositionList.add(PData[2]);
                                    Message message = new Message();
                                    message.what = UPDATE_DATA;
                                    message.obj = PData;
                                    handler.sendMessage(message);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }.start();

                    }
                 },10,1000);


                bt2.setEnabled(true);


                break;
            case R.id.stop:
                timer.cancel();
                timer = null;
                PositionList.clear();
                bt2.setEnabled(false);
                break;
            case R.id.refresh:
                new Thread() {
                    public void run() {
                        try {
                            //fileList.clear();
                            Message message = new Message();
                            message.what = UPDATE_TEXT;

                            message.obj = sql.update(m_con);//update the file name
                            handler.sendMessage(message);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
                break;
            default:
                break;
        }
    }


    protected void onDestroy(){
        super.onDestroy();
        try {
            m_con.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        PositionList.clear();
        fileList.clear();
        tt.cancel();
        tt = null;
        timer.cancel();//退出采集任务
        timer=null;
        releaseWakeLock();

    }
}

