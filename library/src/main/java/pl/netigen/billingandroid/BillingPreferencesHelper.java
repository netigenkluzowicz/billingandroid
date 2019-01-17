package pl.netigen.billingandroid;

import android.content.Context;
import android.content.SharedPreferences;

class BillingPreferencesHelper {

    private SharedPreferences sharedPreferences;
    private static final String BILLING_PREFERNECES_NAME = "NETIGEN_BILLING_PREFERENCES";
    private static final String WAS_CHECKED = "_WAS_CHECKED";

    public static BillingPreferencesHelper billingPreferencesHelper;

    public static BillingPreferencesHelper getInstance(Context context){
        if(billingPreferencesHelper==null){
            billingPreferencesHelper = new BillingPreferencesHelper(context);
        }
        return billingPreferencesHelper;
    }

    public BillingPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(BILLING_PREFERNECES_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSkuBought(String sku) {
        return sharedPreferences.getBoolean(sku, false);
    }

    public boolean wasSkuChecked(String sku) {
        return sharedPreferences.getBoolean(sku + WAS_CHECKED, false);
    }

    public void setSkuBought(String sku, boolean isBought) {
        sharedPreferences.edit().putBoolean(sku, isBought).apply();
    }

    public void setSkuChecked(String sku, boolean wasChecked) {
        sharedPreferences.edit().putBoolean(sku + WAS_CHECKED, wasChecked).apply();
    }

}
