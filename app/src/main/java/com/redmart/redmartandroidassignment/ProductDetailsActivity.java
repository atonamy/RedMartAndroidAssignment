package com.redmart.redmartandroidassignment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.text.DecimalFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ProductDetailsActivity extends AppCompatActivity implements RedMartService.ProductDetailsResult {

    @Bind(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;
    @Bind(R.id.product_details_toolbar) Toolbar toolBar;
    @Bind(R.id.progress_wheel_details)
    ProgressWheel progressWheel;
    @Bind(R.id.product_details_image)
    ImageView productImage;
    @Bind(R.id.product_details_description)
    TextView productDescription;
    @Bind(R.id.product_details_measure)
    TextView productMeasure;
    @Bind(R.id.product_details_price)
    TextView productPrice;
    @Bind(R.id.product_details_promo_price)
    TextView productPromoPrice;
    @Bind(R.id.detail_content)
    CoordinatorLayout detailContent;


    private RedMartService redMartService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        ButterKnife.bind(this);
        initDefaults();
        initUI();
    }

    @Override
    protected void onPostCreate (Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(RedMartService.isOnline(this)) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(detailContent.getVisibility() != View.VISIBLE)
                        progressWheel.setVisibility(View.VISIBLE);

                }
            }, 500);
            loadProductDetails();
        }
        else {
            Toast.makeText(this, getResources().getString(R.string.no_connection),
                    Toast.LENGTH_LONG).show();
            finish();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        redMartService.close();
    }

    protected void initDefaults() {
        redMartService = new RedMartService(getApplicationContext());
    }

    protected void initUI() {
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        progressWheel.setCircleRadius(progressWheel.getLayoutParams().width);
        progressWheel.setVisibility(View.GONE);

        final Drawable upArrow = ResourcesService.getDrawable(this, R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ResourcesService.getColor(getApplicationContext(), R.color.productItemPreviewTitle), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    protected void loadProductDetails() {
        long product_id = getIntent().getLongExtra("ProductId", -1);
        if(product_id == -1)
            finish();
        else
            redMartService.retrieveProductDetails(product_id, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;

        }

        return true;
    }

    @Override
    public void onProductDetailsResponse(RedMartService.ProductDetails product_details) {
        DecimalFormat currency = new DecimalFormat("0.00");
        progressWheel.setVisibility(View.GONE);
        collapsingToolbar.setTitle(product_details.productInfo.getProductTitle());
        productDescription.setText(product_details.productInfo.getProductTitle() + "\n" + product_details.productDescription.replaceAll("��"," "));
        productMeasure.setText(product_details.productInfo.getProductMeasure());
        productPrice.setText(currency.format(product_details.productInfo.getNormalPrice()) + " SGD");
        if(product_details.productInfo.getPromoPrice() > 0 && product_details.productInfo.getPromoPrice() < product_details.productInfo.getNormalPrice())
        {
            productPromoPrice.setText(currency.format(product_details.productInfo.getPromoPrice()) + " SGD");
            productPrice.setPaintFlags(productPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        detailContent.setVisibility(View.VISIBLE);
        SlideShow(product_details.getAdditionalProductImages(), 0);
    }

    @Override
    public void onErrorResponse(String error_message) {
        Toast.makeText(this, error_message,
                Toast.LENGTH_LONG).show();
        finish();
    }

    protected void SlideShow(final Bitmap[] images, final int index) {
        if(images.length == 0)
            return;
        if(index > images.length-1) {
            SlideShow(images, 0);
            return;
        }

        productImage.setImageBitmap(DrawTitleHighlight(images[index]));
        productImage.setAlpha(0.0f);
        productImage.animate().alpha(1.0f);

        if(images.length > 1)
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    SlideShow(images, index+1);
                }
            }, getResources().getInteger(R.integer.product_details_slideshow_delay_in_sec)*1000);
    }

    protected Bitmap DrawTitleHighlight(Bitmap image) {
        Bitmap result = image.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        int percent = canvas.getHeight()/100;
        int height = (canvas.getWidth() > canvas.getHeight()) ?
                percent*getResources().getInteger(R.integer.highlight_product_title_horizontal_proportion)
                : percent*getResources().getInteger(R.integer.highlight_product_title_vertical_proportion);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(getResources().getInteger(R.integer.highlight_product_title_alpha));
        canvas.drawRect(0, canvas.getHeight() - height, canvas.getWidth(), canvas.getHeight(), paint);
        return result;
    }
}
