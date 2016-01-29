package com.redmart.redmartandroidassignment;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import io.realm.annotations.PrimaryKey;

/**
 * Created by archie on 26/1/16.
 */
public class RedMartService {

    private final static String API_PREFIX = "https://api.redmart.com/v1.5.6";
    private final static String IMAGE_PREFIX = "http://media.redmart.com/newmedia/200p";
    private final static String PRODUCT_LIST_API = API_PREFIX + "/catalog/search";
    private final static String PRODUCT_DETAILS_API = API_PREFIX + "/catalog/products";

    private Context currentContext;
    private ProductListResult productListResult;
    private ProductDetailsResult productDetailsResult;
    private TaskExecutor mainExecutor;
    private boolean stopped;


    public interface ImageSetter {
        public void setImage(Bitmap image);
        public void setError(String message);
    }

    public interface Result {
        public void onResult();
        public void onTimeout();
    }


    public interface ProductListResult {
        public void onProductListResponse(ProductItem[] product_list);
        public void onErrorResponse(String error_message);
    }

    public interface ProductDetailsResult {
        public void onProductDetailsResponse(ProductDetails product_details);
        public void onErrorResponse(String error_message);
    }

    public class ProductItem implements ImageSetter {

        @PrimaryKey
        public final long productId;
        public final String productTitle;
        public final String productMeasure;
        public final Double normalPrice;
        public final Double promoPrice;
        public final String imageUrl;
        private Bitmap productImage;
        private String errorMessage;


        public ProductItem(long product_id, String product_title, String product_measure,
                                Double normal_price, Double promo_price, String image_url) {
            this.productId = product_id;
            this.productTitle = product_title;
            this.productMeasure = product_measure;
            this.normalPrice = normal_price;
            this.promoPrice = promo_price;
            this.imageUrl = image_url;
            this.errorMessage = null;
            this.productImage = null;
        }
        public long getProductId() {
            return productId;
        }

        public Bitmap getImage() { return productImage; }
        public String getErrorMessage() {return errorMessage;}


        @Override
        public void setImage(Bitmap image) {
            this.productImage = image;
        }

        @Override
        public void setError(String message) {
            this.errorMessage = message;
        }
    }

    public class ProductDetails implements ImageSetter {

        public final ProductItem productInfo;
        public final String productDescription;
        private List<Bitmap> additionalImages;
        private String errorMessage;

        ProductDetails(ProductItem product, String product_description) {
            this.productDescription = product_description;
            this.additionalImages = new LinkedList<Bitmap>();
            this.productInfo = product;
        }

        @Override
        public void setImage(Bitmap image) {
            if(image != null)
                additionalImages.add(image);
        }

        @Override
        public void setError(String message) {
            errorMessage = message;
        }

        public String getErrorMessage() { return errorMessage; }
        public Bitmap[] getAdditionalProductImages() {return additionalImages.toArray(new Bitmap[additionalImages.size()]);}
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public RedMartService(Context context) {
        currentContext = context;
        initDefaults();
        initExecuror();
    }



    public void retrieveProductList(int page, int page_size, ProductListResult response) {
        productListResult = response;
        final String url = String.format(PRODUCT_LIST_API+"?page=%d&pageSize=%d",  page, page_size);
        ContentLoaderTask loaderTask = new ContentLoaderTask(url);
        if(!stopped)
            mainExecutor.execute(loaderTask);
    }

    public void retrieveProductDetails(long product_id, ProductDetailsResult response) {
        productDetailsResult = response;
        final String url = String.format(PRODUCT_DETAILS_API+"/%d", product_id);
        ContentLoaderTask loaderTask = new ContentLoaderTask(url);
        if(!stopped)
            mainExecutor.execute(loaderTask);
    }

    public void close() {
        stopped = true;
        if(mainExecutor != null)
            mainExecutor.shutdown();
    }

    protected void initDefaults() {
        mainExecutor = null;
        stopped = false;
        productListResult = null;
        productDetailsResult = null;
    }

