
### ThingsBoard Auto-Configuration

The library provides mean to streamline ThingsBoard configuration (e.g. devices, rule chains, dashboards). Instead of configuring ThingsBoard separately, it
allows doing so within the `tb-utils` application itself. This provides multiple benefits:

* Reduced deployment effort
* Streamlined maintenance path, where a new release of an application will also roll out necessary TB configuration
  changes (e.g. schema, rule-chain or dashboard improvements)
* The API also supports merging configuration changes into a running system by

There are 2 dedicated functions, that a developer can instrument to enable application backup and restore.

1. Backup - Download ThingsBoard entities and save them as `json` files
2. Upload - Restore previously persisted entities

Backup and restore are available for all major TB entity types.

See [here](examples/vibration-monitoring/iot-app/src/main/java/com/systema/iot/examples/vibration/TbConfigurator.java)
for a fully worked out example.

#### Backup

This is typically performed after TB has been configured via the web UI, and all relevant settings should be packaged
for deployment.

```java
TbBackup backup = new TbBackup(restClient);
TbConfigPaths configPaths = new TbConfigPaths("tbconfig");

backup.getDeviceProfile().save(System.getenv("DEVICE_PROFILE"),configPaths.profilesPath);
backup.getRuleChain().save("Vibration Root Rule Chain",configPaths.rulesPath);
backup.getWidgetBundle().save("Systema Widgets",configPaths.widgetsBundlePath);
backup.getWidget().save("Systema Widgets","Change Device",configPaths.widgetsPath);
backup.getDashboard().save("Vibration CM",configPaths.dashboardsPath);
```

This will store the entities as json in a user-defined resource directory within the application.

![](docs/userguide/docs/images/entity_persistence_directory_layout.png)

So essentially, the user only needs to declare which entities belong to the application and should be persisted.

#### Upload

Later on during deployment or application update, the persisted entities can be restored/updated using an inverse
procedure:

```java
ConfigUploader uploader=new ConfigUploader(client);
uploader.getDeviceProfile().loadAll("path/to/my-device-profile.json");
uploader.getDashboard().load("path/to/my-dashboard.json");
```

#### Entity Path Model

The config files that `tb-utils` can deal with are `json` files created either via the ThingsBoard web UI export
functionality or by using the `tb-utils` backup feature.

For easier config file handling, there is a utility class structuring the config files in the following way (or a subset
thereof):

```java
var configPaths=new TbConfigPaths("config/root/dir/");
```

This would create a file structure like this:

```
config/root/dir/
├── assets
│   └── xxx.json
├── bundles
│   └── ...
├── customers
│   └── ...
├── dashboards
│   └── ...
├── devices
│   └── ...
├── profiles
│   └── ...
├── relations
│   └── ...
├── rules
│   └── ...
├── tenants
│   └── ...
└── widgets
    └── ...
```