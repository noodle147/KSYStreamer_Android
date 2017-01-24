package com.ksyun.media.diversity.sticker.demo.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.ksyun.media.diversity.sticker.demo.MaterialInfoItem;
import com.ksyun.media.diversity.sticker.demo.R;

import java.util.List;

public class HorizontalListViewAdapter extends BaseAdapter {
	private List<MaterialInfoItem> mImageList;
	private Context mContext;
	private LayoutInflater mInflater;
	Bitmap iconBitmap;
	private int selectIndex = -1;


	public HorizontalListViewAdapter(Context context, List<MaterialInfoItem> adsList){
		this.mContext = context;
		this.mImageList = adsList;
		mInflater=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//LayoutInflater.from(mContext);
	}
	@Override
	public int getCount() {
		if(mImageList == null){
			return 0;
		}
		return mImageList.size();
	}
	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;
		if(convertView==null){
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.horizontal_list_item, null);
			holder.mImage=(ImageView)convertView.findViewById(R.id.img_list_item);
			holder.mTitle=(TextView)convertView.findViewById(R.id.text_list_item);
			convertView.setTag(holder);
		}else{
			holder=(ViewHolder)convertView.getTag();
		}
		if(position == selectIndex){
			convertView.setSelected(true);
		}else{
			convertView.setSelected(false);
		}

		String text = mContext.getString(R.string.ad_cost_info) + mImageList.get(position).material.price;
	//	holder.mTitle.setText(mImageList.get(position).material.price);
		holder.mTitle.setText(text);
	//	BitmapDrawable ob = new BitmapDrawable(mImageList.get(position).thumbnail);
	//	holder.mImage.setBackgroundDrawable(ob);
		holder.mImage.setImageBitmap(mImageList.get(position).thumbnail);

		return convertView;
	}

	private static class ViewHolder {
		private TextView mTitle ;
		private ImageView mImage;
	}


	public void setSelectIndex(int i){
		selectIndex = i;
	}
}