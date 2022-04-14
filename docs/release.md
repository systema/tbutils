# Release Protocol

## How to publish new version ?

1. Update `CHANGES.md`
2. Set release version in `build.gradle`
3. ... and in `README.md`
4. Run (stepwise):

```bash
## Run tests
./gradlew check

## Extract version from build-file
trim() { while read -r line; do echo "$line"; done; }
tbutils_version='v'$(grep '^version' build.gradle | cut -f2 -d' ' | tr -d "'" | trim)

echo "new version is $tbutils_version"

if [[ $tbutils_version == *"-SNAPSHOT" ]]; then
  echo "ERROR: Won't publish snapshot build $tbutils_version!" 1>&2
  exit 1
fi

# todo automate version patch in readme
#kscript src/test/kotlin/krangl/misc/PatchVersion.kts "${tbutils_version:1}"

# commit pending changes
git status
git commit -am "${tbutils_version} release"


# make sure that are no pending chanes
git diff --exit-code  || echo "There are uncommitted changes"

# tag the current version and push commits and tag
git tag "${tbutils_version}"

git push origin
git push origin --tags

## Build and publish the binary release to nexus
./gradlew publish
```


## Nexus Profile Configuration
[//]: # (todo remove this prior to github publication)

Make sure that system setting are ready

```bash
λ cat ~/.gradle/init.d/nexusConfig.gradle
allprojects{
  ext {
    nexus_repository_release_url=https://nexus01/repository/maven-releases/
    nexus_repository_snapshot_url=https://nexus01/repository/maven-snapshots/
    nexus_repository_read_url=https://nexus01/repository/maven-public

    nexus_repository_release_playground_url=https://nexus01/repository/playground-releases/
    nexus_repository_snapshot_playground_url=https://nexus01/repository/playground-snapshots/

    nexus_repository_npm_url=https://nexus01/repository/npm-public/
    nexus_repository_nodejs_url=https://nexus01/repository/nodejs-dist/

    nexus_username=ask-HRa
    nexus_password=ask-HRa
  }
}
```
