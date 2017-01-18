package com.jay.bihu.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jay.bihu.R;
import com.jay.bihu.adapter.QuestionRvAdapter;
import com.jay.bihu.data.User;
import com.jay.bihu.config.ApiConfig;
import com.jay.bihu.utils.HttpUtils;
import com.jay.bihu.utils.JsonParser;
import com.jay.bihu.view.CircleImageView;
import com.jay.bihu.view.LoginDialog;

public class MainActivity extends BaseActivity {
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private RecyclerView mQuestionRv;
    private SwipeRefreshLayout mRefreshLayout;

    private User mUser;
    private QuestionRvAdapter mQuestionRvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUser = JsonParser.getUser(getIntent().getBundleExtra("data").getString("data"));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mNavigationView = (NavigationView) findViewById(R.id.navigationView);
        mQuestionRv = (RecyclerView) findViewById(R.id.questionRv);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        setUpToolBar();
        setUpNavigationView();
        setUpQuestionRv();
        setUpRefreshLayout();
    }

    private void setUpRefreshLayout() {
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                HttpUtils.sendHttpRequest(ApiConfig.QUESTION_LIST, "page=0&count=20", new HttpUtils.Callback() {
                    @Override
                    public void onResponse(HttpUtils.Response response) {
                        mRefreshLayout.setRefreshing(false);
                        if (response.isSuccess())
                            mQuestionRvAdapter.refreshQuestionList(JsonParser.getQuestionList(response.bodyString()));
                        else showMessage(response.message());
                    }

                    @Override
                    public void onFail(Exception e) {
                        showMessage(e.toString());
                        mRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void setUpQuestionRv() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mQuestionRv.setLayoutManager(layoutManager);

        HttpUtils.sendHttpRequest(ApiConfig.QUESTION_LIST, "page=0&count=20", new HttpUtils.Callback() {
            @Override
            public void onResponse(HttpUtils.Response response) {
                if (response.isSuccess()) {
                    mQuestionRvAdapter = new QuestionRvAdapter(mUser, JsonParser.getQuestionList(response.bodyString()), ApiConfig.QUESTION_LIST);
                    mQuestionRv.setAdapter(mQuestionRvAdapter);
                } else showMessage(response.message());
            }

            @Override
            public void onFail(Exception e) {
                showMessage(e.toString());
            }
        });
    }

    private void setUpNavigationView() {
        //menu
        mNavigationView.setCheckedItem(R.id.home);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        mQuestionRv.scrollToPosition(0);
                        break;
                    case R.id.question:
                        Bundle data = new Bundle();
                        data.putString("token", mUser.getToken());
                        activityStart(QuestionActivity.class, data);
                        break;
                    case R.id.favorite:
                        Bundle bundle = new Bundle();
                        bundle.putString("token", mUser.getToken());
                        activityStart(FavoriteActivity.class, bundle);
                        break;
                    case R.id.avatar:
                        upLoadAvatar();
                        break;
                    case R.id.changePassword:
                        changePassword();
                        break;
                    case R.id.logout:
                        logout();
                        break;
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        //header
        View view = mNavigationView.inflateHeaderView(R.layout.navigation_header);
        CircleImageView avatar = (CircleImageView) view.findViewById(R.id.avatar);
        TextView username = (TextView) view.findViewById(R.id.username);

        username.setText(mUser.getUsername());
        //获取头像
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upLoadAvatar();
            }
        });

    }

    private void upLoadAvatar() {

    }

    private void changePassword() {
        final LoginDialog dialog = new LoginDialog(this);
        dialog.show();
        dialog.getMessageTextView().setText("更改密码");
        dialog.addPasswordWrapper("新" + getString(R.string.hint_password));
        dialog.setLoginButton("确定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String password = dialog.getPasswordWrapper().getEditText().getText().toString();
                if (password.length() < 5) {
                    dialog.getPasswordWrapper().setError("密码过短");
                    return;
                }
                String param = "token=" + mUser.getToken() + "&password=" + password;
                HttpUtils.sendHttpRequest(ApiConfig.CHANGE_PASSWORD, param, new HttpUtils.Callback() {
                    @Override
                    public void onResponse(HttpUtils.Response response) {
                        if (response.isSuccess()) {
                            dialog.dismiss();
                            showMessage(response.getInfo(), Toast.LENGTH_SHORT);
                            SharedPreferences preferences = getSharedPreferences("account", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("password", password);
                            editor.apply();
                        } else showMessage(response.message());
                    }

                    @Override
                    public void onFail(Exception e) {
                        showMessage(e.toString());
                    }
                });
            }
        });
    }

    private void logout() {
        SharedPreferences preferences = getSharedPreferences("account", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLogin", false);
        editor.putString("password", "");
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setUpToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            mDrawerLayout.openDrawer(GravityCompat.START);
        return false;
    }
}
