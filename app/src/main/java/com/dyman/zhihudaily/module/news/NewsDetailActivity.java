package com.dyman.zhihudaily.module.news;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dyman.zhihudaily.R;
import com.dyman.zhihudaily.ZhiHuDailyApp;
import com.dyman.zhihudaily.base.BaseActivity;
import com.dyman.zhihudaily.base.IntentKeys;
import com.dyman.zhihudaily.entity.NewsDetailInfo;
import com.dyman.zhihudaily.entity.StoryExtraInfo;
import com.dyman.zhihudaily.network.RetrofitHelper;
import com.dyman.zhihudaily.utils.common.DisplayUtil;
import com.dyman.zhihudaily.utils.common.WebUtils;
import com.dyman.zhihudaily.utils.helper.ScrollPullDownHelper;
import com.dyman.zhihudaily.utils.common.ToastUtil;
import com.dyman.zhihudaily.widget.MyImageTextLayout;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NewsDetailActivity extends BaseActivity implements ViewTreeObserver
        .OnScrollChangedListener, View.OnClickListener{

    private static final String TAG = NewsDetailActivity.class.getSimpleName();
    /** 新闻ID */
    private int newsID;
    /** 新闻背景控件 */
    private MyImageTextLayout imageTextLayout;
    private ScrollView mScrollView;
    private WebView webView;
    private Toolbar toolbar;
    private TextView markNumTv;
    private TextView commentNumTv;
    /** ScrollView 下滑监听帮助类 */
    private ScrollPullDownHelper mScrollPullDownHelper;
    /** 状态栏高度 */
    private float statusHeight = 0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        initToolbar();
        init();

        mScrollPullDownHelper = new ScrollPullDownHelper();
    }


    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.story_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }


    private void init() {

        // 初始化数据
        newsID = getIntent().getIntExtra(IntentKeys.NEWS_ID, 0);
        statusHeight = DisplayUtil.getStatusBarHeight(ZhiHuDailyApp.getInstance());
        //  实例化Toolbar部件的监听
        findViewById(R.id.share_iv_status).setOnClickListener(this);
        findViewById(R.id.collect_iv_status).setOnClickListener(this);
        findViewById(R.id.comment_iv_status).setOnClickListener(this);
        findViewById(R.id.mark_iv_status).setOnClickListener(this);
        markNumTv = (TextView) findViewById(R.id.markNum_tv_layout_story_toolbar);
        commentNumTv = (TextView) findViewById(R.id.commentNum_tv_layout_story_toolbar);
        //  头部控件
        imageTextLayout = (MyImageTextLayout) findViewById(R.id.container_header_activity_news_detail);
        //  初始化 ScrollView 及其滑动监听
        mScrollView = (ScrollView) findViewById(R.id.scrollView_activity_news_detail);
        mScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); //去掉滑动到底部的蓝色阴影
        mScrollView.getViewTreeObserver().addOnScrollChangedListener(this);
        //  初始化网页显示控件
        webView = (WebView) findViewById(R.id.webView_activity_news_detail);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.getSettings().setJavaScriptEnabled(true);// 设置支持JavaScript

        loadData(String.valueOf(newsID));
    }


    private void loadData(String newsID) {
        Log.i(TAG, "-----------loadData: newID=" + newsID);
        RetrofitHelper.getZhiHuAPI()
                .getNewsDetail(newsID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NewsDetailInfo>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "-----加载文章数据完成-----");
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.ShortToast("加载数据失败");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(NewsDetailInfo info) {
                        // TODO: update UI
                        showHtml(info);
                        bindHeaderViewData(info.getTitle(), info.getImage(), info.getImage_source());
                    }
                });

        RetrofitHelper.getZhiHuAPI()
                .getStoryExtra(newsID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<StoryExtraInfo>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "-------加载文章额外信息完成-------");
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.ShortToast("无法获取文章额外数据");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(StoryExtraInfo storyExtraInfo) {
                        markNumTv.setText(String.valueOf(storyExtraInfo.getPopularity()));
                        commentNumTv.setText(String.valueOf(storyExtraInfo.getComments()));
                    }
                });
    }

    /**
     *  绑定头部展示的图片数据
     * @param title
     * @param imageUrl
     * @param imageSource
     */
    private void bindHeaderViewData(String title, String imageUrl, String imageSource) {
        Log.i(TAG, "----------------bindHeaderViewData is called");
        imageTextLayout.setTitle(title);
        Glide.with(ZhiHuDailyApp.getInstance())
                .load(imageUrl)
                .centerCrop()
                .into(imageTextLayout.getImageView());
        imageTextLayout.setImageSourceInfo(imageSource);
    }


    /**
     *  显示html
     * @param newsDetailInfo
     */
    private void showHtml(NewsDetailInfo newsDetailInfo) {
        Log.i(TAG, "----------------showHtml is called");
        String data = WebUtils.buildHtmlWithCss(newsDetailInfo.getBody(), newsDetailInfo.getCss(), false);
        webView.loadDataWithBaseURL(WebUtils.BASE_URL, data, WebUtils.MIME_TYPE, WebUtils.ENCODING, WebUtils.FAIL_URL);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.share_iv_status:
                ToastUtil.ShortToast("点击了分享");
                break;
            case R.id.collect_iv_status:

                break;
            case R.id.comment_iv_status:
                Intent it = new Intent(NewsDetailActivity.this, CommentActivity.class);
                it.putExtra(IntentKeys.NEWS_ID, newsID);
                startActivity(it);
                break;
            case R.id.mark_iv_status:

                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollChanged() {

        //  改变 HeaderView 的位置
        int scrollY = mScrollView.getScrollY();
        int headerScrollY = (scrollY > 0) ? (scrollY / 2) : 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            imageTextLayout.setScrollY(headerScrollY);
            imageTextLayout.requestLayout();//view 的位置改变,请求重新绘制
        }

        //TODO: 改变 Toolbar 的透明度
        int storyHeaderViewHeight = getResources().getDimensionPixelSize(R.dimen.view_header_story_height);
        float toolbarHeight = toolbar.getHeight();
        float contentHeight = storyHeaderViewHeight - toolbarHeight;

        float ratio = Math.min(scrollY / contentHeight, 1.0f);
        toolbar.setAlpha(1-ratio);
        Log.i(TAG, "onScrollChanged: ratio="+ratio);
        if (scrollY <= contentHeight) {

            toolbar.setY(statusHeight);
            return;
        }

        boolean isPullingDown = mScrollPullDownHelper.onScrollChange(scrollY);
        float toolBarPositionY = isPullingDown ? statusHeight : (contentHeight - scrollY);
        toolbar.setY(toolBarPositionY);
        toolbar.setAlpha(1f);
    }


}
