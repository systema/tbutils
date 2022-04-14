# Device Simulator App

App to simulate a device connecting to ThingsBoard and continuously streaming vibration data.

## How to Start
```bash
# relative to tb-utils root
cd examples/vibration-monitoring/device-simulator
# build jar
#sudo chmod +x gradlew --> use gitattributes instead
 
./gradlew clean build
# start the app
cd ..
docker-compose up --build -d sim
# stop the app
docker-compose down
```

## How It Behaves

The simulator mimics a machine that is vibrating and regularly sending vibration measurements to ThingsBoard. The vibration level increases over time (tool wear) at a random speed.

When a critical vibration limit is exceeded for a certain amount of time, the tool breaks.

The breakage may be fixed or prevented by carrying out maintenance.

See [Device Twin](#device-twin) for details about related ThingsBoard device attributes.

### Configuration

The simulator may be configured by changing the following environment variables:

- `TB_HOST`: ThingsBoard hostname
- `TB_REST_PORT`: ThingsBoard REST port
- `TB_MQTT_PORT`: ThingsBoard MQTT port
- `DEVICE_NAME`: name of the simulator device in ThingsBoard (a random number is appended in addition)
- `PROFILE_NAME`: name of the simulator device profile in ThingsBoard
- `SAMPLE_FREQ`: number of vibration samples per second to send to ThingsBoard

### Device Twin

#### CLIENT_SCOPE
- `broken`
  - type: `bool`
  - `true`, when the simulated device is broken, because it has been vibrating too strong for too long (set by the simulator)

#### SERVER_SCOPE
- `maintenanceFinished`
  - type: `bool`
  - `true`, when the simulated device has been maintained, resulting in vibration going back to normal (to be set by a server application)

#### LATEST_TELEMETRY
- `vibration`
  - type: `double`
  - the current vibration measurement