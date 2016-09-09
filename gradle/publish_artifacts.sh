#!/bin/bash
cd $TRAVIS_BUILD_DIR

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then 
    echo "Publishing artifacts to Sonatype OSS..." 
    ./gradlew upload -Psigning.keyId=$SIGNING_KEY_ID -Psigning.password=$SIGNING_PASSWORD -Psigning.secretKeyRingFile=$TRAVIS_BUILD_DIR/gradle/secring.gpg
else
    echo "Skipping artifact publish for $TRAVIS_BRANCH, $TRAVIS_BUILD_NUMBER"
fi
