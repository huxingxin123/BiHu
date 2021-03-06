package com.jay.bihu.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.jay.bihu.R;
import com.jay.bihu.adapter.FavoriteListRvAdapter;
import com.jay.bihu.config.ApiConfig;
import com.jay.bihu.data.User;
import com.jay.bihu.utils.HttpUtils;
import com.jay.bihu.utils.JsonParser;
import com.jay.bihu.utils.ToastUtils;

public class FavoriteListActivity extends BaseActivity {
    private User mUser;

    private RecyclerView mFavoriteRv;
    private SwipeRefreshLayout mRefreshLayout;
    private FavoriteListRvAdapter mFavoriteListRvAdapter;
    private boolean mLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        mUser = getIntent().getParcelableExtra("user");
        mFavoriteRv = (RecyclerView) findViewById(R.id.favoriteRv);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        setUpToolBar();
        setUpQuestionRv();
        setUpRefreshLayout();
    }

    private void setUpToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        }
    }

    private void setUpQuestionRv() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mFavoriteRv.setLayoutManager(layoutManager);
        mFavoriteListRvAdapter = new FavoriteListRvAdapter(mUser);
        mFavoriteRv.setAdapter(mFavoriteListRvAdapter);
    }

    private void setUpRefreshLayout() {
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mLoading)
                    return;
                mLoading = true;
                HttpUtils.sendHttpRequest(ApiConfig.FAVORITE_LIST, "token=" + mUser.getToken() + "&page=0", new HttpUtils.Callback() {
                    @Override
                    public void onResponse(HttpUtils.Response response) {
                        mLoading = false;
                        mRefreshLayout.setRefreshing(false);
                        if (response.isSuccess())
                            mFavoriteListRvAdapter.refreshFavoriteList(JsonParser.getQuestionList(response.bodyString()));
                        else ToastUtils.showError(response.message());
                    }

                    @Override
                    public void onFail(Exception e) {
                        mLoading = false;
                        ToastUtils.showError(e.toString());
                        mRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return true;
    }
}
