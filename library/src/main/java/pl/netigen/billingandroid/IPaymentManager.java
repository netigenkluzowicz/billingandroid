package pl.netigen.billingandroid;

import android.app.Activity;

public interface IPaymentManager {
    void onDestroy();

    void initiatePurchase(String sku, PurchaseListener purchaseListener, Activity activity);

    void isItemPurchased(String itemSku, PurchaseListener purchaseListener);

    void consumeAsync(final String purchaseToken);
}
