#!/bin/bash

cd $TRAVIS_BUILD_DIR

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    openssl aes-256-cbc -K $encrypted_77054ff15c0a_key -iv $encrypted_77054ff15c0a_iv -in $TRAVIS_BUILD_DIR/gradle/secring.gpg.enc -out $TRAVIS_BUILD_DIR/gradle/secring.gpg -d
    ./gradlew clean assemble javadoc -Psigning.keyId=$SIGNING_KEY_ID -Psigning.password=$SIGNING_PASSWORD -Psigning.secretKeyRingFile=$TRAVIS_BUILD_DIR/gradle/secring.gpg
else
    ./gradlew clean check
fi
