#!/bin/bash

#
# Copyright (C) 2021-2023 Rick Busarow
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# exit when any command fails
set -e

# keep track of the last executed command
trap 'last_command=$current_command; current_command=$BASH_COMMAND' DEBUG
# echo an error message before exiting
trap 'echo "\"${last_command}\" command filed with exit code $?."' EXIT

./gradlew clean

# Publish Maven release
./gradlew publish --no-daemon --no-configuration-cache

# Publish to Gradle Plugin Portal
./gradlew publishPlugins --no-configuration-cache

# Create new website docs version
./gradlew versionDocs

# Set all versions in the root README to the new version
./gradlew updateProjectReadmeVersionRefs --no-configuration-cache

# Copy the CHANGELOG from project root to the website dir and update its formatting
./gradlew updateWebsiteChangelog

# Capture a snapshot of the current version's dokka api docs
./gradlew dokkaHtmlMultiModule syncDokkaToArchive

echo
echo ' ___ _   _  ___ ___ ___  ___ ___'
echo '/ __| | | |/ __/ __/ _ \/ __/ __|'
echo '\__ \ |_| | (_| (_|  __/\__ \__ \'
echo '|___/\__,_|\___\___\___||___/___/'
echo
echo
echo The release is done and a new docs version has been created for Docusaurus.
echo
echo These changes need to get merged into main and published.
echo
echo
