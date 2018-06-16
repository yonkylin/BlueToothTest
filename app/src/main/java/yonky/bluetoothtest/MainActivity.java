package yonky.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    TextView text_state;

    RecyclerView mRecyclerView;
    MyAdapter adapter;

    BluetoothAdapter btAdapter;
    boolean isDiscovering;

    private static final UUID BT_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");
//    private ConnectThread  connectThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btSwitch = findViewById(R.id.bt_switch);
        btSearch = findViewById(R.id.bt_search);
        text_state =findViewById(R.id.tv_state);
        mRecyclerView = findViewById(R.id.recyclerview);

        adapter = new MyAdapter(this);
        RecyclerView.LayoutManager layoutManager= new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        adapter.setListener(this);
        mRecyclerView.setAdapter(adapter);

        initReceiver();

        btSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btAdapter =BluetoothAdapter.getDefaultAdapter();
                if(btAdapter==null){
                    Toast.makeText(MainActivity.this,"当前设备不支持蓝牙功能",Toast.LENGTH_SHORT).show();
                }
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
        });

        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btAdapter.startDiscovery();
                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                if(pairedDevices.size()>0){
                    for(BluetoothDevice device:pairedDevices){
                        adapter.addDevice(device);
                        adapter.notifyDataSetChanged();
                    }
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
//        try{
//            BluetoothSocket socket =device.createRfcommSocketToServiceRecord(BT_UUID);
//            connectThread =
//        }
    }

    /**
     * 连接线程
     */
/*    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private boolean activeConnect;
        InputStream inputStream;
        OutputStream outputStream;

        private ConnectThread(BluetoothSocket socket, boolean connect) {
            this.socket = socket;
            this.activeConnect = connect;
        }

        @Override
        public void run() {
            try {
                //如果是自动连接 则调用连接方法
                if (activeConnect) {
                    socket.connect();
                }
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText(getResources().getString(R.string.connect_success));
                    }
                });
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while (true) {
                    //读取数据
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        text_msg.post(new Runnable() {
                            @Override
                            public void run() {
                                text_msg.setText(getResources().getString(R.string.get_msg)+new String(data));
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText(getResources().getString(R.string.connect_error));
                    }
                });
            }
        }*/

}
