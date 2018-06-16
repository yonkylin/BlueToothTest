package yonky.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/6/16.
 */

public class MyAdapter extends RecyclerView.Adapter {
    Context mContext;
    List<BluetoothDevice> list;
    MyListener listener;
    public MyAdapter(Context context) {
        mContext = context;
        list = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(mContext).inflate(R.layout.item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            ((MyHolder)holder).name.setText(list.get(position).getName());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(list.get(position));
                }
            });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addDevice(BluetoothDevice device){
        list.add(device);
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView name = itemView.findViewById(R.id.tv_name);
        public MyHolder(View itemView) {
            super(itemView);
        }
    }

    public void setListener(MyListener listener) {
        this.listener = listener;
    }
}
