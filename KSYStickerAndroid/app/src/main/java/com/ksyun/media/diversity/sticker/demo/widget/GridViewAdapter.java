package com.ksyun.media.diversity.sticker.demo.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ksyun.media.diversity.sticker.demo.MaterialInfoItem;
import com.ksyun.media.diversity.sticker.demo.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sensetime on 16-7-26.
 */
public class GridViewAdapter extends BaseAdapter {
    private String TAG = "GridViewAdapter";
    private List<MaterialInfoItem> mImageList;
    private Context mContext;
    private LayoutInflater mInflater;
    Bitmap iconBitmap;
    private int selectIndex = -1;
    private GridView mGridView;
    private int TYPE_CPC = 1;
    private int TYPE_CPM = 2;
    public static int STATE_INIT = 0;
    public static int STATE_DOWNLOADING = 1;
    public static int STATE_DOWNLOADED = 2;
    public static int STATE_COOLDOWNING = 3;
    public static int STATE_STICKER_READY = 4;
    public static int STATE_DOWNLOADTHUMBNAIL = 5;
    private Map<Integer,Integer> mItemState;
    private boolean mIsAd;

    @Override
    public int getCount() {
        if(mImageList == null){
            return 0;
        }
        return mImageList.size();
    }
    public GridViewAdapter(Context context, List<MaterialInfoItem> materialList, boolean isAd){
        this.mContext = context;
        this.mImageList = materialList;
        mIsAd = isAd;
        mInflater=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//LayoutInflater.from(mContext);
        int count = getCount();
        mItemState = new HashMap<Integer, Integer>(count);
        for(int i = 0; i < mImageList.size(); i++) {
            mItemState.put(i,STATE_INIT);
        }
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
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.gridview_list_item, null);
            holder.mImage=(ImageView)convertView.findViewById(R.id.gimg_list_item);
            holder.mTitle=(TextView)convertView.findViewById(R.id.gtext_list_item);
            holder.mDLIndImage = (ImageView)convertView.findViewById(R.id.download_ind);
            holder.mTypeView = (ImageView)convertView.findViewById(R.id.ads_type);
            holder.mDownloadProgress = (ProgressBar) convertView.findViewById(R.id.process_download);
            holder.mCoolDownProgressBar = (CircleProgressBar)convertView.findViewById(R.id.circle_progressbar);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
            updateItemView(holder,position);
        }

        if (mItemState.get(position) == null) {
            return convertView;
        }

        if(mItemState.get(position) == STATE_STICKER_READY || (position == 0) || mItemState.get(position) == STATE_INIT) {
            if (position == selectIndex) {
                convertView.setSelected(true);
                holder.mImage.setBackgroundResource(R.drawable.choose);
            }
        }

        if (position != selectIndex) {
            convertView.setSelected(false);
            holder.mImage.setBackgroundColor(Color.TRANSPARENT);
        }

        String text = mImageList.get(position).material.price;
        if(position == 0){
            holder.mTitle.setText(mContext.getResources().getString(R.string.null_sticker));
            holder.mTypeView.setVisibility(View.GONE);
            holder.mDLIndImage.setVisibility(View.GONE);
        }else {
            holder.mTitle.setVisibility(View.VISIBLE);
            if (mIsAd) {
                text += mContext.getString(R.string.price_unit);
                holder.mTitle.setText(text);
            } else {
                holder.mTitle.setText("");
            }
            if(mImageList.get(position).commissionType == TYPE_CPC){
                holder.mTypeView.setVisibility(View.VISIBLE);
                holder.mTypeView.setImageResource(R.drawable.hand);
            }else if(mImageList.get(position).commissionType == TYPE_CPM){
                holder.mTypeView.setVisibility(View.VISIBLE);
                holder.mTypeView.setImageResource(R.drawable.eye);
            }else{
                holder.mTypeView.setVisibility(View.GONE);
            }

            if(mImageList.get(position).hasDownload){
                Log.d(TAG,"position "+position+" have downloaded");
                holder.mDLIndImage.setVisibility(View.GONE);
                holder.mDownloadProgress.setVisibility(View.GONE);
                if((mImageList.get(position).intervalTime != 0) & mImageList.get(position).mTriggerCoolDown){
                    if(mItemState.get(position) != STATE_COOLDOWNING) {
                        holder.mCoolDownProgressBar.setVisibility(View.VISIBLE);
                        changeLight(holder.mImage,-100);
                        holder.mCoolDownProgressBar.setMaxProgress(mImageList.get(position).intervalTime*1000);
                        new CountDownTimer(mImageList.get(position).intervalTime * 1000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                holder.mCoolDownProgressBar.setProgress(mImageList.get(position).intervalTime*1000-(int) millisUntilFinished);
                            }

                            @Override
                            public void onFinish() {
                                holder.mCoolDownProgressBar.setVisibility(View.GONE);
                                mImageList.get(position).setTriggerCoolDowntag(false);
                                changeLight(holder.mImage,0);
                                mItemState.put(position,STATE_STICKER_READY);
                            }
                        }.start();
                        mItemState.put(position,STATE_COOLDOWNING);
                    }
                }else{
                    mItemState.put(position,STATE_STICKER_READY);
                }
            }
        }

        holder.mImage.setImageBitmap(mImageList.get(position).thumbnail);

        return convertView;
    }

    public boolean isCoolDowning(int position){
        if (mItemState == null || mItemState.size() <= position) {
            return true;
        }
        return mItemState.get(position) == STATE_COOLDOWNING ? true:false;
    }

    public void triggerCoolDown(int position){
        Log.d(TAG,"triggerCoolDown for position "+position);
        mImageList.get(position).setTriggerCoolDowntag(true);
    }

    public void updateItemView(int position) {
        int state = mItemState.get(position);
        Log.d(TAG,"updateItemView for position "+position+" ,the state is "+state);
        Log.d(TAG,"updateItemView FirstVisiblePosition is "+mGridView.getFirstVisiblePosition()+" ,LastVisiblePosition is"+mGridView.getLastVisiblePosition());

        if (position >= mGridView.getFirstVisiblePosition()
                && position <= mGridView.getLastVisiblePosition()) {
            if (state == STATE_DOWNLOADING) {
                Log.d(TAG,"updateItemView state is DOWNLOADING");
                View view = mGridView.getChildAt(position
                        - mGridView.getFirstVisiblePosition());
                ProgressBar process = (ProgressBar) view.findViewById(R.id.process_download);
                ImageView thumbImage = (ImageView) view.findViewById(R.id.gimg_list_item);
                changeLight(thumbImage, -100);
                process.setVisibility(View.VISIBLE);
            } else if (state == STATE_DOWNLOADED) {
                Log.d(TAG,"updateItemView state is DOWNLOADED");
                View view = mGridView.getChildAt(position
                        - mGridView.getFirstVisiblePosition());
                ImageView dlIndImage = (ImageView) view.findViewById(R.id.download_ind);
                ProgressBar process = (ProgressBar) view.findViewById(R.id.process_download);
                ImageView thumbImage = (ImageView) view.findViewById(R.id.gimg_list_item);
                changeLight(thumbImage, 0);
                process.setVisibility(View.GONE);
                dlIndImage.setVisibility(View.GONE);
            }if (state == STATE_DOWNLOADTHUMBNAIL) {
                Log.d(TAG,"updateItemView state is STATE_DOWNLOADTHUMBNAIL");
                View view = mGridView.getChildAt(position
                        - mGridView.getFirstVisiblePosition());
                ImageView thumbImage = (ImageView) view.findViewById(R.id.gimg_list_item);
                changeLight(thumbImage, -100);
            }
        }
    }

    public void updateItemView(ViewHolder holder, int position) {
        if (mItemState.get(position) == null) {
            return ;
        }
        int state = mItemState.get(position);
        Log.d(TAG,"updateItemView with holder for position "+position+" ,the state is "+state);

        if(state == STATE_DOWNLOADING) {
            Log.d(TAG,"updateItemView with holder at state STATE_DOWNLOADING");
            holder.mDownloadProgress.setVisibility(View.VISIBLE);
            holder.mDLIndImage.setVisibility(View.VISIBLE);
            changeLight(holder.mImage, -100);
        }else if (state == STATE_DOWNLOADED) {
            Log.d(TAG,"updateItemView with holder at state STATE_DOWNLOADED");
            holder.mDownloadProgress.setVisibility(View.GONE);
            changeLight(holder.mImage, 0);
            holder.mDLIndImage.setVisibility(View.GONE);
        }else{
            Log.d(TAG,"updateItemView with holder");
            holder.mDownloadProgress.setVisibility(View.GONE);
            changeLight(holder.mImage, 0);
            if(mImageList.get(position).hasDownload){
                holder.mDLIndImage.setVisibility(View.GONE);
            }else{
                holder.mDLIndImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void changeLight(ImageView imageview, int brightness) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[] { 1, 0, 0, 0, brightness, 0, 1, 0, 0,
                brightness, 0, 0, 1, 0, brightness, 0, 0, 0, 1, 0 });
        imageview.setColorFilter(new ColorMatrixColorFilter(matrix));

    }

    public void setGridView(GridView view){
        mGridView = view;
    }

    private static class ViewHolder {
        private TextView mTitle ;
        private ImageView mImage;
        private ImageView mDLIndImage;
        private ImageView mTypeView;
        private ProgressBar mDownloadProgress;
        private CircleProgressBar mCoolDownProgressBar;
    }


    public void setSelectIndex(int i){
        selectIndex = i;
        notifyDataSetChanged();
    }

    public void setItemState(int position, int state){
        mItemState.put(position, state);
    }

    public int getItemState(int position){
        return mItemState.get(position);
    }
}
