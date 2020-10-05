# ConfigCat SDK for Java
https://configcat.com

ConfigCat SDK for Java provides easy integration for your application to ConfigCat.

ConfigCat is a feature flag and configuration management service that lets you separate code releases from deployments. You can turn features ON or OFF using the <a href="http://app.configcat.com" target="_blank">ConfigCat Dashboard</a> even after they are deployed. ConfigCat lets you target specific groups of users based on region, email, or any other custom user attribute.

ConfigCat is a <a href="https://configcat.com" target="_blank">hosted feature flag service</a> that lets you manage feature toggles across frontend, backend, mobile, and desktop apps. <a href="https://configcat.com" target="_blank">Alternative to LaunchDarkly</a>. Management app + feature flag SDKs.

[![Build Status](https://travis-ci.com/configcat/java-sdk.svg?branch=master)](https://travis-ci.com/configcat/java-sdk)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-java-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-java-client)
[![Coverage Status](https://img.shields.io/codecov/c/github/ConfigCat/java-sdk.svg)](https://codecov.io/gh/ConfigCat/java-sdk)
[![Javadocs](http://javadoc.io/badge/com.configcat/configcat-java-client.svg)](http://javadoc.io/doc/com.configcat/configcat-java-client)
![License](https://img.shields.io/github/license/configcat/java-sdk.svg)

## Getting started

### 1. Install the Java client
*Maven:*
```xml
<dependency>
  <groupId>com.configcat</groupId>
  <artifactId>configcat-java-client</artifactId>
  <version>[5.1.0,)</version>
</dependency>
```
*Gradle:*
```groovy
compile group: 'com.configcat', name: 'configcat-java-client', version: '5.+'
```

### 2. Go to the <a href="https://app.configcat.com/sdkkey" target="_blank">Connect your applications</a> tab to get your *API Key*:
![API-KEY](https://raw.githubusercontent.com/ConfigCat/java-sdk/master/media/readme01.png  "API-KEY")

### 3. Import *com.configcat.** in your application code
```java
import com.configcat.*;
```

### 4. Create a *ConfigCat* client instance
```java
ConfigCatClient client = new ConfigCatClient("#YOUR-API-KEY#");
```

### 5. Get the setting's value:
```java
boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "isMyAwesomeFeatureEnabled", false);
if(isMyAwesomeFeatureEnabled) {
    doTheNewThing();
} else{
    doTheOldThing();
}
```
Or use the async APIs:
```java
client.getValueAsync(Boolean.class, "isMyAwesomeFeatureEnabled", false)
    .thenAccept(isMyAwesomeFeatureEnabled -> {
        if(isMyAwesomeFeatureEnabled) {
            doTheNewThing();
        } else{
            doTheOldThing();
        }
    });
```

## Getting user-specific setting values with Targeting
Using this feature, you will be able to get different setting values for different users in your application by passing a `User Object` to the `getValue()` function.

Read more about Targeting [here](https://configcat.com/docs/advanced/targeting/).


## User Object
Percentage and targeted rollouts are calculated by the user object passed to the configuration requests.
The user object must be created with a **mandatory** identifier parameter which uniquely identifies each user:
```java
User user = User.newBuilder()
        .build("#USER-IDENTIFIER#"); // mandatory

boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "isMyAwesomeFeatureEnabled", user, false);
if(isMyAwesomeFeatureEnabled) {
    doTheNewThing();
} else{
    doTheOldThing();
}
```

## Sample/Demo app
* [Sample Console App](https://github.com/ConfigCat/java-sdk/tree/master/samples/console)
* [Sample Web app](https://github.com/ConfigCat/java-sdk/tree/master/samples/web)

## Polling Modes
The ConfigCat SDK supports three different polling mechanisms to acquire the setting values from ConfigCat. After the latest setting values are downloaded, they are stored in an internal cache . After that, all requests are served from the cache. Read more about Polling Modes and how to use them at [ConfigCat Java Docs](https://configcat.com/docs/sdk-reference/java/) or [ConfigCat Android Docs](https://configcat.com/docs/sdk-reference/android/).

## Support
If you need help using this SDK, feel free to contact the ConfigCat Staff at [https://configcat.com](https://configcat.com). We're happy to help.

## Contributing
Contributions are welcome.

## About ConfigCat
- [Official ConfigCat SDKs for other platforms](https://github.com/configcat)
- [Documentation](https://configcat.com/docs)
- [Blog](https://configcat.com/blog)
