# ConfigCat Java SDK
ConfigCat is a cloud based configuration as a service. It integrates with your apps, backends, websites, 
and other programs, so you can configure them through [this](https://configcat.com) website even after they are deployed.

[![Build Status](https://travis-ci.org/configcat/java-sdk.svg?branch=master)](https://travis-ci.org/configcat/java-sdk)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-client)
[![Coverage Status](https://img.shields.io/codecov/c/github/ConfigCat/java-sdk.svg)](https://codecov.io/gh/ConfigCat/java-sdk)
[![Javadocs](http://javadoc.io/badge/com.configcat/configcat-client.svg)](http://javadoc.io/doc/com.configcat/configcat-client)

## Getting started

**1. Add the package to your project**

*Maven:*
```xml
<dependency>
    <groupId>com.configcat</groupId>
    <artifactId>configcat-client</artifactId>
    <version>2.0.1</version>
</dependency>
```
*Gradle:*
```groovy
compile 'com.configcat:configcat-client:2.0.1'
```
**2. Get your Api Key from [ConfigCat.com](https://configcat.com) portal**
![YourConnectionUrl](https://raw.githubusercontent.com/ConfigCat/java-sdk/master/media/readme01.png  "ApiKey")

**3. Import the ConfigCat package**
```java
import com.configcat.*;
```

**4. Create a ConfigCatClient instance**
```java
ConfigCatClient client = new ConfigCatClient("<PLACE-YOUR-API-KEY-HERE>");
```
**5. (Optional) Prepare a User object for rollout calculation**
```java
User user = User.newBuilder()
        .email("simple@but.awesome.com")
        .country("Awesomnia")
        .build("<PLACE-YOUR-USER-IDENTIFIER-HERE>");
```
**6. Get your config value**
```java
boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "key-of-my-awesome-feature", user, false);
if(isMyAwesomeFeatureEnabled) {
    //show your awesome feature to the world!
}
```
Or use the async APIs:
```java
client.getValueAsync(Boolean.class, "key-of-my-awesome-feature", user, false)
    .thenAccept(isMyAwesomeFeatureEnabled -> {
        if(isMyAwesomeFeatureEnabled) {
            //show your awesome feature to the world!
        }
    });
```

## Android
The minimum supported sdk version is 26 (oreo). Java 1.8 or later is required.
```groovy
android {
    defaultConfig {
        //...
        minSdkVersion 26
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```
You also have to put this line into your manifest xml to enable the library access to the network.
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
## User object
Percentage and targeted rollouts are calculated by the user object you can optionally pass to the configuration requests.
The user object must be created with a **mandatory** identifier parameter which should uniquely identify each user:
```java
User user = User.newBuilder()
        .build("<PLACE-YOUR-USER-IDENTIFIER-HERE>"); // mandatory
```
But you can also set other custom attributes if you'd like to calculate the rollout based on them:
```java
Map<String,String> customAttributes = new HashMap<String,String>();
        customAttributes.put("SubscriptionType", "Free");
        customAttributes.put("Role", "Knight of Awesomnia");

User user = User.newBuilder()
        .email("simple@but.awesome.com")
        .country("Awesomnia")
        .custom(customAttributes)
        .build("<PLACE-YOUR-USER-IDENTIFIER-HERE>"); // mandatory
```
## Configuration
### HttpClient
The ConfigCat client internally uses an [OkHttpClient](https://github.com/square/okhttp) instance to fetch the latest configuration over HTTP. You have the option to override the internal HttpClient with your customized one. For example if your application runs behind a proxy you can do the following:
```java
Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxyHost", proxyPort));

ConfigCatClient client = ConfigCatClient.newBuilder()
                .httpClient(new OkHttpClient.Builder()
                            .proxy(proxy)
                            .build())
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
> As the ConfigCat client maintains the whole lifetime of the internal HttpClient, it's being closed simultaneously with the ConfigCat client, refrain from closing the HttpClient manually.

### Refresh policies
The internal caching control and the communication between the client and ConfigCat are managed through a refresh policy. There are 3 predefined implementations built in the library.
#### 1. Auto polling policy (default)
This policy fetches the latest configuration and updates the cache repeatedly. 
##### Poll interval 
You have the option to configure the polling interval through its builder (it has to be greater than 2 seconds, the default is 60):
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    AutoPollingPolicy.newBuilder()
                        .autoPollIntervalInSeconds(120) // set the polling interval
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
##### Change listeners 
You can set change listeners that will be notified when a new configuration is fetched. The policy calls the listeners only, when the new configuration is differs from the cached one.
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    AutoPollingPolicy.newBuilder()
                        .configurationChangeListener((parser, newConfiguration) -> {
                            // here you can parse the new configuration like this: 
                            // parser.parseValue(Boolean.class, newConfiguration, "key-of-my-awesome-feature", user)                            
                        })
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
If you want to subscribe to the configuration changed event later in your applications lifetime, then you can do the following (this will only work when you have an auto polling refresh policy configured in the ConfigCat client):
```java
client.getRefreshPolicy(AutoPollingPolicy.class)
    .addConfigurationChangeListener((parser, newConfiguration) -> {
        // here you can parse the new configuration like this: 
        // parser.parseValue(Boolean.class, newConfiguration, "key-of-my-awesome-feature", user)  
    });
```
You can check this in action in the [Android sample](https://github.com/ConfigCat/java-sdk/tree/master/samples/android).
#### 2. Expiring cache policy
This policy uses an expiring cache to maintain the internally stored configuration. 
##### Cache refresh interval 
You can define the refresh rate of the cache in seconds, 
after the initial cached value is set this value will be used to determine how much time must pass before initiating a new configuration fetch request through the `ConfigFetcher`.
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    ExpiringCachePolicy.newBuilder()
                        .cacheRefreshIntervalInSeconds(120) // the cache will expire in 120 seconds
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
##### Async / Sync refresh
You can define how do you want to handle the expiration of the cached configuration. If you choose asynchronous refresh then 
when a request is being made on the cache while it's expired, the previous value will be returned immediately 
until the fetching of the new configuration is completed.
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    ExpiringCachePolicy.newBuilder()
                        .asyncRefresh(true) // the refresh will be executed asynchronously
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
If you set the `.asyncRefresh()` to be `false`, the refresh operation will be awaited
until the fetching of the new configuration is completed.

#### 3. Manual polling policy
With this policy every new configuration request on the ConfigCatClient will trigger a new fetch over HTTP.
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> new ManualPollingPolicy(configFetcher,cache));
```

#### Custom Policy
You can also implement your custom refresh policy by extending the `RefreshPolicy` abstract class.
```java
public class MyCustomPolicy extends RefreshPolicy {
    
    public MyCustomPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        super(configFetcher, cache);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        // this method will be called when the configuration is requested from the ConfigCat client.
        // you can access the config fetcher through the super.fetcher() and the internal cache via super.cache()
    }
    
    // optional, in case if you have any resources that should be closed
    @Override
     public void close() throws IOException {
        super.close();
        // here you can close your resources
    }
}
```
> If you decide to override the `close()` method, you also have to call the `super.close()` to tear down the policy appropriately.

Then you can simply inject your custom policy implementation into the ConfigCat client:
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> new MyCustomPolicy(configFetcher, cache)) // inject your custom policy
                .build("<PLACE-YOUR-API-KEY-HERE>");
```

### Custom Cache
You have the option to inject your custom cache implementation into the client. All you have to do is to inherit from the `ConfigCache` abstract class:
```java
public class MyCustomCache extends ConfigCache {
    
    @Override
    public String read() {
        // here you have to return with the cached value
        // you can access the latest cached value in case 
        // of a failure like: super.inMemoryValue();
    }

    @Override
    public void write(String value) {
        // here you have to store the new value in the cache
    }
}
```

Then use your custom cache implementation:
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .cache(new MyCustomCache()) // inject your custom cache
                .build("<PLACE-YOUR-API-KEY-HERE>");
```

### Maximum wait time for synchronous calls
You have the option to set a timeout value for the synchronous methods of the library (`getConfigurationJsonString()`, `getConfiguration()`, `getValue()` etc.) which means
when a sync call takes longer than the timeout value, it'll return with the default.
```java
ConfigCatClient client = ConfigCatClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2) // set the max wait time
                .build("<PLACE-YOUR-API-KEY-HERE>");
```
### Force refresh
Any time you want to refresh the cached configuration with the latest one, you can call the `forceRefresh()` method of the library,
which will initiate a new fetch and will update the local cache.

## Logging
The ConfigCat client uses the facade of [slf4j](https://www.slf4j.org) for logging.

## Samples
* [Console](https://github.com/ConfigCat/java-sdk/tree/master/samples/console)
* [Web app](https://github.com/ConfigCat/java-sdk/tree/master/samples/web)
* [Android](https://github.com/ConfigCat/java-sdk/tree/master/samples/android)
