package com.redmart.redmartandroidassignment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CatalogActivity extends AppCompatActivity implements RedMartService.ProductListResult {

    private static int PAGIGNATION_PREFIX = 6;

    @Bind(R.id.progress_wheel) ProgressWheel progressWheel;
    @Bind(R.id.activity_main_swipe_refresh_catalog) SwipeRefreshLayout swipeRefreshCatalog;
    @Bind(R.id.activity_main_catalog) RecyclerView mainCatalog;
    @Bind(R.id.toolbar) Toolbar mainToolbar;

    private RedMartService redMartService;
    private boolean catalogLoading;
    private RedMartCatalogRecyclerViewAdapter catalogAdapter;
    private int currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);
        ButterKnife.bind(this);
        mainCatalog.setLayoutManager(new LinearLayoutManager(this,  LinearLayoutManager.VERTICAL, false)); // require instantiate here otherwise application will crash (android bug due to android:scrollbars="vertical")
        VolleyManager.init(getApplicationContext());
        setSupportActionBar(mainToolbar);
        initUI();
        initDefaults();
    }


    protected void initDefaults() {
        catalogLoading = true;
        redMartService = new RedMartService(getApplicationContext());
        catalogAdapter = null;
        currentPage = 0;
    }

    @Override
    protected void onPostCreate (Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        progressWheel.setVisibility(View.VISIBLE);
        if(RedMartService.isOnline(this))
            loadCatalog(currentPage);
        else {
            progressWheel.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getResources().getString(R.string.no_connection),
                    Toast.LENGTH_LONG).show();
        }

    }

    protected void initUI() {

        progressWheel.setCircleRadius(progressWheel.getLayoutParams().width);
        progressWheel.setVisibility(View.INVISIBLE);
        mainCatalog.setVerticalScrollBarEnabled(true);
        mainCatalog.setHasFixedSize(false);

        swipeRefreshCatalog.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorPrimary);
        swipeRefreshCatalog.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                catalogAdapter = null;
                currentPage = 0;
                if(RedMartService.isOnline(CatalogActivity.this))
                    loadCatalog(currentPage);
                else {
                    swipeRefreshCatalog.setRefreshing(false);
                    Toast.makeText(CatalogActivity.this, CatalogActivity.this.getResources().getString(R.string.no_connection),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        mainCatalog.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {
                    int visible_item_count = mainCatalog.getLayoutManager().getChildCount();
                    int total_item_count = mainCatalog.getLayoutManager().getItemCount();
                    int past_visibles_item1s = ((LinearLayoutManager)mainCatalog.getLayoutManager()).findFirstVisibleItemPosition();

                    if (catalogLoading)
                    {
                        if ( (visible_item_count + past_visibles_item1s+PAGIGNATION_PREFIX) >= total_item_count)
                        {
                            catalogLoading = false;
                            if(RedMartService.isOnline(CatalogActivity.this)) {
                                catalogAdapter.addLoader(true);
                                loadCatalog(currentPage++);
                            }
                            else
                                Toast.makeText(CatalogActivity.this, CatalogActivity.this.getResources().getString(R.string.no_connection),
                                        Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }

    protected void loadCatalog(int page) {
        redMartService.retrieveProductList(page, getResources().getInteger(R.integer.page_size), CatalogActivity.this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        redMartService.close();
    }

    @Override
    public void onProductListResponse(RedMartService.ProductItem[] product_list) {
        progressWheel.setVisibility(View.INVISIBLE);
        if(product_list.length == getResources().getInteger(R.integer.page_size))
            catalogLoading = true;
        if(catalogAdapter != null)
            catalogAdapter.removeLoader(false);

        swipeRefreshCatalog.setRefreshing(false);
        if(catalogAdapter == null) {
            catalogAdapter = new RedMartCatalogRecyclerViewAdapter(this, product_list);
            mainCatalog.setAdapter(catalogAdapter);
        }else {
            catalogAdapter.addProducts(product_list);
        }
        if(currentPage == 0)
            startIntroAnimation();
    }

    @Override
    public void onErrorResponse(String error_message) {
        progressWheel.setVisibility(View.INVISIBLE);
        catalogLoading = true;
        if(catalogAdapter != null)
            catalogAdapter.removeLoader(true);
        swipeRefreshCatalog.setRefreshing(false);
        Toast.makeText(this, error_message,
                Toast.LENGTH_LONG).show();
    }

    private void startIntroAnimation() {
        mainCatalog.setTranslationY(mainCatalog.getHeight());
        mainCatalog.setAlpha(0f);
        mainCatalog.animate()
                .translationY(0)
                .setDuration(500)
                .alpha(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

}
