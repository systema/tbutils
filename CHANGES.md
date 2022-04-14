# Changes

# v2.0.1-SNAPSHOT

* included user config features
* config loading: more expressive Exceptions, minor refactoring
* rearranged tests package structure
* added class for running app with cli args
* more expressive Exception, if ExtRestClient constructor fails
* **!!! BREAKING CHANGES !!!**
  * renamed TbRollback -> ConfigUploader (incl. base class)

# v2.0.0

* added websocket subscription unit tests
* simplified websocket message parsing implementation
* added device discovery incl. test
* added example application:
  * vibration monitoring
    * device simulator
    * iot app
* **!!! BREAKING CHANGES !!!**
  * removed SYSTEMA dependencies
    * logging: 
      * replaced SYSTEMA logging with slf4j facade (for an adapter implementation see http://svndd.dd.systemagmbh.de/svnrepos/Users/Schmiedgen.Olaf/slf4j-systema/trunk and https://nexus01/#browse/search=keyword%3Dslf4j:d53e2dde3edbeb1e8e382f915378219f)
      * moved LogHelper class to external repo (iot-utils?)
    * exceptions: 
      * moved IotException class to external repo (iot-utils?)
      * replaced SYSTEMA exceptions in method signatures with generic ones

# v1.10.3

* added support to for websocket attribute filtering
* added thread-safety features to device twin
* fixed ignoring attribute history entries in device twin
* improved device twin implementation (code reuse, null-safety)

# v1.10.2

* added functionality to parse subscription ID out of ThingsBoard websocket message (WsParserUtils)

# v1.10.1

* ...

# v1.10

* Implemented support for config change merging when restoring tb entities in `TbRollback`. Currently only the following entity types are supported: Device, DeviceProfile, Dashboard, WidgetBundle & Widget
* Added `substituteVars` to simplify variable substitution in widget json template files

# v1.9

* modified DeviceTwin: now considers two attribute values equal, if their String representations are equal (e.g. "true" == true)
* ExtRestClient alarm handling: added clearAlarmIfActive() and newAlarmIfNotActive(), fixed getAlarms(), added unit tests

# v1.8

* Fixed ExtRestClient#getAlarms() method, added unit test for that
* minor changes in AttributeHistoryTest and RemoveTest (URL handling)

# v1.7

* Added moving average implementation `MovingAverage` for sensor readout smoothing. The model is using an actual `java.time.Duration` time-window and not measurement-counts to account for missing events.

# v1.6

* Changed attribute history keys to not use json because of https://github.com/thingsboard/thingsboard/issues/4657

# v1.5

* Changed LogHelper#COMPONENT_TYPE_IOT visibility to public
* Added attribute historization with`AttributeHistory` (example see `JavaSupportLibUsage`)
* Added `getAttribute` wrapper to ExtRestClient
* Simplified signatures in `DeviceMqttClient`
* Backported patched `RestClient.getAlarms` from scrubber-iiot

## v1.4

* Harmonized instantiation of ExtRestClient and RestClient to require complete URL as argument (and not just host:port)
* Refactored configuration save/restore for more type-safe API


## v1.3.8

* Merged in generic bits from scrubber-app:
  * device twin
  * IoTException
  * logger helper
* Added device-by-profile lookup functionality to ext rest client
* Increased tb REST dependency to current v3.2.2 version

## v1.3.7

* Reworked ERC signatures to consume `DeviceId` first
* More `RestClient` compliant naming
* Deprecated `waitForAttributeChanges` (in favor of WebSocket)


## v1.3.6

* renamed TbMqtt -> TbMqttClient
* moved Scope from com/systema/eia/iot/tb/clients to com/systema/eia/iot/tb/utils
* added TBRemover
* fixed bugs in TbFinder
    * .devices.getAllByProfile
    * WidgetBundleFinder extends ATbFinder
* added tests

## v1.3.1

Changed websocket, tb-rest and mqtt dependencies to `implementation` (to allow transitive use)

## v1.3.0

Initial release to internal nexus
