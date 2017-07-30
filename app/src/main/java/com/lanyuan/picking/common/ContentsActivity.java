package com.lanyuan.picking.common;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.lanyuan.picking.R;
import com.lanyuan.picking.pattern.BasePattern;
import com.lanyuan.picking.util.ScreenUtil;
import com.lanyuan.picking.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ContentsActivity extends BaseActivity {

    @BindView(R.id.swipe_target)
    RecyclerView recyclerView;
    @BindView(R.id.menu_recycle_view)
    RecyclerView menuRecycleView;
    @BindView(R.id.swipe_layout)
    SwipeToLoadLayout refreshLayout;

    private String baseUrl;
    private String currentUrl;

    private boolean isRunnable = true;
    private boolean hasMore = true;

    private ContentsAdapter adapter;

    private List<Menu> menuList;

    private BasePattern pattern;

    public enum parameter {
        RESULT, CURRENT_URL
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        pattern = (BasePattern) intent.getSerializableExtra("pattern");

        menuList = getMenuList() == null ? new ArrayList<Menu>() : getMenuList();
        MenuAdapter menuAdapter = new MenuAdapter(this, menuList);
        menuAdapter.setOnClickListener(new MenuAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(View view, int position) {
                ToastUtil.toast(menuList.get(position).getUrl());
                adapter.removeAll();
                baseUrl = getBaseUrl(menuList, position);
                currentUrl = menuList.get(position).getUrl();
                new GetContent().execute(currentUrl);
            }

            @Override
            public void ItemLongClickListener(View view, int position) {

            }
        });
        menuRecycleView.setLayoutManager(new LinearLayoutManager(this));
        menuRecycleView.setAdapter(menuAdapter);

        baseUrl = getBaseUrl(menuList, 0);
        currentUrl = menuList.get(0).getUrl();

        adapter = new ContentsAdapter(this, new ArrayList<AlbumInfo>(), ScreenUtil.getScreenWidth(this) / 2);
        adapter.setOnClickListener(new ContentsAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(View view, int position, AlbumInfo albumInfo) {
                Intent intent = new Intent(ContentsActivity.this, DetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("currentUrl", albumInfo.getAlbumUrl());
                bundle.putString("baseUrl", baseUrl);
                bundle.putSerializable("pattern", pattern);
                intent.putExtras(bundle);
                startActivity(intent);
            }

            @Override
            public void ItemLongClickListener(View view, int position) {

            }
        });

        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                toast("refresh");
                refreshLayout.setRefreshing(false);
            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                new GetNextPage().execute(currentUrl);
            }
        });

        new GetContent().execute(currentUrl);
    }

    public String getBaseUrl(List<Menu> menuList, int position) {
        return pattern.getBaseUrl(menuList, position);
    }

    public Class getTargetDetailActivity() {
        return null;
    }

    public List<Menu> getMenuList() {
        return pattern.getMenuList();
    }

    public Map<parameter, Object> getContent(String baseUrl, String currentUrl) {
        return pattern.getContent(baseUrl, currentUrl);
    }

    public String getNext(String baseUrl, String currentUrl) {
        return pattern.getNext(baseUrl, currentUrl);
    }

    private class GetContent extends AsyncTask<String, Integer, Map<parameter, Object>> {

        @Override
        protected Map<parameter, Object> doInBackground(String... strings) {
            if (strings.length > 0)
                return getContent(baseUrl, strings[0]);
            else
                return null;
        }

        @Override
        protected void onPostExecute(Map<parameter, Object> resultMap) {
            if (!isRunnable) return;

            currentUrl = (String) resultMap.get(parameter.CURRENT_URL);

            List<AlbumInfo> urls = (List<AlbumInfo>) resultMap.get(parameter.RESULT);
            adapter.addMore(urls);
        }
    }

    private class GetNextPage extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length > 0)
                return getNext(baseUrl, strings[0]);
            else
                return "";
        }

        @Override
        protected void onPostExecute(String url) {
            if (!isRunnable) return;
            if (!"".equals(url)) {
                new GetContent().execute(url);
                refreshLayout.setLoadingMore(false);
            } else {
                hasMore = false;
                toast("下面已经没有更多了！");
                refreshLayout.setLoadingMore(false);
            }
        }
    }
}
