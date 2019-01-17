package pl.netigen.billingandroid;

import android.app.Activity;

public interface TestPaymentManager {
    void initTestPurchased(PurchaseListener purchaseListener, Activity activity);

    void initTestCanceled(PurchaseListener purchaseListener, Activity activity);

    void initTestItemUnavailable(PurchaseListener purchaseListener, Activity activity);

    void testConsumeAsyncPurchased(String packageName, ItemConsumedListener itemConsumedListener);

    void testConsumeAsyncItemUnavailable(String packageName, ItemConsumedListener itemConsumedListener);

    void testConsumeAsyncCanceled(String packageName, ItemConsumedListener itemConsumedListener);
}