    protected void initExecuror() {
        if(mainExecutor != null)
            mainExecutor.shutdown();

        mainExecutor = new TaskExecutor();
    }

    protected void handleProductList(JSONArray products) throws JSONException, InterruptedException {
        List<ProductItem> product_list = new LinkedList<ProductItem>();
        for(int i = 0; i < products.length() && !stopped; i++) {
            JSONObject product = products.getJSONObject(i);
            String product_image_url = IMAGE_PREFIX + product.getJSONObject("img").getString("name");
            ProductItem product_item = new ProductItem(product.getLong("id"), product.getString("title"),
                product.getJSONObject("measure").getString("wt_or_vol"), (Double)product.getJSONObject("pricing").getDouble("price"),
                    (Double)product.getJSONObject("pricing").getDouble("promo_price"), product_image_url);
            product_list.add(product_item);
            ImageLoaderTask image_loader = new ImageLoaderTask(product_image_url, product_item, Bitmap.CompressFormat.JPEG);
            if(!stopped)
                mainExecutor.execute(image_loader);
        }

        populateProductListResult(product_list);
    }

    protected void populateProductListResult(final List<ProductItem> result)
    {
        populateResult(new Result() {
            @Override
            public void onResult() {
                if(productListResult != null)
                    productListResult.onProductListResponse(result.toArray(new ProductItem[result.size()]));
            }

            @Override
            public void onTimeout() {
                if(productListResult != null)
                    productListResult.onErrorResponse(currentContext.getResources().getString(R.string.connection_timeout));
            }
        });
    }

    protected void handleProductDetails(JSONObject product) throws JSONException {
        String product_image_url = IMAGE_PREFIX + product.getJSONObject("img").getString("name");
        ProductItem product_item = new ProductItem(product.getLong("id"), product.getString("title"),
                product.getJSONObject("measure").getString("wt_or_vol"), (Double)product.getJSONObject("pricing").getDouble("price"),
                (Double)product.getJSONObject("pricing").getDouble("promo_price"), product_image_url);
        ProductDetails product_details = new ProductDetails(product_item, product.getString("desc"));
        JSONArray images = product.getJSONArray("images");
        for(int i = 0; i < images.length(); i++) {
            product_image_url = IMAGE_PREFIX + images.getJSONObject(i).getString("name");
            ImageLoaderTask image_loader = new ImageLoaderTask(product_image_url, product_details, Bitmap.CompressFormat.PNG);
            if (!stopped)
                mainExecutor.execute(image_loader);
            else
                break;
        }

        populateProductDetailsResult(product_details);
    }

    protected void populateProductDetailsResult(final ProductDetails result) {
        populateResult(new Result() {
            @Override
            public void onResult() {
                if(productDetailsResult != null)
                    productDetailsResult.onProductDetailsResponse(result);
            }

            @Override
            public void onTimeout() {
                if(productDetailsResult != null)
                    productDetailsResult.onErrorResponse(currentContext.getResources().getString(R.string.connection_timeout));
            }
        });
    }


    protected void populateResult(final Result result) {
        if(stopped)
            mainExecutor.shutdown();
        else if(result != null)
            mainExecutor.waitForFinished(new TaskExecutor.CompleteResult() {
                @Override
                public void onThreadPoolComplete() {
                    if((productListResult != null || productDetailsResult != null) && !stopped)
                        (new Handler()).post(new Runnable() {
                            @Override
                            public void run() {
                                result.onResult();
                            }
                        });
                }

                @Override
                public void onThreadPoolTimeout() {
                    if((productListResult != null || productDetailsResult != null) && !stopped)
                        (new Handler()).post(new Runnable() {
                            @Override
                            public void run() {
                                result.onTimeout();
                            }
                        });
                }
            });
    }



    private class ContentLoaderTask implements TaskExecutor.Task {

        private String jsonUrl;
        private Handler currentHandler;
        private boolean isComplete;
        private boolean stopped;

