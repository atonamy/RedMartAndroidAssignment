package com.redmart.redmartandroidassignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by archie on 27/1/16.
 */
public class RedMartCatalogRecyclerViewAdapter extends RecyclerView.Adapter<RedMartCatalogRecyclerViewAdapter.CatalogViewHolder> {

    private static final int MAX_BUFFER = 300;

    private Context currentContext;
    List<RedMartService.ProductItem> productList;
    private Integer wheelIndex;
    private int progressAdded;
    private final int updatedRange;

    public RedMartCatalogRecyclerViewAdapter(Context context, RedMartService.ProductItem[] product_list) {
        currentContext = context;
        wheelIndex = null;
        productList = new ArrayList<>();
        progressAdded = 0;
        populateCatalog(product_list);
        updatedRange = currentContext.getResources().getInteger(R.integer.page_size)/2;
    }

    public void populateCatalog(RedMartService.ProductItem[] product_list) {
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

        RedMartService.ProductItem product = getItem(i);
        populateProductLayout(product, viewHolder.productImage1, viewHolder.productTitle1,
                viewHolder.productMeasure1, viewHolder.productPrice1, viewHolder.productPromoPrice1);
        ProductItemClick product_click = new ProductItemClick(product.getProductId());
        viewHolder.productItem1.setOnClickListener(product_click);

        if(i < last_index)
        {
            product = getItem(i+1);
            populateProductLayout(product, viewHolder.productImage2, viewHolder.productTitle2,
                    viewHolder.productMeasure2, viewHolder.productPrice2, viewHolder.productPromoPrice2);
            product_click = new ProductItemClick(product.getProductId());
            viewHolder.productItem2.setOnClickListener(product_click);
        }

    }

    protected void populateProductLayout(RedMartService.ProductItem product,
                                         ImageView product_image, TextView product_title, TextView product_measure,
                                         TextView product_price, TextView product_promo_price) {
        DecimalFormat currency = new DecimalFormat("0.00");
        product_image.setImageDrawable(ResourcesService.getDrawable(product_image.getContext(), R.mipmap.ic_launcher));
        if(product.getImage() != null)
            product_image.setImageBitmap(product.getImage());
        product_title.setText(adjustTitle(product.productTitle));
        product_measure.setText(product.productMeasure);
        product_price.setText(currentContext.getResources().getString(R.string.catalog_currency_prefix)+currency.format(product.normalPrice));
        product_promo_price.setText("");
        product_price.setPaintFlags(product_price.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        if(product.promoPrice > 0 && product.promoPrice < product.normalPrice){
            product_promo_price.setText(currentContext.getResources().getString(R.string.catalog_currency_prefix)+currency.format(product.promoPrice));
            product_price.setPaintFlags(product_price.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }


    public RedMartService.ProductItem getItem(int position) {
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

    protected void Add(RedMartService.ProductItem[] products) {
        List<RedMartService.ProductItem> temp = Arrays.asList(products);
        //Collections.shuffle(temp); //just for fun
        productList.addAll(temp);
    }

    public void addLoader(boolean update) {
        progressAdded = 1;
        if(update)
            notifyItemRangeChanged(getItemCount()-1, getItemCount());
    }

    public void addProducts(RedMartService.ProductItem[] products) {
        populateCatalog(products);
        notifyItemRangeChanged(getItemCount()-updatedRange, getItemCount());
    }


    public void removeLoader(boolean update) {
        progressAdded = 0;
        if(update)
            notifyItemRangeChanged(getItemCount()-1, getItemCount());
    }

    public void checkIfNeedToClear() {
        if(productList.size() > MAX_BUFFER) {
            int last_position = (MAX_BUFFER - MAX_BUFFER / 3);
            productList.subList(0, last_position).clear();
            notifyItemRangeRemoved(0, (last_position/2));
        }
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
