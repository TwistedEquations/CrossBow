Crossbow Wear
===========

Crossbow Wear is exact same as Crossbow but built for use on wearable devices. It interconnects with
Crossbow on the phone and handles all the wear <-> handheld communication for you.

Setup
------

Add the CrossbowWear dependency to your build.gradle in your wear project
```groovy
dependencies {
    compile 'com.twistedequations.crossbow:crossbow-wear:0.8.7'
}
```

On the Handheld app you need to have the main Crossbow and the Crossbow wear receiver libraries to
handle the in oncoming requests from the wearable
```groovy
dependencies {
 compile 'com.twistedequations.crossbow:crossbow:0.8.7'
 compile 'com.twistedequations.crossbow:crossbow-wear-receiver:0.8.7'
}
```

you also need to register the CrossbowListenerService in your manifest to listen for wear requests
```xml
<service android:name=".CrossbowListenerService">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.BIND_LISTENER" />
    </intent-filter>
</service>
```
If you already have a WearableListenerService set up for receiving events from a wearable you need to
change it to extend the CrossbowListenerService instead of the WearableListenerService and register it in instead.
```java
public class CustomWearService extends CrossbowListenerService {
}
```
```xml
<service android:name=".CustomWearService">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.BIND_LISTENER" />
    </intent-filter>
</service>
```
If you are using a custom crossbow instance or custom components you to need to let the CrossbowListenerService know
what RequestQueue to use for the wear requests. This is done is your subclass of CrossbowListenerService and overriding the
getRequestQueue() method.
```java
public class CustomWearService extends CrossbowListenerService {
    @Override
     public RequestQueue getRequestQueue() {
         return // Custom request queue here.
     }
 }
```
Lastly if you are using the compatibility api with custom compatibilities you need to add the crossbow wear
crossbow_compatibility string to the android_wear_capabilities string array so that the wearable knows
which node to connect to.
```xml
    <string-array name="android_wear_capabilities">
        <item>@string/crossbow_compatbility</item>
        <item>@string/other_compatibilty</item>
    </string-array>
```

Making Wear Requests
------

When on wear you need to use the CrossbowWear class to obtain a Crossbow instance. This
manages the caching, compression and play services communication for you via the play network class.
```java
Crossbow crossbow = CrossbowWear.get(this);
```
making a network request is the same as the handheld and can use all the same requests
```java
RepoNameRequest repoNameRequest = new RepoNameRequest("https://api.github.com/users/twistedequations/repos", new Response.Listener<List<String>>() {
        @Override
        public void onResponse(List<String> response) {
        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
        }
    });
    CrossbowWear.get(this).add(repoNameRequest);
```

The Image Loaders work perfectly on Wear as well
```java
CrossbowWear.get(this)
    .loadImage()
    .centerCrop()
    .source("http://i.imgur.com/ByktT4N.jpg")
    .fade(200)
    .into(imageView)
    .load();
```

Response Transforming
------

Due to the wearable being a low power, having limited computational power and having a narrow
bandwidth to and from the wearable it makes sense to preform some of the parsing on the handheld to
shrink the network response.

First add the WearRequest interface to your request and return a unique key for the getTransFormerKey()
method an optional bundle of arguments for the transformer to use

```java
public class WearImageRequest extends RecycleImageRequest implements WearRequest {

    private final int maxWidth;
    private final int maxHeight;

    public WearImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight, Bitmap.Config decodeConfig, Response.ErrorListener errorListener) {
        super(url, listener, maxWidth, maxHeight, decodeConfig, errorListener);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public Bundle getTransformerParams() {
        Bundle bundle = new Bundle();
        bundle.putInt("maxWidth", maxWidth);
        bundle.putInt("maxHeight", maxHeight);
        return bundle;
    }

    @Override
    public String getTransFormerKey() {
        return "transformer_key";
    }
```

Next create the matching RequestTransformer on the handheld for the request to compress the data.
The Bundle passed in here will have the values as the Bundle from the WearRequest
getTransformerParams() method.

```java
public class ImageRequestTransformer implements ResponseTransformer {

    @Override
    public byte[] transform(Bundle requestArgs, byte[] data) throws ParseError {

        int width = requestArgs.getInt("width", 500);
        int height = requestArgs.getInt("height", 500);
        Bitmap.Config config = (Bitmap.Config) requestArgs.getSerializable("config");
        if(config == null) {
            config = Bitmap.Config.RGB_565;
        }

        Bitmap bitmap = ImageDecoder.parseImage(data, config, width, height);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
        return stream.toByteArray();
}
```

Lastly register the transformer and its key in a subclass of CrossbowListenerService using the same key
from the getTransFormerKey() method in the WearRequest
```java
public class WearMessageService extends CrossbowListenerService {

    @Override
    public Map<String, ResponseTransformer> getTransformerMap() {
        Map<String, ResponseTransformer> map = new HashMap<>();
        map.put("transformer_key", new CustomResponseTransformer());
        return map;
    }
}
```