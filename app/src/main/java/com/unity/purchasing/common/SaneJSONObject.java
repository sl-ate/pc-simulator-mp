package com.unity.purchasing.common;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public class SaneJSONObject extends JSONObject {
    @Override // org.json.JSONObject
    public JSONObject put(String str, double d) {
        try {
            return super.put(str, d);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // org.json.JSONObject
    public JSONObject put(String str, Object obj) {
        try {
            return super.put(str, obj);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // org.json.JSONObject
    public JSONObject put(String str, boolean z) {
        try {
            return super.put(str, z);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
