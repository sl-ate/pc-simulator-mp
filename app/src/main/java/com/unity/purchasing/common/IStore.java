package com.unity.purchasing.common;

import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
public interface IStore {
    void FinishTransaction(ProductDefinition productDefinition, String str);

    void Purchase(ProductDefinition productDefinition);

    void Purchase(ProductDefinition productDefinition, String str);

    void RetrieveProducts(List<ProductDefinition> list);
}
