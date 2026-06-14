### MQTT Event Publishing

Informix Post Commit Trigger allows to publish database changes as JSON messages to an MQTT broker after successful transaction commits. 

Typical use cases:

- Change Data Capture (CDC)
- IoT integrations
- Event-driven architectures
- Real-time synchronization
- Message-driven workflows

## Requirements

- Java 17+
- GNU Make
- Java json-20210307.jar and org.eclipse.paho.client.mqttv3-1.2.5.jar libraries

## Build

```bash
make build
```
## Install

```bash
make install
```

This script copies main jar file "MqttPublish.jar" to $INFORMIXDIR/extend/krakatoa. The required libraries are copied to $INFORMIXDIR/libjars

Then, you need to change the JVPCLASSPATH parameter on $INFORMIXDIR/etc/$ONCONFIG to include the required libraries in the CLASSPATH:

```txt
JVPCLASSPATH $INFORMIXDIR/extend/krakatoa/krakatoa.jar:$INFORMIXDIR/libjars/json-20210307.jar:$INFORMIXDIR/libjars/org.eclipse.paho.client.mqttv3-1.2.5.jar
```

## Test environment

To test if jar libraries are properly installed, you can connect to any database using dbaccess and execute the publish procedure directly:

```sql
execute procedure j_Json2Mqtt('{}');
```