        public ContentLoaderTask(String json_url) {
            jsonUrl= json_url;
            currentHandler = new Handler();
            isComplete = false;
            stopped = false;
        }

        @Override
        public void run() {
            JsonObjectRequest json_request =  new JsonObjectRequest(jsonUrl, null, successJsonResponse, errorResponse);
            if(!stopped)
                VolleyManager.getVolleyQueue().add(json_request);
        }

        private Response.Listener<JSONObject> successJsonResponse =  new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                isComplete = true;
                String error_message = null;
                try {
                    if(response.has("products") && !stopped)
                        handleProductList(response.getJSONArray("products"));
                    else if(response.has("product") && !stopped)
                        handleProductDetails(response.getJSONObject("product"));
                    else
                        error_message = currentContext.getResources().getString(R.string.error_api_unrecognized);

                } catch (final JSONException e) {
                    e.printStackTrace();
                    error_message = e.getMessage();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    error_message = e.getMessage();
                }

                final String message = error_message;
                if(message != null)
                    currentHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(productListResult != null && !stopped)
                                productListResult.onErrorResponse(message);
                            if(productDetailsResult != null && !stopped)
                                productDetailsResult.onErrorResponse(message);
                        }
                    });
            }
        };

        private Response.ErrorListener errorResponse =  new Response.ErrorListener() {

            @Override
            public void onErrorResponse(final VolleyError error) {
                isComplete = true;
                currentHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(productListResult != null && !stopped)
                            productListResult.onErrorResponse(error.getMessage());
                        if(productDetailsResult != null && !stopped)
                            productDetailsResult.onErrorResponse(error.getMessage());
                    }
                });
            }
        };

        @Override
        public boolean isDone() {
            return isComplete;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

    public static class ImageLoaderTask implements TaskExecutor.Task {

        private final static int IMAGE_MAX_PREVIEW_SIZE = 400;
        private final static int IMAGE_MAX_SIZE = 900;

        private ImageSetter imageSetter;
        private String imageUrl;
        private boolean isComplete;
        private boolean stopped;
        private Bitmap.CompressFormat imageQuality;

        public ImageLoaderTask(String image_url, ImageSetter image_setter, Bitmap.CompressFormat quality) {
            this.imageSetter = image_setter;
            this.imageUrl = image_url;
            this.isComplete = false;
            this.stopped = false;
            this.imageQuality = quality;
        }

        @Override
        public void run() {
            if(imageSetter != null && imageUrl != null)
            {
                if(!imageUrl.isEmpty()) {
                    ImageRequest image_request = null;
                    if(this.imageQuality ==  Bitmap.CompressFormat.JPEG)
                        image_request = new ImageRequest(imageUrl, successImageResponse, IMAGE_MAX_PREVIEW_SIZE, IMAGE_MAX_PREVIEW_SIZE,
                                ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, errorResponse);
                    else if(this.imageQuality == Bitmap.CompressFormat.PNG)
                        image_request = new ImageRequest(imageUrl, successImageResponse, IMAGE_MAX_SIZE, IMAGE_MAX_SIZE,
                                ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, errorResponse);
                    if(!stopped && image_request != null)
                        VolleyManager.getVolleyQueue().add(image_request);
                    else
                        isComplete = true;
                }
            }
        }


        private Response.ErrorListener errorResponse =  new Response.ErrorListener() {

            @Override
            public void onErrorResponse(final VolleyError error) {
                isComplete = true;
                if(imageSetter != null && !stopped)
                    imageSetter.setError(error.getMessage());
            }
        };

        private Response.Listener<Bitmap> successImageResponse =  new Response.Listener<Bitmap>() {

            @Override
            public void onResponse(Bitmap image) {
                isComplete = true;
                if(imageSetter != null && !stopped)
                    imageSetter.setImage(image);
            }
        };

        @Override
        public boolean isDone() {
            return isComplete;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

}
