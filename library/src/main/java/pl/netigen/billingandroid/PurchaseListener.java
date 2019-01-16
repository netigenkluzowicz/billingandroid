package pl.netigen.billingandroid;

import android.support.annotation.Nullable;

import com.android.billingclient.api.Purchase;

import java.util.List;

public interface PurchaseListener {
    void onItemBought(String sku);

    void onPaymentsError(int errorMsg);

    void onItemNotBought(@Nullable String sku);

    void onPurchasedItemsLoaded(List<Purchase> purchases);

}
