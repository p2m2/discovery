#
#  - Compile JVM/Js
#  - Tests JVM
#  - publish JVM Sonatype Snapshot for branches : master/develop
#  - publish JVM Sonatype Stagge/Release (wait approval) for tags release
#
#  DOCKER_CONTEXT is a context global env variable for all application github p2m2 organization
#   - DOCKER_USER          -> login
#   - DOCKER_PASS          -> password
#   - ORGANIZATION_NAME    -> organization register on dockerhub
#
#  CREDENTIAL_CONTEXT used by build.sbt
#   - REALM_CREDENTIAL,HOST_CREDENTIAL,LOGIN_CREDENTIAL,PASSWORD_CREDENTIAL
#
#  NPM_CONTEXT
#   - NPM_TOKEN : token (from ~/.npmrc) to publish nodejs lib
#
version: 2.1
orbs:
  node: circleci/node@5.0.2
workflows:
  compile-workflow:
    jobs:
      - compile:
          context:
            - DOCKER_CONTEXT

      - lib_js:
          requires:
            - compile
          context:
            - DOCKER_CONTEXT

      - test_js:
          requires:
            - compile
          context:
            - DOCKER_CONTEXT

      - test_and_coverage_jvm:
          requires:
            - compile
          context:
            - DOCKER_CONTEXT

      - publish_snapshot:
          requires:
            - test_and_coverage_jvm
          context:
            - DOCKER_CONTEXT
            - CREDENTIAL_CONTEXT
            - GPG_CONTEXT
          filters:
            tags:
              ignore: /.*/
            branches:
              only:
                - develop
                - master

      - publish_tag_to_sonatype_stagge_for_release:
          context:
            - DOCKER_CONTEXT
            - CREDENTIAL_CONTEXT
            - GPG_CONTEXT
          filters:
            tags:
              only: /.*/
            branches:
              ignore: /.*/

      - build_webpack_fullOptJS_npm_publish:
          context:
            - DOCKER_CONTEXT
            - CREDENTIAL_CONTEXT
            - NPM_CONTEXT
          filters:
            tags:
              only: /.*/
            branches:
              ignore: /.*/

      - build_release_assembly:
          context:
            - DOCKER_CONTEXT
            - CREDENTIAL_CONTEXT
          filters:
            tags:
              only: /.*/
            branches:
              ignore: /.*/

      - publish-github-release:
          requires:
            - build_release_assembly
          context:
            - DOCKER_CONTEXT
            - CREDENTIAL_CONTEXT
            - GITHUB_CONTEXT
          filters:
            tags:
              only: /.*/
            branches:
              ignore: /.*/

      - check_discovery_valid_fullopjs_cdn_jsdelivr:
          filters:
            tags:
              only: /.*/

executors:

  openjdk:
      working_directory: ~/repo
      docker:
        - image: cimg/openjdk:17.0.2
          auth:
            username: ${DOCKER_USER}
            password: ${DOCKER_PASS}

  virtuoso_environment_executor:
      working_directory: ~/repo
      docker:
        - image: cimg/openjdk:17.0.2
          auth:
            username: ${DOCKER_USER}
            password: ${DOCKER_PASS}

        - image: tenforce/virtuoso:virtuoso7.2.5
          auth:
            username: ${DOCKER_USER}
            password: ${DOCKER_PASS}
          environment:
            VIRT_Parameters_NumberOfBuffers: 51000
            VIRT_Parameters_MaxDirtyBuffers: 37500
            VIRT_Parameters_TN_MAX_memory: 4000000
            VIRT_Parameters_TransactionAfterImageLimit: 500000
            VIRT_SPARQL_ResultSetMaxRows: 1000
            VIRT_SPARQL_MaxDataSourceSize: 10000
            VIRT_SPARQL_MaxQueryCostEstimationTime: 0
            VIRT_SPARQL_MaxQueryExecutionTime: 0
            DBA_PASSWORD: dba
            SPARQL_UPDATE: true
            DEFAULT_GRAPH: "graph:test:discovery:default:"

        - image: inraep2m2/service-static-file-server:latest
          auth:
            username: ${DOCKER_USER}
            password: ${DOCKER_PASS}
          environment:
            CORS: "true"
            DEBUG: "true"

