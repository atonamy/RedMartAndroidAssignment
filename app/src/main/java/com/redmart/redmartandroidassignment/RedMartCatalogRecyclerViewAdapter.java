package com.redmart.redmartandroidassignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.realm.RealmList;

/**
 * Created by archie on 27/1/16.
 */
public class RedMartCatalogRecyclerViewAdapter extends RecyclerView.Adapter<RedMartCatalogRecyclerViewAdapter.CatalogViewHolder> {

    private Context currentContext;
    List<ProductItem> productList;
    private Integer wheelIndex;
    private int progressAdded;
    private LinearLayoutManager layoutManager;
    private final int updatedRange;

    public RedMartCatalogRecyclerViewAdapter(Context context, ProductItem[] product_list, LinearLayoutManager layout_manager) {
        currentContext = context;
        wheelIndex = null;
        productList = new RealmList<ProductItem>();
        progressAdded = 0;
        populateCatalog(product_list);
        layoutManager = layout_manager;
        updatedRange = currentContext.getResources().getInteger(R.integer.page_size)/2;
    }

    public void populateCatalog(ProductItem[] product_list) {
        if(product_list != null)
            Add(product_list);
    }

    public class CatalogViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.product_item1_title)
        TextView productTitle1;
        @Bind(R.id.product_item1_image)
        ImageView productImage1;
        @Bind(R.id.product_item2_title)
        TextView productTitle2;
        @Bind(R.id.product_item2_image)
        ImageView productImage2;
        @Bind(R.id.product_item1_measure)
        TextView productMeasure1;
        @Bind(R.id.product_item2_measure)
        TextView productMeasure2;
        @Bind(R.id.product_item1_price_new)
        TextView productPrice1;
        @Bind(R.id.product_item1_promo_price_new)
        TextView productPromoPrice1;
        @Bind(R.id.product_item2_price_new)
        TextView productPrice2;
        @Bind(R.id.product_item2_promo_price_new)
        TextView productPromoPrice2;
        @Bind(R.id.catalog_row)
        LinearLayout productListRow;
        @Bind(R.id.progress_wheel_small)
        ProgressWheel progressWheel;
        @Bind(R.id.product_item1)
        CardView productItem1;
        @Bind(R.id.product_item2)
        CardView productItem2;

        public CatalogViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }

    }


    @Override
    public CatalogViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View inflatedView = LayoutInflater.from(currentContext).inflate(R.layout.product_item, viewGroup, false);
        return new CatalogViewHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(CatalogViewHolder viewHolder, int i) {
        int last_index = productList.size()-1;
        int position = i;
        i = getNext(i);

        if(i > last_index)
        {
            viewHolder.productListRow.setVisibility(View.GONE);
            viewHolder.progressWheel.setVisibility(View.VISIBLE);
            return;
        }
        else
        {
            viewHolder.productListRow.setVisibility(View.VISIBLE);
            viewHolder.progressWheel.setVisibility(View.GONE);
        }

        ProductItem product = getItem(i);
        populateProductLayout(position, product, viewHolder.productImage1, viewHolder.productTitle1,
                viewHolder.productMeasure1, viewHolder.productPrice1, viewHolder.productPromoPrice1);
        ProductItemClick product_click = new ProductItemClick(product.getProductId());
        viewHolder.productItem1.setOnClickListener(product_click);

        if(i < last_index)
        {
            product = getItem(i+1);
            populateProductLayout(position, product, viewHolder.productImage2, viewHolder.productTitle2,
                    viewHolder.productMeasure2, viewHolder.productPrice2, viewHolder.productPromoPrice2);
            product_click = new ProductItemClick(product.getProductId());
            viewHolder.productItem2.setOnClickListener(product_click);
        }
        else
            viewHolder.productItem2.setVisibility(View.INVISIBLE);

    }

    protected void populateProductLayout(int position, ProductItem product,
                                         ImageView product_image, TextView product_title, TextView product_measure,
                                         TextView product_price, TextView product_promo_price) {
        DecimalFormat currency = new DecimalFormat("0.00");
        //if(product.getImage() != null)
        //    product_image.setImageBitmap(BitmapFactory.decodeByteArray(product.getImage(), 0, product.getImage().length));
        populateImage(position, product.getImagreUrl(), product_image);
        product_title.setText(adjustTitle(product.getProductTitle()));
        product_measure.setText(product.getProductMeasure());
        product_price.setText(currentContext.getResources().getString(R.string.catalog_currency_prefix)+currency.format(product.getNormalPrice()));
        product_promo_price.setText("");
        product_price.setPaintFlags(product_price.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        if(product.getPromoPrice() > 0 && product.getPromoPrice() < product.getNormalPrice()){
            product_promo_price.setText(currentContext.getResources().getString(R.string.catalog_currency_prefix)+currency.format(product.getPromoPrice()));
            product_price.setPaintFlags(product_price.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void populateImage(final int position, final String url, final ImageView product_image) {
        if(product_image.getTag(R.integer.product_image_task) != null) {
            ((RedMartService.ImageLoaderTask) product_image.getTag(R.integer.product_image_task)).stop();
            product_image.setTag(R.integer.product_image_task, null);
        }
        if(product_image.getTag(R.integer.product_image_thread) != null) {
            ((Thread) product_image.getTag(R.integer.product_image_thread)).interrupt();
            product_image.setTag(R.integer.product_image_thread, null);
        }

        product_image.setImageDrawable(null);
        RedMartService.ImageLoaderTask image_loader = new RedMartService.ImageLoaderTask(currentContext, url, new RedMartService.ImageSetter() {
            @Override
            public void setImage(byte[] image) {
                if (layoutManager != null && position >= layoutManager.findFirstVisibleItemPosition() && position <= layoutManager.findLastVisibleItemPosition()) {
                    product_image.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
                    product_image.setAlpha(0.0f);
                    product_image.animate().alpha(1.0f);
                }
            }

            @Override
            public void setError(String message) {
                product_image.setImageDrawable(ResourcesService.getDrawable(product_image.getContext(), R.mipmap.ic_launcher));
                product_image.setAlpha(0.0f);
                product_image.animate().alpha(1.0f);
            }
        }, Bitmap.CompressFormat.JPEG);

        product_image.setTag(R.integer.product_image_task, image_loader);
        Thread thread = new Thread(image_loader);
        product_image.setTag(R.integer.product_image_thread, thread);
        thread.start();
    }


    public ProductItem getItem(int position) {
        return productList.get(position);

    }

    public int getNext(int position) {
        if(position > 0)
            position *= 2;
        return position;

    }

    protected String adjustTitle(String title) {
        if(title.length() > 45)
            return title.substring(0, 42) + "...";
        return title;
    }

    @Override
    public int getItemCount() {
        int size = productList.size()/2;
        return (((productList.size()%2) == 0) ? size : size+1)+progressAdded;
    }

    protected void Add(ProductItem[] products) {
        List<ProductItem> temp = Arrays.asList(products);
        Collections.shuffle(temp); //just for fun
        productList.addAll(temp);
    }

    public void addLoader(boolean update) {
        progressAdded = 1;
        if(update)
            notifyItemRangeChanged(getItemCount()-1, getItemCount());
    }

    public void addProducts(ProductItem[] products) {
        populateCatalog(products);
        notifyItemRangeChanged(getItemCount()-updatedRange, getItemCount());
    }


    public void removeLoader(boolean update) {
        progressAdded = 0;
        if(update)
            notifyItemRangeChanged(getItemCount()-1, getItemCount());
    }


    private class ProductItemClick implements View.OnClickListener {

        private final long productId;

        public ProductItemClick(long product_id) {
            this.productId = product_id;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(currentContext, ProductDetailsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("ProductId", productId);
            currentContext.startActivity(intent);
        }
    }


}
