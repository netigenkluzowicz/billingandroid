# Billing Android
Billingandroid is an Android library for dealing with Google Play Billing. It provides abstraction layer over Google billing that will make it easier for you to implement buying and consuming items. 

## Usage

Usage differens depending on whether you use it with or without Netigen Android Api. But general rules are the same.

You need to add this dependency to your build.gradle:

```Groovy
implementation 'com.github.netigenkluzowicz:billingandroid:0.7.0'
```

Here's example of usage with Netigen Api base activities:

### Splash 

Your Splash activity should extends BaseSplashActivity implements PurchaseListener

```Java
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        paymentManager = (PaymentManager) PaymentManager.createIPaymentManager(this);
        paymentManager.isItemPurchased(SKU_STRING_HERE, this);
    }
```

BaseSplashActivity method isNoAdsPaymentAvailable should return true for no ads payment to work properly:

```Java
@Override
    public boolean isNoAdsPaymentAvailable() {
        return true;
    }
```
You should also override clickPay method and initiate purchase in there:

```Java
    @Override
    public void clickPay() {
        paymentManager.initiatePurchase(Const.NO_ADS_SKU, this, this);
    }
```

Lastly implementation of these 3 methods should look like this:

```Java
    @Override
    public void onItemBought(String sku) {
        onNoAdsPaymentProcessingFinished(true);
    }

    @Override
    public void onPaymentsError(String errorMsg) {
        onNoAdsPaymentProcessingFinished(false);
    }

    @Override
    public void onItemNotBought(@Nullable String sku) {
        onNoAdsPaymentProcessingFinished(false);
    }
```

### Other Activities

Every Activity that should react on no ads bought should:

```Java
MainActivity extends BaseBannerActivity implements PurchaseListener {
```
In onCreate method you should check if no ads item was bought:

```Java
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        paymentManager = (PaymentManager) PaymentManager.createIPaymentManager(this);
        paymentManager.isItemPurchased(SKU_STRING_HERE, this);
    }
```

Lastly implementation of these 3 methods should look like this:

```Java
    @Override
    public void onItemBought(String sku) {
        //hide ads
    }

    @Override
    public void onPaymentsError(String errorMsg) {
        //show error message to user
    }

    @Override
    public void onItemNotBought(@Nullable String sku) {
        //item was not bought
    }
```

Any button that should start billing flow for buying no ads should contain this line:

```Java
paymentManager.initiatePurchase(Const.NO_ADS_SKU, this, this);
```

To avoid memory leaks every activity that uses PaymentManager should override its onDestroy method:

```Java
    @Override
    public void onDestroy() {
        paymentManager.onDestroy();
        super.onDestroy();
    }
```


### Testing

Fake visa - "android.test.purchased"
