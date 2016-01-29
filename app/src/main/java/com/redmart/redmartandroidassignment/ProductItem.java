package com.redmart.redmartandroidassignment;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by archie on 28/1/16.
 */
public class ProductItem extends RealmObject implements RedMartService.ImageSetter {

    @PrimaryKey
    private long productId;
    private String productTitle;
    private String productMeasure;
    private Double normalPrice;
    private Double promoPrice;
    private byte[] image;
    private String error;


    public void setProductId(long id) {
        this.productId = id;
    }
    public long getProductId() {
        return productId;
    }

    public void setProductTitle(String title) {
        this.productTitle = title;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductMeasure(String measure) {
        this.productMeasure = measure;
    }

    public String getProductMeasure() {
        return productMeasure;
    }

    public void setNormalPrice(Double price) {
        this.normalPrice = price;
    }

    public Double getNormalPrice() {
        return normalPrice;
    }

    public void setPromoPrice(Double price) {
        this.promoPrice = price;
    }

    public Double getPromoPrice() {
        return promoPrice;
    }

    public byte[] getImage() { return image; }
    public String getError() {return error;}


    @Override
    public void setImage(byte[] image) {
        this.image = image;
    }

    @Override
    public void setError(String message) {
        this.error = message;
    }
}