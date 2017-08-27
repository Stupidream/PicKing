package com.lanyuan.picking.ui.contents;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.lanyuan.picking.R;
import com.lanyuan.picking.common.bean.AlbumInfo;
import com.lanyuan.picking.common.bean.PicInfo;
import com.lanyuan.picking.common.WebViewTask;
import com.lanyuan.picking.pattern.MultiPicturePattern;
import com.lanyuan.picking.pattern.Searchable;
import com.lanyuan.picking.pattern.SinglePicturePattern;
import com.lanyuan.picking.ui.BaseActivity;
import com.lanyuan.picking.ui.dialog.PicDialog;
import com.lanyuan.picking.ui.detail.DetailActivity;
import com.lanyuan.picking.ui.menu.Menu;
import com.lanyuan.picking.config.AppConfig;
import com.lanyuan.picking.pattern.BasePattern;
import com.lanyuan.picking.util.FrescoUtil;
import com.lanyuan.picking.util.OkHttpClientUtil;
import com.lanyuan.picking.util.SPUtils;
import com.lanyuan.picking.util.ScreenUtil;
import com.lanyuan.picking.util.SnackbarUtils;
import com.lanyuan.picking.util.StatusBarUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class ContentsActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener, NavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener {

    @BindView(R.id.swipe_target)
    RecyclerView recyclerView;
    @BindView(R.id.menu_view)
    NavigationView menuView;
    @BindView(R.id.swipe_layout)
    SwipeToLoadLayout refreshLayout;
    @BindView(R.id.content_drawer)
    DrawerLayout drawerLayout;
    @BindView(R.id.detail_title_image)
    SimpleDraweeView titleImage;
    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout toolbarLayout;
    @BindView(R.id.appbar_detail)
    AppBarLayout appBarLayout;
    @BindView(R.id.title_detaill)
    TextView title;
    @BindView(R.id.time_detail)
    TextView time;

    SearchView searchView;
    EditText searchText;

    private String baseUrl;
    private String currentUrl;
    private String firstUrl;

    private boolean isRunnable = true;
    private boolean hasMore = true;

    private ContentsAdapter adapter;

    private List<Menu> menuList;

    private PicDialog picDialog;

    private BasePattern pattern;

    private Snackbar picDialogSnackBar;

    private boolean isSinglePic = false;
    private boolean isSearchable = false;

    private CollapsingToolbarLayoutState appBarState;

    private boolean needRefresh = true;

    private enum CollapsingToolbarLayoutState {
        EXPANDED,
        COLLAPSED,
        MIDDLE
    }

    public enum parameter {
        RESULT, CURRENT_URL, GIF_THUMB
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        // MIUI状态栏字体颜色调整
        StatusBarUtil.MIUISetStatusBarLightMode(this, true);

        // 设置状态栏颜色透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        // 绑定控件
        ButterKnife.bind(this);

        // 初始化单图对话框和提示框
        picDialog = new PicDialog(this);
        picDialogSnackBar = SnackbarUtils.Indefinite(getWindow().getDecorView(), "正在加载，请稍候……").info().getSnackbar();

        // 获取传来的规则并初始化状态
        Intent intent = getIntent();
        pattern = (BasePattern) intent.getSerializableExtra("pattern");
        initPattern(pattern);

        FrescoUtil.setBlurFrescoController(titleImage, pattern.getCategoryCoverUrl(), 1, 1);
        titleImage.setBackgroundColor(pattern.getBackgroundColor());
        title.setText(pattern.getWebsiteName());
        time.setText(pattern.getBaseUrl(null, 0));
        appBarLayout.addOnOffsetChangedListener(this);

        // 加载分类侧边栏
        menuList = getMenuList() == null ? new ArrayList<Menu>() : getMenuList();
        menuView.setItemIconTintList(null);
        menuView.setNavigationItemSelectedListener(this);
        menuView.getMenu().setGroupCheckable(0, true, true);
        for (int i = 0; i < menuList.size(); i++) {
            menuView.getMenu().add(0, i, 0, menuList.get(i).getName());
        }
        CardView cardView = ButterKnife.findById(menuView.getHeaderView(0), R.id.search_card);
        if (isSearchable) {
            cardView.setVisibility(View.VISIBLE);
            searchView = ButterKnife.findById(cardView, R.id.search_tags);
            searchView.setOnQueryTextListener(this);
            searchText = ButterKnife.findById(searchView, android.support.v7.appcompat.R.id.search_src_text);
            searchText.setTextColor(Color.BLACK);
            ImageView searchButton = ButterKnife.findById(searchView, android.support.v7.appcompat.R.id.search_mag_icon);
            searchButton.setImageResource(R.mipmap.search);
            ImageView deleteButton = ButterKnife.findById(searchView, android.support.v7.appcompat.R.id.search_close_btn);
            deleteButton.setImageResource(R.mipmap.delete);
        }

        // 初始化访问地址
        baseUrl = getBaseUrl(menuList, 0);
        currentUrl = menuList.get(0).getUrl();
        firstUrl = currentUrl;

        // 是否滑动界面的时候不加载图片
        if (!(boolean) SPUtils.get(this, AppConfig.load_pic_swipe, false))
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState > 0)
                        Fresco.getImagePipeline().pause();
                    else
                        Fresco.getImagePipeline().resume();
                }
            });

        // 设置列表adapter，区分单图模式和多图模式，分别处理
        adapter = new ContentsAdapter(this, new ArrayList<AlbumInfo>(), ScreenUtil.getScreenWidth(this) / 2);
        adapter.setOnClickListener(new ContentsAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(View view, int position, AlbumInfo albumInfo) {
                if (isSinglePic) {
                    new GetSinglePicContent().execute(albumInfo.getAlbumUrl());
                    picDialogSnackBar.show();
                } else if (pattern instanceof MultiPicturePattern) {
                    Intent intent = new Intent(ContentsActivity.this, DetailActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("albumInfo", albumInfo);
                    bundle.putString("baseUrl", baseUrl);
                    bundle.putSerializable("pattern", pattern);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }

            @Override
            public void ItemLongClickListener(View view, int position, AlbumInfo albumInfo) {
            }
        });

        // 设置列表显示的列数
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setAdapter(adapter);

        // 是否自动加载更多
        if ((boolean) SPUtils.get(this, AppConfig.auto_load_more, false))
            recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (!ViewCompat.canScrollVertically(recyclerView, 1)) {
                            refreshLayout.setLoadingMore(true);
                        }
                    }
                }
            });

        // 设置下拉刷新和上拉加载
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.e("ContentsActivity", "onCreate: refresh");
                adapter.removeAll();
                new GetContent().execute(firstUrl);
            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                new GetContentNext().execute(currentUrl);
            }
        });
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });
    }

    private void initPattern(BasePattern pattern) {
        if (pattern instanceof SinglePicturePattern)
            isSinglePic = true;
        else if (pattern instanceof MultiPicturePattern)
            isSinglePic = false;
        if (pattern instanceof Searchable)
            isSearchable = true;
        else
            Log.e("ContentsActivity", "initPattern: false");
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        if (verticalOffset == 0) {
            if (appBarState != CollapsingToolbarLayoutState.EXPANDED) {
                Log.e("ContentsActivity", "onOffsetChanged: EXPANDED");
                refreshLayout.setRefreshEnabled(true);
                refreshLayout.setLoadMoreEnabled(false);
                if (needRefresh) {
                    refreshLayout.setRefreshing(true);
                    needRefresh = false;
                }
                appBarState = CollapsingToolbarLayoutState.EXPANDED; // 展开
            }
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            Log.e("ContentsActivity", "onOffsetChanged: COLLAPSED");
            refreshLayout.setLoadMoreEnabled(true);
            refreshLayout.setRefreshEnabled(false);
            appBarState = CollapsingToolbarLayoutState.COLLAPSED; // 折叠
        } else {
            if (appBarState != CollapsingToolbarLayoutState.MIDDLE) {
                if (appBarState == CollapsingToolbarLayoutState.COLLAPSED) {
                    Log.e("ContentsActivity", "onOffsetChanged: MIDDLE + COLLAPSED");
                }
                Log.e("ContentsActivity", "onOffsetChanged: MIDDLE");
                appBarState = CollapsingToolbarLayoutState.MIDDLE; // 中间
            }
        }

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if ("".equals(query))
            SnackbarUtils.Short(getWindow().getDecorView(), "搜索内容不能为空").gravityFrameLayout(Gravity.TOP).danger().show();
        else {
            SnackbarUtils.Long(getWindow().getDecorView(), "正在搜索：" + query).gravityFrameLayout(Gravity.TOP).info().show();
            drawerLayout.closeDrawer(GravityCompat.END);
            adapter.removeAll();
            currentUrl = ((Searchable) pattern).getSearch(query);
            firstUrl = currentUrl;

            // refreshLayout.setRefreshing(true);
            if (appBarState != CollapsingToolbarLayoutState.EXPANDED) {
                needRefresh = true;
                appBarLayout.setExpanded(true);
            } else
                refreshLayout.setRefreshing(true);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int position = item.getItemId();
        adapter.removeAll();
        baseUrl = getBaseUrl(menuList, position);
        currentUrl = menuList.get(position).getUrl();
        firstUrl = currentUrl;

        // refreshLayout.setRefreshing(true);
        if (appBarState != CollapsingToolbarLayoutState.EXPANDED) {
            needRefresh = true;
            appBarLayout.setExpanded(true);
        } else
            refreshLayout.setRefreshing(true);

        drawerLayout.closeDrawer(GravityCompat.END);
        if (isSearchable)
            searchView.clearFocus();

        return false;
    }

    public String getBaseUrl(List<Menu> menuList, int position) {
        return pattern.getBaseUrl(menuList, position);
    }

    public List<Menu> getMenuList() {
        return pattern.getMenuList();
    }

    public Map<parameter, Object> getContent(String baseUrl, String currentUrl, byte[] result, Map<parameter, Object> resultMap) throws UnsupportedEncodingException {
        return pattern.getContent(baseUrl, currentUrl, result, resultMap);
    }

    public String getContentNext(String baseUrl, String currentUrl, byte[] result) throws UnsupportedEncodingException {
        return pattern.getContentNext(baseUrl, currentUrl, result);
    }

    public PicInfo getSinglePicContent(String baseUrl, String currentUrl, byte[] result) throws UnsupportedEncodingException {
        return ((SinglePicturePattern) pattern).getSinglePicContent(baseUrl, currentUrl, result);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    private class GetContent extends AsyncTask<String, Integer, Map<parameter, Object>> {

        @Override
        protected Map<parameter, Object> doInBackground(String... strings) {
            if (strings.length > 0) {
                try {
                    Map<parameter, Object> resultMap = new HashMap<>();
                    Request request = new Request.Builder()
                            .url(strings[0])
                            .build();
                    Call call = OkHttpClientUtil.getInstance().newCall(request);
                    Response response = call.execute();
                    byte[] result = response.body().bytes();
                    return getContent(baseUrl, strings[0], result, resultMap);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else
                return null;
        }

        @Override
        protected void onPostExecute(Map<parameter, Object> resultMap) {
            if (!isRunnable)
                return;
            else if (resultMap == null) {
                Snackbar.make(getWindow().getDecorView(), "获取内容失败，请检查网络连接", Snackbar.LENGTH_LONG).show();
                refreshLayout.setRefreshing(false);
                return;
            }

            currentUrl = (String) resultMap.get(parameter.CURRENT_URL);
            if (firstUrl == null) firstUrl = currentUrl;

            List<AlbumInfo> urls = (List<AlbumInfo>) resultMap.get(parameter.RESULT);
            if (urls.size() == 0) {
                Snackbar.make(getWindow().getDecorView(), "获取内容失败，请检查网络连接", Snackbar.LENGTH_LONG).show();
                refreshLayout.setRefreshing(false);
                return;
            }
            adapter.addMore(urls);
            refreshLayout.setRefreshing(false);
            refreshLayout.setLoadingMore(false);
        }
    }

    private class GetContentNext extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length > 0) {
                try {
                    Request request = new Request.Builder()
                            .url(strings[0])
                            .build();
                    Call call = OkHttpClientUtil.getInstance().newCall(request);
                    Response response = call.execute();
                    byte[] result = response.body().bytes();
                    return getContentNext(baseUrl, strings[0], result);
                } catch (IOException e) {
                    e.printStackTrace();
                    return "";
                }
            } else
                return "";
        }

        @Override
        protected void onPostExecute(String url) {
            if (!isRunnable) return;
            if (!"".equals(url)) {
                new GetContent().execute(url);
            } else {
                hasMore = false;
                SnackbarUtils.Short(getWindow().getDecorView(), "下面已经没有更多了！").danger().show();
                refreshLayout.setLoadingMore(false);
            }
        }
    }

    private class GetSinglePicContent extends AsyncTask<String, Integer, PicInfo> {

        @Override
        protected PicInfo doInBackground(String... strings) {
            if (strings.length > 0) {
                try {
                    Request request = new Request.Builder()
                            .url(strings[0])
                            .build();

                    // 这是针对煎蛋很粗糙的一个处理
                    if (strings[0].endsWith("jpg") || strings[0].endsWith("gif") || strings[0].endsWith("png"))
                        return getSinglePicContent(baseUrl, currentUrl, strings[0].getBytes());

                    Response response = OkHttpClientUtil.getInstance().newCall(request).execute();
                    byte[] result = response.body().bytes();
                    return getSinglePicContent(baseUrl, currentUrl, result);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else
                return null;
        }

        @Override
        protected void onPostExecute(PicInfo picInfo) {
            if (!isRunnable)
                return;
            else if (picInfo == null || "".equals(picInfo.getPicUrl())) {
                SnackbarUtils.Long(getWindow().getDecorView(), "获取内容失败，请检查网络连接").danger().show();
                return;
            }

            picDialog.show(picInfo);
            picDialogSnackBar.dismiss();
        }
    }

    private class GetContentByWebView extends WebViewTask {

        public GetContentByWebView(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Map<parameter, Object> resultMap = new HashMap<>();
                resultMap = getContent(baseUrl, currentUrl, result.getBytes(), resultMap);

                if (!isRunnable) {
                    cancel();
                    return;
                } else if (resultMap == null) {
                    return;
                }

                currentUrl = (String) resultMap.get(parameter.CURRENT_URL);
                if (firstUrl == null) firstUrl = currentUrl;

                Log.e("GetContentByWebView", "onPostExecute: " + currentUrl);

                List<AlbumInfo> urls = (List<AlbumInfo>) resultMap.get(parameter.RESULT);
                if (urls.size() == 0) {
                    return;
                }

                Log.e("GetContentByWebView", "onPostExecute: " + urls.size());

                adapter.addMore(urls);
                refreshLayout.setRefreshing(false);
                refreshLayout.setLoadingMore(false);
                cancel();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onFailureExecute() {
            Snackbar.make(getWindow().getDecorView(), "获取内容失败，请检查网络连接", Snackbar.LENGTH_LONG).show();
            refreshLayout.setRefreshing(false);
        }
    }

    private class getContentNextByWebView extends WebViewTask {

        public getContentNextByWebView(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                String url = getContentNext(baseUrl, currentUrl, result.getBytes());

                Log.e("getContentNextByWebView", "onPostExecute: " + url);

                if (!isRunnable) {
                    cancel();
                    return;
                }
                if (!"".equals(url)) {
                    new GetContentByWebView(ContentsActivity.this).execute(url);
                } else {
                    hasMore = false;
                    SnackbarUtils.Short(getWindow().getDecorView(), "下面已经没有更多了！").danger().show();
                    refreshLayout.setLoadingMore(false);
                    cancel();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onFailureExecute() {
            Snackbar.make(getWindow().getDecorView(), "获取内容失败，请检查网络连接", Snackbar.LENGTH_LONG).show();
            refreshLayout.setLoadingMore(false);
        }
    }
}
