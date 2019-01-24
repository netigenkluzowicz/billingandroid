package pl.netigen.billingandroid;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class PaymentManager implements IPaymentManager, PurchasesUpdatedListener, TestPaymentManager {

    private BillingClient billingClient;
    private PurchaseListener purchaseListener;
    private boolean isServiceConnected;
    private String sku;

    public static final String OK = "OK";
    public static final String BILLING_UNAVAILABLE = "BILLING_UNAVAILABLE";
    public static final String DEVELOPER_ERROR = "DEVELOPER_ERROR";
    public static final String ERROR = "ERROR";
    public static final String FEATURE_NOT_SUPPORTED = "FEATURE_NOT_SUPPORTED";
    public static final String ITEM_ALREADY_OWNED = "ITEM_ALREADY_OWNED";
    public static final String SERVICE_DISCONNECTED = "SERVICE_DISCONNECTED";
    public static final String USER_CANCELED = "USER_CANCELED";
    public static final String ITEM_UNAVAILABLE = "ITEM_UNAVAILABLE";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String ITEM_NOT_OWNED = "ITEM_NOT_OWNED";
    public static final String TEST_PURCHASED = "android.test.purchased";
    public static final String TEST_CANCELED = "android.test.canceled";
    public static final String TEST_ITEM_UNAVAILABLE = "android.test.item_unavailable";

    private BillingPreferencesHelper billingPreferencesHelper;
    private Activity activity;

    public static IPaymentManager createIPaymentManager(Activity activity) {
        return (IPaymentManager) new PaymentManager(activity);
    }

    public static TestPaymentManager createTestPaymentManager(Activity activity) {
        return (TestPaymentManager) new PaymentManager(activity);
    }

    private PaymentManager(Activity activity) {
        this.activity = activity;
        billingPreferencesHelper = BillingPreferencesHelper.getInstance(activity);
        billingClient = BillingClient.newBuilder(activity).setListener(this).build();
    }

    private static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case BillingClient.BillingResponse.OK:
                return OK;
            case BillingClient.BillingResponse.BILLING_UNAVAILABLE:
                return BILLING_UNAVAILABLE;
            case BillingClient.BillingResponse.DEVELOPER_ERROR:
                return DEVELOPER_ERROR;
            case BillingClient.BillingResponse.ERROR:
                return ERROR;
            case BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED:
                return FEATURE_NOT_SUPPORTED;
            case BillingClient.BillingResponse.ITEM_ALREADY_OWNED:
                return ITEM_ALREADY_OWNED;
            case BillingClient.BillingResponse.ITEM_NOT_OWNED:
                return ITEM_NOT_OWNED;
            case BillingClient.BillingResponse.SERVICE_DISCONNECTED:
                return SERVICE_DISCONNECTED;
            case BillingClient.BillingResponse.USER_CANCELED:
                return USER_CANCELED;
            case BillingClient.BillingResponse.ITEM_UNAVAILABLE:
                return ITEM_UNAVAILABLE;
            case BillingClient.BillingResponse.SERVICE_UNAVAILABLE:
                return SERVICE_UNAVAILABLE;
            default:
                return ERROR;
        }
    }

    private void queryPurchases() {

        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                if (billingClient == null) return;
                Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                int responseCode = purchasesResult.getResponseCode();
                if (responseCode != BillingClient.BillingResponse.OK) {
                    purchaseListener.onPaymentsError(getErrorMessage(responseCode));
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
            if (billingClient == null) return;
            billingClient = BillingClient.newBuilder(activity).setListener(this).build();
            if (billingClient.isReady()) {
            } else {
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
            if (billingClient == null) return;
            billingClient.launchBillingFlow(activity, purchaseParams);
        };
        executeServiceRequest(purchaseFlowRequest);
    }

    private void onQueryPurchasesFinished(Purchase.PurchasesResult result) {
        if (billingClient == null || result.getResponseCode() != BillingClient.BillingResponse.OK) {
            purchaseListener.onPaymentsError(getErrorMessage(result.getResponseCode()));
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
                    purchaseListener.onPaymentsError(getErrorMessage(billingResponseCode));
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
        }
        billingClient = null;
        activity = null;
    }

    @Override
    public void onPurchasesUpdated(int responseCode, List<Purchase> purchases) {
        if (purchases == null) {
            purchaseListener.onPaymentsError(ERROR);
        } else {
            purchaseListener.onPurchasedItemsLoaded(purchases);
            for (Purchase purchase : purchases) {
                String purchaseSku = purchase.getSku();
                if (purchaseSku.equals(sku)) {
                    purchaseListener.onItemBought(sku);
                    billingPreferencesHelper.setSkuBought(sku, true);
                    return;
                }
            }
            purchaseListener.onItemNotBought(sku);
        }
    }

    public void isItemPurchased(String itemSku, PurchaseListener purchaseListener) {
        if (isItemInSharedPreferences(itemSku, purchaseListener)) return;

        this.sku = itemSku;
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
                        purchaseListener.onPaymentsError(getErrorMessage(BillingClient.BillingResponse.ERROR));
                    }
                });
            }
        } else {
            if (activity != null) {
                billingClient = BillingClient.newBuilder(activity).setListener(this).build();
            } else {

            }
        }
    }

    private boolean isItemInSharedPreferences(String itemSku, PurchaseListener purchaseListener) {
        if (billingPreferencesHelper.wasSkuChecked(itemSku)) {
            if (billingPreferencesHelper.isSkuBought(itemSku)) {
                purchaseListener.onItemBought(itemSku);
            } else {
                purchaseListener.onItemNotBought(itemSku);
            }
            return true;
        }
        return false;
    }

    public void consumeAsync(final String purchaseToken, ItemConsumedListener itemConsumedListener) {
        final ConsumeResponseListener onConsumeListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String purchaseToken) {
                itemConsumedListener.onItemConsumed(getErrorMessage(responseCode), purchaseToken);
            }
        };

        Runnable consumeRequest = new Runnable() {
            @Override
            public void run() {
                if (billingClient != null) {
                    billingClient.consumeAsync(purchaseToken, onConsumeListener);
                } else {
                    if (activity != null) {
                        billingClient = BillingClient.newBuilder(activity).setListener(PaymentManager.this).build();
                        startServiceConnectionAndRun(this);
                    }
                }
            }
        };
        executeServiceRequest(consumeRequest);
    }

    public void testConsumeAsyncCanceled(String packageName, ItemConsumedListener itemConsumedListener) {
        consumeAsync("inapp:" + packageName + ":" + TEST_CANCELED, itemConsumedListener);
    }

    public void testConsumeAsyncItemUnavailable(String packageName, ItemConsumedListener itemConsumedListener) {
        consumeAsync("inapp:" + packageName + ":" + TEST_ITEM_UNAVAILABLE, itemConsumedListener);
    }

    public void testConsumeAsyncPurchased(String packageName, ItemConsumedListener itemConsumedListener) {
        consumeAsync("inapp:" + packageName + ":" + TEST_PURCHASED, itemConsumedListener);
    }

    public void initTestPurchased(PurchaseListener purchaseListener, Activity activity) {
        initiatePurchase(TEST_PURCHASED, purchaseListener, activity);
    }

    public void initTestCanceled(PurchaseListener purchaseListener, Activity activity) {
        initiatePurchase(TEST_CANCELED, purchaseListener, activity);
    }

    public void initTestItemUnavailable(PurchaseListener purchaseListener, Activity activity) {
        initiatePurchase(TEST_ITEM_UNAVAILABLE, purchaseListener, activity);
    }

}