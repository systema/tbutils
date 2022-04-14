### Improved Connectivity

#### Extended Rest Client

[//]: # (**TODO** since the extended rest-client is a key feature it could be documented in more detail with a bit more structure)
[//]: # (**TODO** replace links to source code with links to API docs)

The [`ExtRestClient`](src/main/java/com/systema/eia/iot/tb/clients/ExtRestClient.kt) is a wrapper
around [ThingsBoard Java Client](https://thingsboard.io/docs/reference/rest-client/) to extend functionality. Here are
some highlights.

Connect to ThingsBoard and obtain information of a certain device (Java example):

```java
var client=new ExtRestClient(url,user,password);
var device=client.getOrCreateDevice("myDevice","myProfile");
```

Find all devices of a certain profile (Java example):

```java
List<Device> devices=client.getDevicesByProfile("myProfile");
```

Read or update device attributes or telemetry (Java example):

```java
client.getAttribute(device.getId(),Scope.CLIENT_SCOPE,"myAttribute");
client.saveAttribute(device.getId(),Scope.SERVER_SCOPE,"myAttribute",42);
client.sendTelemetry(device.getId(),"temperature",47.11);
```

Subscribe to attribute updates, filtered by names (Kotlin example):

```kotlin
client.subscribeToWS(device.id, SubscriptionType.SHARED_SCOPE, listOf("temperature", "humidity")) { changes ->
    changes.forEach { println("Received attribute update: $it") }
}
```

... or in Java:

```java
WsSubscribeKt.subscribeToWS(client, device.getId(), SubscriptionType.SHARED_SCOPE, List.of("temperature", "humidity"),
    (changes) -> {
        changes.forEach((change) -> System.out.println("Received attribute update: " + change));
        return Unit.INSTANCE; // ~void; required for Java-Kotlin compatibility
    });
```

Clear any active alarm or create a new alarm, if none of the same type is currently active (Java example):

```java
client.newAlarmIfNotActive(device.getId(),device.getName(),"myAlarmType",AlarmSeverity.WARNING);
client.clearAlarmIfActive(device.getId(),device.getName(),"myAlarmType");
```

Store attribute changes as telemetry. Each attribute update, that is received by the iot application, will be sent to
ThingsBoard as a telemetry item, named according to the attribute name and scope. E.g. each update of a `status`
attribute in the `SHARED_SCOPE` would now be stored as `attrscope__SHARED_SCOPE__name__status` telemetry value. (Kotlin
example):

```kotlin
client.saveAttributeChanges(device.id)
```

#### Mqtt Client

The [`DeviceMqttClient`](src/main/java/com/systema/eia/iot/tb/clients/DeviceMqttClient.kt) allows to send and receive
data to/from ThingsBoard via MQTT.

<!-- ### The [`ws`](src/main/java/com/systema/eia/iot/tb/ws) Package

Classes to connect to ThingsBoard via websocket and handle incoming telemetry / attribute updates.   -->
