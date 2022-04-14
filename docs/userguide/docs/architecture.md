<!--# Examples-->

`tbutils` provides different APIs to streamline IIoT application development.

### Device Auto-Discovery

`tb-utils` allows to [discover](src/main/java/com/systema/eia/iot/tb/utils/DeviceDiscovery.java) all devices of a given
device profile registered with ThingsBoard, grouped into active and inactive devices. A device is active, if
its `SERVER_SCOPE` attribute `active` is currently `true`.

```java
// action to handle discovered active devices
var activeAction=new Consumer<List<Device>>(){
    @Override  
    public void accept(List<Device> activeDevices){
        for(Device dev:activeDevices){
        System.out.println("Found active device "+dev.getName());
        // open websocket connection to receive attribute /telemetry events, create handlers etc.
        }
    }
};

// action to handle discovered inactive devices
var inactiveAction=new Consumer<List<Device>>(){
@Override 
public void accept(List<Device> inactiveDevices){
        for(Device dev:inactiveDevices){
        System.out.println("Found inactive device "+dev.getName());
        // close open connections, remove handlers etc.
        }
    }
  };

// start discovery and schedule it to run every 20s
var discoveryTask=new DeviceDiscovery(tbRestClient,deviceProfile,activeAction,inactiveAction);

var executor=Executors.newScheduledThreadPool(1);
executor.scheduleAtFixedRate(discoveryTask,0,20,TimeUnit.SECONDS);
```

### Device Twin

A `tbutils` [DeviceTwin](src/main/java/com/systema/eia/iot/tb/utils/DeviceTwin.java) keep a local copy of ThingsBoard device attributes:

```kotlin
val twin = DeviceTwin(device)

client.subscribeToWS(device.id, SubscriptionType.SHARED_SCOPE) { updates ->
    updates.forEach { twin.update(SubscriptionType.SHARED_SCOPE, it) }
}
```

### Simplified Entity Management

Find entities in ThingsBoard with `com.systema.eia.iot.tb.persistence.search.TbFinder`

* `getById()` - by id
* `getByName()` - by name
* `getAll()` - all of them within a tenant

Java example:

```java
ExtRestClient client = new ExtRestClient(tbUrl);
TbFinder finder = new ExtRestClient(client);

RuleChain rulChain = finder.getRuleChain().getByName("my rule chain");
```

Delete entities in ThingsBoard with `com.systema.eia.iot.tb.persistence.remove.TbRemover`

* `findAndRemoveByName()` - by name
* `findAndRemoveById()` - by entity id

Kotlin example

```kotlin
val remover = TbRemover(TB_URL)

remover.device.findAndRemoveByName("my device")
remover.device.findAndRemoveById("<eniity id>")
```
