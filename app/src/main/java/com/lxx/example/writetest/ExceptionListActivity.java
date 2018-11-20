package com.lxx.example.writetest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ExceptionListActivity extends AppCompatActivity {
    private ListView listView;
    private List<ExceptionBean> exceptionList;
    private ExceptionAdapter mAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exception_list);
        listView = findViewById(R.id.exception_list);
        exceptionList = new ArrayList<>();
        exceptionList.addAll(ExceptionBean.getExceptionList());
        mAdapter = new ExceptionAdapter();
        listView.setAdapter(mAdapter);
    }


    private class ExceptionAdapter extends BaseAdapter{
        @Override
        public int getCount(){
            return exceptionList.size();
        }

        @Override
        public Object getItem(int positon){
            return exceptionList.get(positon);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.exception_item,parent,false);
                viewHolder.id = convertView.findViewById(R.id.id);
                viewHolder.time = convertView.findViewById(R.id.time);
                viewHolder.content = convertView.findViewById(R.id.content);
                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder)convertView.getTag();
            }
            ExceptionBean exception = exceptionList.get(position);
            if(exception != null){
                viewHolder.id.setText(String.valueOf(exception.getId()));
                viewHolder.time.setText(exception.getTime());
                viewHolder.content.setText(exception.getContent());
            }
            return convertView;
        }
    }

    private class ViewHolder{
        TextView id;
        TextView time;
        TextView content;
    }
}
