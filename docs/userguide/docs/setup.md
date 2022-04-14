# Installation

`tbutils` requires Java11 or higher.


## How to use?

`tb-utils` is released to maven-central. To add it to your project, simply add is dependency to your gradle or maven
project definition.

```gradle
implementation "com.systema.eia.iot:tb-utils:2.0.1"
```

Builds are hosted on [maven-central](https://search.maven.org/search?q=a:tbutils) supported by the great folks at [sonatype](https://www.sonatype.com/).

## Jitpack Integration

You can also use [JitPack with Maven or Gradle](https://jitpack.io/#systema/tbutils) to include the latest snapshot as a dependency in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
        implementation 'com.systema.eia.iot:tbutils:-SNAPSHOT'
}
```


## How to build it from sources?

To build and install it into your local maven cache, simply clone the repo and run
```bash
./gradlew install
```

Clearly, we could also build jar and use it directly with `./gradlew jar` but this is not recommended.