jobs:
  compile:
    executor: openjdk
    environment:
      JVM_OPTS: -Xmx3200m
      TERM: dumb
    steps:
      - checkout
      - run:
          name: Compile - JVM
          command: cat /dev/null | sbt discoveryJVM/test:compile

      - run:
          name: Compile - JS
          command: cat /dev/null | sbt discoveryJS/test:compile

  lib_js:
    executor: openjdk
    working_directory: ~/repo
    steps:
      - checkout
      - run:
          name: fastOptJS
          command: cat /dev/null | sbt discoveryJS/fastOptJS

      - run:
          name: fullOptJS
          command: cat /dev/null | sbt discoveryJS/fullOptJS

  test_and_coverage_jvm:
    executor: virtuoso_environment_executor
    environment:
      JVM_OPTS: -Xmx3200m
      TERM: dumb
    steps:
      - checkout
      - run:
          name: Test JVM env and Coverage
          command: |
            cat /dev/null | sbt clean coverage discoveryJVM/Test/test discoveryJVM/coverageReport
            bash <(curl -s https://codecov.io/bash)
            bash <(curl -Ls https://coverage.codacy.com/get.sh) report
      - store_artifacts:
          path: discovery/jvm/target/test-reports

  test_js:
      executor: virtuoso_environment_executor
      environment:
        JVM_OPTS: -Xmx3200m
        TERM: dumb
      steps:
        - checkout
        - setup_remote_docker
        - node/install:
            node-version: '17.4'
        - run:
            name: Test ScalaJs/JavaScript
            command: sbt discoveryJS/test
        - run:
            name: Test TypeScript
            command: |
              ## bug with Node 17 -> https://github.com/webpack/webpack/issues/14532
              export NODE_OPTIONS=--openssl-legacy-provider
              sbt "discoveryJS / Compile / fastOptJS / webpack"
              sbt npmPackageJson
              npm i
              npm test

  publish_snapshot:
    executor: openjdk
    steps:
      - checkout
      - run:
          name: Snapshot publication
          command: |
            export DISCOVERY_VERSION="${CIRCLE_BRANCH}-SNAPSHOT"
            echo "Importing key"
            echo -e "$GPG_KEY" | gpg --import
            sbt publish

  publish_tag_to_sonatype_stagge_for_release:
    executor: openjdk
    steps:
      - checkout
      - run:
          name: Snapshot publication
          command: |
            export DISCOVERY_VERSION="${CIRCLE_TAG}"
            echo "Importing key"
            echo -e "$GPG_KEY" | gpg --import
            sbt publish
  

  build_release_assembly:
    executor: openjdk
    steps:
      - checkout
      - run:
          name: Release Assembly
          command: |
            export DISCOVERY_VERSION="${CIRCLE_TAG}"
            sbt discoveryJVM/assembly
            mkdir -p build
            find . -name discovery-assembly-${CIRCLE_TAG}.jar
            mv $(find . -name d*-assembly-*.jar) build/
            pwd
      - persist_to_workspace:
          root: build
          paths:
            - .
  build_webpack_fullOptJS_npm_publish:
    executor: openjdk
    steps:
      - checkout
      - node/install:
          node-version: '17.4'
      - run:
          name: Authenticate with registry
          command: echo "//registry.npmjs.org/:_authToken=${NPM_TOKEN}" > ~/.npmrc
      - run:
          name: WebPack fullOptJS
          command: |
            export NODE_OPTIONS=--openssl-legacy-provider
            npm config set registry https://registry.npmjs.org
            export DISCOVERY_VERSION="${CIRCLE_TAG}"
            sbt discoveryJS/fullOptJS/webpack
            sbt npmPackageJson
            npm unpublish @p2m2/discovery@${CIRCLE_TAG}
            npm config ls -l | grep userconf
            npm publish --access public

  publish-github-release:
    docker:
      - image: circleci/golang:1.16
    steps:
      - checkout
      - attach_workspace:
          at: build
      - run:
          name: "Publish Release on GitHub"
          command: |
            go get github.com/tcnksm/ghr
            VERSION=${CIRCLE_TAG:="unknown"}
            JAR="discovery-$VERSION.jar"
            mv $(find build -name discovery*.jar) $JAR
            ghr -t ${GITHUB_TOKEN} \
                -u ${CIRCLE_PROJECT_USERNAME} \
                -r ${CIRCLE_PROJECT_REPONAME} \
                -c ${CIRCLE_SHA1} \
                -n $VERSION \
                -delete $VERSION $JAR

  check_discovery_valid_fullopjs_cdn_jsdelivr:
    executor: openjdk
    steps:
      - checkout
      - setup_remote_docker
      - run:
          name: compare checksum scala files
          command: |

            cat $(find . -name *.scala | sort -V | grep -v SWDiscoveryVersionAtBuildTime.scala) | md5sum > ./checksum

            F1=./checksum
            F2=./dist/checksum

            if [ "$(diff -q $F1 $F2)" != ""  ]; then
               echo "current checksum : "$(cat checksum)
               echo "repo checksum    : "$(cat dist/checksum)
               exit 1;
            fi ;
