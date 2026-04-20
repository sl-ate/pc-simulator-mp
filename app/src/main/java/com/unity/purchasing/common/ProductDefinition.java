package com.unity.purchasing.common;

/* JADX INFO: loaded from: classes2.dex */
public class ProductDefinition {

    /* JADX INFO: renamed from: id */
    public String f150id;
    public String storeSpecificId;
    public ProductType type;

    public ProductDefinition(String str, String str2, ProductType productType) {
        this.f150id = str;
        this.storeSpecificId = str2;
        this.type = productType;
    }

    public ProductDefinition(String str, ProductType productType) {
        this(str, str, productType);
    }
}
