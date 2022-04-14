# Vibration Monitoring Demo

Objective:
> Demonstrate use of tb-utils using a small demo integration

Demonstrated capabilities

* Automatic ThingsBoard configuration updates
* Automatic device discovery
* Subscribing to and acting upon ThingsBoard device attribute and telemetry updates

## How to run?

We use docker-compose to orchestrate 3 services

1. Thingsboard
2. A [simulated](device-simulator/README.md) edge-device monitoring a vibrating drill. To make the demo more exciting we spin up a configurable number of instances (default 3).
3. A ThingsBoard [application](iot-app/README.md) to monitor the device and to trigger maintenance before the drill breaks.

```bash
alias dc="docker-compose --project-name tbdemo"

# TODO: remove
# publish to local m2-cache when using snapshot-dependency 
#cd ${TBUTILS_HOME} && ./gradlew publishToMavenLocal

cd examples/vibration-monitoring

## TODO inline by using build in Dockerfile once jar is on maven-central
## build the simulator
cd device-simulator
./gradlew distTar
cd ..

## build iot-tb-application 
cd iot-app
./gradlew distTar
cd ..

## start application
dc up --build -d

# Inspect logs
dc logs -f iot-app

## Shut down the application
dc down
```

Once the demo is running, open ThingsBoard (http://localhost:9090), login with user `tbutils@systema.com`, password `tbutils` and review the user dashboard. (**TODO**: link)

What you can see:

* The drill monitoring device is streaming telemetry data (vibration) to ThingsBoard. The vibration is continuously rising.
* The IoT application receives the telemetry data from ThingsBoard and determines the tool state from it (`ok`, `maintenance` (**TODO**: fix)).
* Once the vibration exceeds a certain level, the IoT application triggers maintenance (e.g. by notifying a maintenance person).
* When maintenance is done, the vibration level is low again and the tool continues to operate.