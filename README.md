# ConfigCat SDK for Java
https://configcat.com

ConfigCat SDK for Java provides easy integration for your application to ConfigCat.

ConfigCat is a feature flag and configuration management service that lets you separate releases from deployments. You can turn your features ON/OFF using <a href="http://app.configcat.com" target="_blank">ConfigCat Management Console</a> even after they are deployed. ConfigCat lets you target specific groups of users based on region, email or any other custom user attribute.

[![Build Status](https://travis-ci.com/configcat/java-sdk.svg?branch=master)](https://travis-ci.com/configcat/java-sdk)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-client)
[![Coverage Status](https://img.shields.io/codecov/c/github/ConfigCat/java-sdk.svg)](https://codecov.io/gh/ConfigCat/java-sdk)
[![Javadocs](http://javadoc.io/badge/com.configcat/configcat-client.svg)](http://javadoc.io/doc/com.configcat/configcat-client)
![License](https://img.shields.io/github/license/configcat/node-sdk.svg)

## Getting started

### 1. Install the package
*Maven:*
```xml
<dependency>
    <groupId>com.configcat</groupId>
    <artifactId>configcat-client</artifactId>
    <version>[2.0.0,)</version>
</dependency>
```
*Gradle:*
```groovy
compile 'com.configcat:configcat-client:2.+'
```

### 2. Go to <a href="https://app.configcat.com/connect" target="_blank">Connect your application</a> tab to get your *API Key*:
![API-KEY](https://raw.githubusercontent.com/ConfigCat/java-sdk/master/media/readme01.png  "API-KEY")

### 3. Import *com.configcat.** to your application
```java
import com.configcat.*;
```

### 4. Create the *ConfigCat* client instance
```java
ConfigCatClient client = new ConfigCatClient("#YOUR-API-KEY#");
```

### 5. Get your setting value:
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

## Getting user specific setting values with Targeting
Using this feature, you will be able to get different setting values for different users in your application by passing a `User Object` to the `getValue()` function.

Read more about [Targeting here](https://docs.configcat.com/docs/advanced/targeting/).


## User object
Percentage and targeted rollouts are calculated by the user object you can optionally pass to the configuration requests.
The user object must be created with a **mandatory** identifier parameter which should uniquely identify each user:
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
* [Sample Android app](https://github.com/ConfigCat/java-sdk/tree/master/samples/android)

## Polling Modes
The ConfigCat SDK supports 3 different polling mechanisms to acquire the setting values from ConfigCat. After latest setting values are downloaded, they are stored in the internal cache then all requests are served from there. Read more about Polling Modes and how to use them at [ConfigCat Java Docs](https://docs.configcat.com/docs/sdk-reference/java/) or [ConfigCat Android Docs](https://docs.configcat.com/docs/sdk-reference/android/).

## Support
If you need help how to use this SDK feel free to to contact the ConfigCat Staff on https://configcat.com. We're happy to help.

## Contributing
Contributions are welcome.

## About ConfigCat
- [Official ConfigCat SDK's for other platforms](https://github.com/configcat)
- [Documentation](https://docs.configcat.com)
- [Blog](https://blog.configcat.com)
