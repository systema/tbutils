# F.A.Q.


## How to disable verbose Spring RestTemplate logging?

Wanna get rid of verbose log entries on each REST request?

In the project where you're using `tb-utils`, just create a file `src/main/resources/application.properties`  with this
content:

```properties
logging.level.org.springframework=OFF
logging.level.root=OFF
```


## How to run the  tests?

A running TB instance with is required, which must be exposed via 2 environment variables

* `TB_URL` - tb host
* `TB_MQTT_URL` - tb mqtt broker

> This TB instance must have **default username (tenant@thingsboard.org) and pw (tenant)** configured.

To start a local ThingsBoard instance (for details, see <https://thingsboard.io/docs/user-guide/install/docker>):

```bash
docker-compose up -d mytb
```

To run the tests:

```bash
# change if necessary
export TB_URL=http://localhost:8080
export TB_MQTT_URL=http://localhost:1884

./gradlew test
```

## How to release a new version?

See [release.md](https://github.com/systema/tbutils/docs/release.md)
