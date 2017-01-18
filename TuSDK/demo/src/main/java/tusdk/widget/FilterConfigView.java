/**
 * TuSDKLiveDemo
 * LiveStreamingActivity.java
 *
 * @author 		Yanlin
 * @Date 		2016-4-15 上午10:36:28
 * @Copyright 	(c) 2016 tusdk.com. All rights reserved.
 *
 */
package tusdk.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.lasque.tusdk.core.seles.SelesParameters;
import org.lasque.tusdk.core.seles.SelesParameters.FilterArg;
import org.lasque.tusdk.core.seles.SelesParameters.FilterParameterInterface;
import org.lasque.tusdk.core.seles.sources.SelesOutInput;
import org.lasque.tusdk.core.utils.ContextUtils;
import org.lasque.tusdk.core.utils.anim.AccelerateDecelerateInterpolator;
import org.lasque.tusdk.core.utils.anim.AnimHelper;
import org.lasque.tusdk.core.view.TuSdkRelativeLayout;
import org.lasque.tusdk.core.view.TuSdkViewHelper;
import org.lasque.tusdk.core.view.TuSdkViewHelper.OnSafeClickListener;

import java.util.ArrayList;

import tusdk.widget.FilterConfigSeekbar.FilterConfigSeekbarDelegate;

/**
 * 滤镜配置视图
 *
 * @author Clear
 */
public class FilterConfigView extends TuSdkRelativeLayout
{

    /**
     * 滤镜配置视图委托
     *
     * @author Clear
     */
    public interface FilterConfigViewDelegate
    {
        /**
         * 通知重新绘制
         *
         * @param configView
         */
        void onFilterConfigRequestRender(FilterConfigView configView);
    }

    /**
     * 滤镜配置视图委托
     */
    private FilterConfigViewDelegate mDelegate;

    /**
     * 滤镜配置视图委托
     *
     * @return the mDelegate
     */
    public FilterConfigViewDelegate getDelegate()
    {
        return mDelegate;
    }

    /**
     * 滤镜配置视图委托
     *
     * @param mDelegate
     *            the mDelegate to set
     */
    public void setDelegate(FilterConfigViewDelegate mDelegate)
    {
        this.mDelegate = mDelegate;
    }

    public FilterConfigView(Context context)
    {
        super(context);
    }

    public FilterConfigView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public FilterConfigView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    // 重置按钮
    private View mResetButton;
    // 显示状态按钮
    private View mStateButton;
    // 状态背景
    private View mStateBg;
    // 配置包装
    private LinearLayout mConfigWrap;

    /**
     * 重置按钮
     *
     * @return the mResetButton
     */
    public View getResetButton()
    {
        if (mResetButton == null)
        {
            mResetButton = this.getViewById("lsq_resetButton");
            if (mResetButton != null)
            {
                mResetButton.setOnClickListener(mOnClickListener);
            }
        }
        return mResetButton;
    }

    /**
     * 显示状态按钮
     *
     * @return the mStateButton
     */
    public View getStateButton()
    {
        if (mStateButton == null)
        {
            mStateButton = this.getViewById("lsq_stateButton");
            if (mStateButton != null)
            {
                mStateButton.setOnClickListener(mOnClickListener);
            }
        }
        return mStateButton;
    }

    /**
     * 状态背景
     *
     * @return
     */
    public View getStateBg()
    {
        if (mStateBg == null)
        {
            mStateBg = this.getViewById("lsq_stateBg");
        }
        return mStateBg;
    }

    /**
     * 配置包装
     *
     * @return
     */
    public LinearLayout getConfigWrap()
    {
        if (mConfigWrap == null)
        {
            mConfigWrap = this.getViewById("lsq_configWrap");
        }
        return mConfigWrap;
    }

    @Override
    public void loadView()
    {
        super.loadView();
        // 拖动条高度
        this.mSeekHeigth = ContextUtils.dip2px(this.getContext(), 50);
        this.showViewIn(this.getResetButton(), false);
        ViewCompat.setAlpha(this.getStateButton(), 0.7f);
        ViewCompat.setAlpha(this.getStateBg(), 0);
        this.showViewIn(this.getConfigWrap(), false);
    }

    /**
     * 设置滤镜
     *
     * @param filter
     */
    public void setSelesFilter(SelesOutInput filter)
    {
        if (filter == null || !(filter instanceof FilterParameterInterface))
        {
            this.hiddenDefault();
            return;
        }

        this.showViewIn(true);
        this.resetConfigView(this.getConfigWrap(), (FilterParameterInterface) filter);

        AnimHelper.heightAnimation(this.getStateBg(), mStateBgTotalHeight);
    }

