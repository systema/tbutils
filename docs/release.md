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
#./gradlew publish
```
