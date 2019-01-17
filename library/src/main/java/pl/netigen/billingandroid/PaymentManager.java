package pl.netigen.billingandroid;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class PaymentManager implements IPaymentManager, PurchasesUpdatedListener {

    private BillingClient billingClient;
    private PurchaseListener purchaseListener;
    private boolean isServiceConnected;
    private String sku;
    private List<String> skuList;
    private static IPaymentManager paymentManager;
    public Activity activity;
    public static final int RESPONSE_OK = BillingClient.BillingResponse.OK;

    public static IPaymentManager getInstance(Activity activity) {
        if (paymentManager == null) {
            paymentManager = new PaymentManager(activity);
        }
        return (PaymentManager) paymentManager;
    }

    private PaymentManager(Activity activity) {
        this.activity = activity;
        billingClient = BillingClient.newBuilder(activity).setListener(this).build();
    }

    private void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                int responseCode = purchasesResult.getResponseCode();
                if (responseCode != BillingClient.BillingResponse.OK) {
                    purchaseListener.onPaymentsError(responseCode);
                }
                onQueryPurchasesFinished(purchasesResult);
            }
        };
        executeServiceRequest(queryToExecute);
    }

    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) {
            runnable.run();
        } else {
            billingClient = BillingClient.newBuilder(activity).setListener(this).build();
            if (billingClient.isReady()) {
            }else{
                startServiceConnectionAndRun(runnable);
            }
        }
    }

    public void initiatePurchase(String sku, PurchaseListener purchaseListener, Activity activity) {
        this.purchaseListener = purchaseListener;
        this.sku = sku;
        Runnable purchaseFlowRequest = () -> {
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(sku)
                    .setType(BillingClient.SkuType.INAPP)
                    .setOldSkus(null)
                    .build();
            if (billingClient == null) {
                return;
            }
            billingClient.launchBillingFlow(activity, purchaseParams);
        };
        executeServiceRequest(purchaseFlowRequest);
    }

    private void onQueryPurchasesFinished(Purchase.PurchasesResult result) {
        if (billingClient == null || result.getResponseCode() != BillingClient.BillingResponse.OK) {
            purchaseListener.onPaymentsError(result.getResponseCode());
            return;
        }
        onPurchasesUpdated(BillingClient.BillingResponse.OK, result.getPurchasesList());
    }

    private void startServiceConnectionAndRun(final Runnable executeOnSuccess) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    isServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                } else {
                    purchaseListener.onPaymentsError(billingResponseCode);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isServiceConnected = false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    @Override
    public void onPurchasesUpdated(int responseCode, List<Purchase> purchases) {
        if (purchases == null || purchases.size() == 0) {
            purchaseListener.onItemNotBought(null);
        } else {
            for (Purchase purchase : purchases) {
                purchaseListener.onPurchasedItemsLoaded(purchases);
                String purchaseSku = purchase.getSku();
                if (purchaseSku.equals(sku)) {
                    purchaseListener.onItemBought(sku);
                    return;
                }
            }
            purchaseListener.onItemNotBought(sku);
        }
    }

    public void isItemPurchased(String itemSku, PurchaseListener purchaseListener) {
        this.purchaseListener = purchaseListener;
        if (billingClient != null) {
            if (billingClient.isReady()) {
                queryPurchases();
            } else {
                billingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(int responseCode) {
                        queryPurchases();
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        purchaseListener.onPaymentsError(0);
                    }
                });
            }
        } else {
            if (activity != null) {
                billingClient = BillingClient.newBuilder(activity).setListener(this).build();
            } else {
                Log.d(TAG, "isItemPurchased: activity null");
            }
        }
    }

    private static final String TAG = "PaymentManager";

    public void consumeAsync(final String purchaseToken, ItemConsumedListener itemConsumedListener) {
        final ConsumeResponseListener onConsumeListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String purchaseToken) {
                itemConsumedListener.onItemConsumed(responseCode, purchaseToken);
            }
        };

        Runnable consumeRequest = new Runnable() {
            @Override
            public void run() {
                if (billingClient != null) {
                    billingClient.consumeAsync(purchaseToken, onConsumeListener);
                } else {
                    if(activity!=null){
                        billingClient = BillingClient.newBuilder(activity).setListener(PaymentManager.this).build();
                        startServiceConnectionAndRun(this);
                    }
                }
            }
        };
        executeServiceRequest(consumeRequest);
    }
}