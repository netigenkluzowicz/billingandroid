package pl.netigen.billingandroid;



import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public interface PurchaseListener {
    void onItemBought(String sku);

    void onPaymentsError(String errorMsg);

    void onItemNotBought(String sku);

    void onPurchasedItemsLoaded(List<Purchase> purchases);
}
