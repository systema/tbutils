
## User Configuration

Quite often, the default tenant and sysadmin passwords are used. `tb-utils` provides a simple means to set them programmatically
to improve the security of a TB deployment.

```kotlin
// Configure thingsboard application user (first run only)
UserConfigurator.updateDefaultAdminUserPassword(tbURL, TBUTILS_DEMO_SYSADMIN_PW)
UserConfigurator.updateDefaultTenantUserPassword(tbURL, TBUTILS_DEMO_TENANT_PW)
```

After a deployment has been secured, `tb-utils` allows creating new application user programmatically. By doing so an
application can bootstrap a tenant configuration without additional (manual) intervention.

```kotlin
UserConfigurator.userConfig(
    url,                      // url of TB 
    TBUTILS_DEMO_SYSADMIN_PW, // sysadmin password 
    "me@my.org",              // user email
    "sErcEt",                 // user password
    "My Tenant"               // user tenant
)
```

## Statistics

The [`stats`](src/main/java/com/systema/eia/iot/tb/stats) package provides utilities to process device telemetry data.

* [`MovingAverage`](src/main/java/com/systema/eia/iot/tb/stats/MovingAverage.kt) - moving average with configurable
  history window.

## Misc

As usual, there is a[`utils`](src/main/java/com/systema/eia/iot/tb/utils) Package including miscellaneous utility
classes.