    /**
     * 设置隐藏为默认状态
     */
    public void hiddenDefault()
    {
        this.showViewIn(false);
        this.showViewIn(this.getResetButton(), false);
        this.showViewIn(this.getConfigWrap(), false);
        ViewCompat.setAlpha(this.getConfigWrap(), 0);
        ViewCompat.setRotation(this.getStateButton(), 0);
        ViewCompat.setAlpha(this.getStateButton(), 0.7f);
        ViewCompat.setAlpha(this.getStateBg(), 0);
        this.setHeight(this.getStateBg(), 0);
    }

    /**
     * 滤镜对象
     */
    private FilterParameterInterface mFilter;

    /**
     * 滤镜配置拖动栏列表
     */
    private ArrayList<FilterConfigSeekbar> mSeekbars;

    /**
     * 状态背景总高度
     */
    private int mStateBgTotalHeight;

    /**
     * 拖动条高度
     */
    private int mSeekHeigth;

    /**
     * 配置视图
     *
     * @param configWrap
     * @param filter
     */
    private void resetConfigView(LinearLayout configWrap, FilterParameterInterface filter)
    {
        mFilter = filter;
        if (configWrap == null || mFilter == null) return;

        mStateBgTotalHeight = configWrap.getTop() + this.mSeekHeigth / 2;

        // 删除所有视图
        configWrap.removeAllViews();
        SelesParameters params = mFilter.getParameter();

        if (params == null || params.size() == 0)
        {
            this.hiddenDefault();
            return;
        }

        mSeekbars = new ArrayList<FilterConfigSeekbar>(params.size());

        for (FilterArg arg : params.getArgs())
        {
            FilterConfigSeekbar seekbar = this.buildAppendSeekbar(configWrap, this.mSeekHeigth);
            if (seekbar != null)
            {
                // 设置滤镜配置参数
                seekbar.setFilterArg(arg);
                seekbar.setDelegate(mFilterConfigSeekbarDelegate);
                mSeekbars.add(seekbar);
                mStateBgTotalHeight += this.mSeekHeigth;
            }
        }
    }

    /**
     * 创建并添加滤镜配置拖动栏
     *
     * @param parent
     *            父视图
     * @return
     */
    public FilterConfigSeekbar buildAppendSeekbar(LinearLayout parent, int height)
    {
        if (parent == null) return null;

        FilterConfigSeekbar seekbar = TuSdkViewHelper.buildView(this.getContext(), FilterConfigSeekbar.getLayoutId(), parent);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
        parent.addView(seekbar, params);
        return seekbar;
    }

    /**
     * 滤镜配置拖动栏委托
     */
    protected FilterConfigSeekbarDelegate mFilterConfigSeekbarDelegate = new FilterConfigSeekbarDelegate()
    {
        /**
         * 配置数据改变
         *
         * @param seekbar
         *            滤镜配置拖动栏
         * @param arg
         *            滤镜参数
         */
        public void onSeekbarDataChanged(FilterConfigSeekbar seekbar, FilterArg arg)
        {
            requestRender();
        }
    };

    /**
     * 按钮点击事件
     */
    protected OnSafeClickListener mOnClickListener = new OnSafeClickListener()
    {
        @Override
        public void onSafeClick(View v)
        {
            if (equalViewIds(v, getResetButton()))
            {
                handleResetAction();
            }
            else if (equalViewIds(v, getStateButton()))
            {
                handleShowStateAction();
            }
        }
    };

    // 是否正在进行动画
    private boolean mIsAniming;

    /**
     * 设置滤镜配置选线显示状态
     */
    protected void handleShowStateAction()
    {
        if (this.getConfigWrap() == null || mIsAniming) return;

        mIsAniming = true;

        final boolean isShow = (this.getConfigWrap().getVisibility() == View.VISIBLE);

        this.showViewIn(this.getResetButton(), !isShow);
        this.showViewIn(this.getConfigWrap(), true);

        ViewCompat.animate(this.getConfigWrap()).alpha(isShow ? 0 : 1).setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationEnd(View view)
                    {
                        if (isShow)
                        {
                            showViewIn(getConfigWrap(), false);
                        }
                        mIsAniming = false;
                    }
                });

        ViewCompat.animate(this.getStateButton()).rotation(isShow ? 0 : 90).alpha(isShow ? 0.7f : 1f).setDuration(260)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        ViewCompat.animate(this.getStateBg()).alpha(isShow ? 0 : 1).setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator());

        AnimHelper.heightAnimation(this.getStateBg(), isShow ? 0 : mStateBgTotalHeight);
    }

    /**
     * 重置滤镜配置选项
     */
    protected void handleResetAction()
    {
        if (mSeekbars == null) return;

        for (FilterConfigSeekbar seekbar : mSeekbars)
        {
            seekbar.reset();
        }

        this.requestRender();
    }

    /**
     * 请求渲染
     */
    protected void requestRender()
    {
        if (mFilter != null)
        {
            mFilter.submitParameter();
        }

        if (mDelegate != null)
        {
            mDelegate.onFilterConfigRequestRender(this);
        }
    }
}
