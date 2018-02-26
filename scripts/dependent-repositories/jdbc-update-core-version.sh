#!/bin/bash
#
# Update the core library dependency of the jdbc-storage repository to the latest version.

readonly SOURCE_REPOSITORY=https://api.github.com/repos/SpineEventEngine/core-java
readonly SOURCE_FILE_WITH_VERSION='ext.gradle'
readonly SOURCE_VERSION_VARIABLE='SPINE_VERSION'

readonly TARGET_REPOSITORY=https://api.github.com/repos/SpineEventEngine/jdbc-storage
readonly TARGET_FILE_WITH_VERSION='ext.gradle'
readonly TARGET_VERSION_VARIABLE='coreVersion'

readonly NEW_BRANCH_NAME='update-core-version'
readonly BRANCH_TO_MERGE_INTO='master'

readonly COMMIT_MESSAGE='Update version of the Spine core modules'
readonly PULL_REQUEST_TITLE='Update version of the Spine core modules'
readonly PULL_REQUEST_BODY='Auto-generated PR to update core library dependency to the latest version.'
readonly PULL_REQUEST_ASSIGNEE='armiol'

function main() {
    local gitAuthorizationToken="$1"

    chmod +x ./scripts/dependent-repositories/update-dependency-version.sh
    ./scripts/dependent-repositories/update-dependency-version.sh \
        "${gitAuthorizationToken}" \
        "${SOURCE_REPOSITORY}" \
        "${SOURCE_FILE_WITH_VERSION}" \
        "${SOURCE_VERSION_VARIABLE}" \
        "${TARGET_REPOSITORY}" \
        "${TARGET_FILE_WITH_VERSION}" \
        "${TARGET_VERSION_VARIABLE}" \
        "${NEW_BRANCH_NAME}" \
        "${BRANCH_TO_MERGE_INTO}" \
        "${COMMIT_MESSAGE}" \
        "${PULL_REQUEST_TITLE}" \
        "${PULL_REQUEST_BODY}" \
        "${PULL_REQUEST_ASSIGNEE}"
}

main "$@"
