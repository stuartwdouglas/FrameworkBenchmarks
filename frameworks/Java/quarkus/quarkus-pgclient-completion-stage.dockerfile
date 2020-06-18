FROM maven:3.6.3-jdk-11-slim as maven
WORKDIR /quarkus
COPY pom.xml pom.xml
COPY base/pom.xml base/pom.xml
COPY hibernate/pom.xml hibernate/pom.xml
COPY hibernate-event-loop/pom.xml hibernate-event-loop/pom.xml
COPY hibernate-reactive/pom.xml hibernate-reactive/pom.xml
COPY hibernate-reactive-completion-stage/pom.xml hibernate-reactive-completion-stage/pom.xml
COPY pgclient/pom.xml pgclient/pom.xml
COPY pgclient-completion-stage/pom.xml pgclient-completion-stage/pom.xml

RUN mkdir -p /root/.m2/repository/io
COPY m2-quarkus /root/.m2/repository/io/quarkus

RUN mvn dependency:go-offline -q -pl base

COPY base/src/main/resources base/src/main/resources
COPY hibernate/src hibernate/src
COPY hibernate-event-loop/src hibernate-event-loop/src
COPY hibernate-reactive/src hibernate-reactive/src
COPY hibernate-reactive-completion-stage/src hibernate-reactive-completion-stage/src
COPY pgclient/src pgclient/src
COPY pgclient-completion-stage/src pgclient-completion-stage/src

RUN mvn package -q -pl pgclient-completion-stage -am

FROM openjdk:11.0.6-jdk-slim
WORKDIR /quarkus
COPY --from=maven /quarkus/pgclient-completion-stage/target/lib lib
COPY --from=maven /quarkus/pgclient-completion-stage/target/pgclient-completion-stage-1.0-SNAPSHOT-runner.jar app.jar
CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-Djava.lang.Integer.IntegerCache.high=10000", "-Dvertx.disableHttpHeadersValidation=true", "-Dvertx.disableMetrics=true", "-Dvertx.disableH2c=true", "-Dvertx.disableWebsockets=true", "-Dvertx.flashPolicyHandler=false", "-Dvertx.threadChecks=false", "-Dvertx.disableContextTimings=true", "-Dvertx.disableTCCL=true", "-Dhibernate.allow_update_outside_transaction=true", "-Djboss.threads.eqe.statistics=false", "-jar", "app.jar"]
