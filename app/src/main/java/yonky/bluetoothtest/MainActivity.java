package yonky.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements MyListener {
    Button btSwitch;
    Button btSearch;
    Button btSend;
    TextView text_state;
    TextView text_msg;

    RecyclerView mRecyclerView;
    MyAdapter adapter;

    BluetoothAdapter btAdapter;
    boolean isDiscovering;
    private ConnectThread connectThread;
    private ListenerThread mListenerThread;

    private static final UUID BT_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");
    private final int BUFFER_SIZE = 1024;
    private static final String NAME = "BT_DEMO";
//    private ConnectThread  connectThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btSwitch = findViewById(R.id.bt_switch);
        btSearch = findViewById(R.id.bt_search);
        text_state =findViewById(R.id.tv_state);
        text_msg= findViewById(R.id.tv_message);
        btSend = findViewById(R.id.bt_send);
        mRecyclerView = findViewById(R.id.recyclerview);

        adapter = new MyAdapter(this);
        RecyclerView.LayoutManager layoutManager= new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        adapter.setListener(this);
        mRecyclerView.setAdapter(adapter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        initReceiver();

        mListenerThread = new ListenerThread();
        mListenerThread.start();

        btSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btAdapter==null){
                    Toast.makeText(MainActivity.this,"当前设备不支持蓝牙功能",Toast.LENGTH_SHORT).show();
                }else{
                    if(!btAdapter.isEnabled()){
                        btAdapter.enable();
                    }
                    //开启被其他蓝牙设备发现
                    if(btAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        //设置一直开启
                        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,0);
                        startActivity(i);
                    }
                }

            }
        });

        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btAdapter!=null){
                    btAdapter.startDiscovery();
                    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                    if(pairedDevices.size()>0){
                        for(BluetoothDevice device:pairedDevices){
                            adapter.addDevice(device);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }


             }
        });

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectThread!=null){
                    connectThread.sendMsg("这是蓝牙发送过来的数据");
                }
            }
        });



    }

    private void initReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);
    }

    private  BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED){
                    adapter.addDevice(device);
                    adapter.notifyDataSetChanged();
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Toast.makeText(MainActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
                isDiscovering =true;
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(MainActivity.this,"搜索完毕", Toast.LENGTH_SHORT).show();
                isDiscovering=false;
            }
        }
    };

    @Override
    public void onClick(BluetoothDevice device) {
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        text_state.setText("正在连接");
        try{
            BluetoothSocket socket =device.createRfcommSocketToServiceRecord(BT_UUID);
            connectThread =new ConnectThread(socket,true);
            connectThread.start();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消搜索
        if(btAdapter!=null&&btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        //注销Broadcstreceiver,防治资源泄漏
        unregisterReceiver(mReceiver);
    }

    /**
     * 连接线程
     */
  private class ConnectThread extends Thread {
      private BluetoothSocket socket;
      private boolean activeConnect;
      InputStream mInputStream;
      OutputStream mOutputStream;
      private ConnectThread(BluetoothSocket socket,boolean connect){
          this.socket = socket;
          this.activeConnect = connect;
      }

        @Override
        public void run() {
            try{
                //如果主动连接则调用连接方法
                if(activeConnect){
                    socket.connect();
                }
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText("连接成功");
                    }
                });
                mInputStream = socket.getInputStream();
                mOutputStream = socket.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while(true){
//                    读取数据
                    bytes=mInputStream.read(buffer);
                    if(bytes>0){
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer,0,data,0,bytes);
                        text_msg.post(new Runnable(){
                            @Override
                            public void run() {
                                text_msg.setText("收到消息："+new String(data));
                            }
                        });
                    }
                }

            }catch (IOException e){
                e.printStackTrace();
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText("连接失败");
                    }
                });
            }
        }
        //发送数据
        public void sendMsg(final String msg){
          byte[] bytes = msg.getBytes();
          if(mOutputStream!=null){
              try{
                  //发送数据
                  mOutputStream.write(bytes);
                  text_msg.post(new Runnable() {
                      @Override
                      public void run() {
                          text_msg.setText("发送消息"+msg);
                      }
                  });
              }catch (IOException e){
                  e.printStackTrace();
                  text_msg.post(new Runnable() {
                      @Override
                      public void run() {
                          text_msg.setText("发送消息失败:"+msg);
                      }
                  });
              }
          }
        }
    }
//监听线程
    private class ListenerThread extends Thread{
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        @Override
    public void run(){
            try{
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(NAME,BT_UUID);
                while(true){
                    //线程阻塞，等待别的设备连接
                    socket=serverSocket.accept();
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText("连接中");
                        }
                    });
                    connectThread = new ConnectThread(socket,false);
                    connectThread.start();

                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


}
