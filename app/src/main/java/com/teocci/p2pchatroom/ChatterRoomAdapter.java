package com.teocci.p2pchatroom;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ChatterRoomAdapter extends BaseAdapter
{
    Context mContext;
    List<NsdServiceInfo> mChatterList;


    public ChatterRoomAdapter(Context ctx)
    {
        mContext = ctx;
        mChatterList = new ArrayList<NsdServiceInfo>();
    }

    @Override
    public int getCount()
    {
        return mChatterList.size();
    }

    public void setChatterList(List<NsdServiceInfo> cList)
    {
        mChatterList = cList;
    }

    @Override
    public Object getItem(int position)
    {
        return mChatterList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Only inflate when necessary
        // View listItemView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_line, null);
        }

        TextView tv = (TextView) convertView.findViewById(R.id.userName);
        tv.setText(mChatterList.get(position).getServiceName());

        return convertView;
    }

}
