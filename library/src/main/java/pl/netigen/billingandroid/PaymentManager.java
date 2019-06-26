package pl.netigen.billingandroid;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class PaymentManager implements IPaymentManager, PurchasesUpdatedListener, TestPaymentManager, AcknowledgePurchaseResponseListener {

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

    private List<SkuDetails> skuDetailsList;
    private ArrayList<String> skuList;
    private BillingPreferencesHelper billingPreferencesHelper;
    private Activity activity;

    public static IPaymentManager createIPaymentManager(Activity activity) {
        return new PaymentManager(activity);
    }

    public static TestPaymentManager createTestPaymentManager(Activity activity) {
        TestPaymentManager testPaymentManager = new PaymentManager(activity);
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add(TEST_PURCHASED);
        skuList.add(TEST_ITEM_UNAVAILABLE);
        skuList.add(TEST_CANCELED);
        ((PaymentManager) testPaymentManager).skuList = skuList;
        return testPaymentManager;
    }

    private PaymentManager(Activity activity) {
        this.activity = activity;
        billingPreferencesHelper = BillingPreferencesHelper.getInstance(activity);
        billingClient = buildBillingClient();
    }

    private BillingClient buildBillingClient() {
        return BillingClient.newBuilder(activity).enablePendingPurchases().setListener(this).build();
    }

    private static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case BillingClient.BillingResponseCode.OK:
                return OK;
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                return BILLING_UNAVAILABLE;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return DEVELOPER_ERROR;
            case BillingClient.BillingResponseCode.ERROR:
                return ERROR;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return FEATURE_NOT_SUPPORTED;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return ITEM_ALREADY_OWNED;
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                return ITEM_NOT_OWNED;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return SERVICE_DISCONNECTED;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                return USER_CANCELED;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return ITEM_UNAVAILABLE;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                return SERVICE_UNAVAILABLE;
            default:
                return ERROR;
        }
    }

    private void queryPurchases() {
        Runnable queryToExecute = () -> {
            if (billingClient == null) return;
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            int responseCode = purchasesResult.getResponseCode();
            if (responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseListener.onPaymentsError(getErrorMessage(responseCode));
            }
            onQueryPurchasesFinished(purchasesResult);
        };
        executeServiceRequest(queryToExecute);
    }

    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) {
            runnable.run();
        } else {
            if (billingClient == null) return;
            billingClient = buildBillingClient();
            if (billingClient.isReady()) {
            } else {
                startServiceConnectionAndRun(runnable);
            }
        }
    }

    private void getSkuDetailsList(SkuDetailsResponseListener listener) {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                listener);
    }

    public void getSkuDetailsList(ArrayList<String> skuList, SkuDetailsResponseListener listener) {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        //Not sure if the line below is really necessary
        this.skuList = skuList;
        billingClient.querySkuDetailsAsync(params.build(),
                listener);
    }

    private Runnable prepareInitPurchaseRunnable(SkuDetails skuDetails) {
        Runnable purchaseFlowRequest = () -> {
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build();
            if (billingClient == null) {
                return;
            }
            billingClient.launchBillingFlow(activity, purchaseParams);
        };
        return purchaseFlowRequest;
    }

    public void initiatePurchase(String sku, PurchaseListener purchaseListener, Activity activity) {
        this.purchaseListener = purchaseListener;
        this.sku = sku;

        if (skuList == null) {
            skuList = new ArrayList<>();
            skuList.add(sku);
        }

        if (skuDetailsList != null) {
            SkuDetails skuDetails = getSkuDetailsForSku(sku);
            executeServiceRequest(prepareInitPurchaseRunnable(skuDetails));
        } else {
            executeServiceRequest(new Runnable() {
                @Override
                public void run() {
                    PaymentManager.this.getSkuDetailsList(new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                PaymentManager.this.skuDetailsList = skuDetailsList;
                                SkuDetails skuDetails = getSkuDetailsForSku(sku);
                                executeServiceRequest(prepareInitPurchaseRunnable(skuDetails));
                            } else {
                                purchaseListener.onPaymentsError(billingResult.getDebugMessage());
                            }
                        }
                    });
                }
            });
        }
    }

    @Nullable
    private SkuDetails getSkuDetailsForSku(String sku) {
        SkuDetails skuDetails = null;
        for (int i = 0; i < skuDetailsList.size(); i++) {
            if (sku.equals(skuDetailsList.get(i).getSku())) {
                skuDetails = skuDetailsList.get(i);
            }
        }
        return skuDetails;
    }

    private void onQueryPurchasesFinished(Purchase.PurchasesResult result) {
        if (billingClient == null || result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            purchaseListener.onPaymentsError(getErrorMessage(result.getResponseCode()));
            return;
        }
        onPurchasesUpdated(result.getBillingResult(), result.getPurchasesList());
    }

    private void startServiceConnectionAndRun(final Runnable executeOnSuccess) {
        billingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                } else {
                    purchaseListener.onPaymentsError(getErrorMessage(billingResult.getResponseCode()));
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

    public void isItemPurchased(String itemSku, PurchaseListener purchaseListener) {
        isItemInSharedPreferences(itemSku, purchaseListener);
        this.sku = itemSku;
        if (skuList == null) {
            skuList = new ArrayList<>();
            skuList.add(itemSku);
        }
        this.purchaseListener = purchaseListener;
        if (billingClient != null) {
            if (billingClient.isReady()) {
                queryPurchases();
            } else {
                billingClient.startConnection(new BillingClientStateListener() {

                    @Override
                    public void onBillingSetupFinished(BillingResult billingResult) {
                        queryPurchases();
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        purchaseListener.onPaymentsError(getErrorMessage(BillingClient.BillingResponseCode.ERROR));
                    }
                });
            }
        } else {
            if (activity != null) {
                billingClient = buildBillingClient();
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
        final ConsumeResponseListener onConsumeListener = (billingResult, purchaseToken1) -> itemConsumedListener.onItemConsumed(getErrorMessage(billingResult.getResponseCode()), purchaseToken1);

        Runnable consumeRequest = new Runnable() {
            @Override
            public void run() {
                if (billingClient != null) {
                    ConsumeParams consumeParams =
                            ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchaseToken)
                                    .setDeveloperPayload("")
                                    .build();
                    billingClient.consumeAsync(consumeParams, onConsumeListener);
                } else {
                    if (activity != null) {
                        billingClient = buildBillingClient();
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

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchaseListener.onPurchasedItemsLoaded(purchases);
            for (Purchase purchase : purchases) {
                String purchaseSku = purchase.getSku();
                if (purchaseSku.equals(sku)) {
                    handlePurchase(purchase);
                    return;
                }
            }
            billingPreferencesHelper.setSkuBought(sku, false);
            purchaseListener.onItemNotBought(sku);
        } else {
            purchaseListener.onPaymentsError(ERROR);
        }
    }

    void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            purchaseListener.onItemBought(sku);
            billingPreferencesHelper.setSkuBought(sku, true);

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
            }
        }else if(purchase.getPurchaseState() == Purchase.PurchaseState.PENDING){
            purchaseListener.onItemNotBought(sku);
        }else{
            purchaseListener.onItemNotBought(sku);
        }
    }

    /** This callback returns failure(response code BILLING_UNAVAILABLE with message API_VERSION_NOT_V9),
     * even though everything seems to be going fine */
    @Override
    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {

    }

}