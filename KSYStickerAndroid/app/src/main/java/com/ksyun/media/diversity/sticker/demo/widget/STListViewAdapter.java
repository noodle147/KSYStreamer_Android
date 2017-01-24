package com.ksyun.media.diversity.sticker.demo.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.ksyun.media.diversity.sticker.demo.R;


/**
 * Created by sensetime on 16-7-29.
 */
public class STListViewAdapter extends BaseAdapter {
    private Context mContext;
    private int mTypeCount = 2;
    private LayoutInflater mInflater;
    private int selectIndex = -1;

    int selector[] = {R.drawable.sticker_type_selector,R.drawable.ads_type_selector};

    public STListViewAdapter(Context context){
        mContext = context;
        mInflater=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mTypeCount;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.sticker_type_list_item, null);
            holder.mImage = (ImageView)convertView.findViewById(R.id.sticker_type_item);
            convertView.setTag(holder.mImage);
        }else{
            holder=(ViewHolder) convertView.getTag();
        }

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams)(holder.mImage.getLayoutParams());
        params.width = width/2;
        holder.mImage.setLayoutParams(params);

        if(position == selectIndex){
            convertView.setSelected(true);
        }else{
            convertView.setSelected(false);
        }
        holder.mImage.setImageResource(selector[position]);
        return convertView;
    }

    private static class ViewHolder {
        private ImageView mImage;
    }

    public void setSelectIndex(int i) {
        selectIndex = i;
    }
}
