#!/bin/bash -e

if [ -z "$EUROPA_CONFIG" ]; then
    EUROPA_CONFIG=EuropaConfig.json
fi

export STAGE=beta
DEPS_CLASSPATH=`cat target/.classpath`
CLASSPATH=target/classes/:$DEPS_CLASSPATH
JVM_ARGS="-Duser.timezone=UTC -Xmx2000M -Xms2000M -Dlog4j.configuration=file:$(pwd)/log4j-console-only.properties"
$JAVA_HOME/bin/java $JVM_ARGS -cp $CLASSPATH com.distelli.europa.Europa --stage beta --port 5050 --ssl-port 5443 --config "$EUROPA_CONFIG" $@
