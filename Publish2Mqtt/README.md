### MQTT Event Publishing

Informix Post Commit Trigger allows to publish database changes as JSON messages to an MQTT broker after successful transaction commits. 

Typical use cases:

- Change Data Capture (CDC)
- IoT integrations
- Event-driven architectures
- Real-time synchronization
- Message-driven workflows


`PublishMqtt` is a Java utility class that publishes JSON messages to an MQTT broker using the Eclipse Paho MQTT client library.

The class:

1. Loads MQTT configuration from a file.
2. Creates an MQTT client connection.
3. Optionally derives the MQTT topic from values contained in the JSON payload.
4. Publishes the JSON message to the MQTT broker.
5. Disconnects from the broker.

The class is designed to be invoked from external applications (such as Informix UDRs).

## Dependencies

### MQTT Client

- Eclipse Paho MQTT Client

```xml
org.eclipse.paho.client.mqttv3
```

### JSON Processing

- JSON-Java (`org.json`)

```xml
org.json.JSONObject
```

## Configuration File

The application loads its configuration from:

```text
$INFORMIXDIR/etc/mqtt.conf
```

### Configuration Parameters

| Property | Description |
|-----------|-------------|
| `brokerUrl` | MQTT broker URL. |
| `clientId` | MQTT client identifier. If empty, the hostname is used. |
| `username` | MQTT username (Optional). |
| `password` | MQTT password (Optional). |
| `topic` | MQTT topic where messages will be published, You can configure placeholders replaced by JSON properties. |
| `qosLevel` | MQTT QoS Level (Default 0). |
| `debug` | Enables debug logging when set to `true`. |

### Example Configuration

```properties
brokerUrl=tcp://mqtt.example.com:1883
clientId=
#username=myuser
#password=mypassword
topic=informix/%database/%table
qosLevel=0 # QoS defined here (0, 1, or 2)
debug=false
```

## Notes and Limitations

1. A new MQTT connection is created for every published message.
2. Only top-level JSON fields can be used as topic placeholders.
3. Missing placeholder values are not validated and remain unchanged in the topic.

## Building and installing from source code

### Requirements

- Java 17+
- GNU Make

### Build

```bash
make build
```
### Install

```bash
make install
```

This script copies main jar file "MqttPublish.jar" to $INFORMIXDIR/extend/krakatoa. The required libraries are copied to $INFORMIXDIR/libjars

### Configure Informix Krakatoa

Change the JVPCLASSPATH parameter on $INFORMIXDIR/etc/$ONCONFIG to include the required libraries in the CLASSPATH:

```txt
JVPCLASSPATH $INFORMIXDIR/extend/krakatoa/krakatoa.jar:$INFORMIXDIR/libjars/json-20210307.jar:$INFORMIXDIR/libjars/org.eclipse.paho.client.mqttv3-1.2.5.jar
```
### Configure mqtt.properties file

Copy the mqtt.properties file to $INFORMIXDIR/etc/mqtt.properties
Edit mqtt.properties file and set up the proper parameters to connect to MQTT broker

### Install jar and deploy procedures

```sql
DATABASE xxx;
EXECUTE PROCEDURE sqlj.install_jar('file:$INFORMIXDIR/extend/krakatoa/PublishMQTT.jar', 'Publish2Mqtt', 1);
```

### Test environment

To test if jar libraries are properly installed, you can connect to the database where the jar has been installed and using dbaccess, execute the publish procedure directly:

```sql
execute procedure j_json2mqtt('{"operation":"insert", "table":"state", "owner":"informix", "database":"stores_demo", "txnid":12124695617764, "commit_time":1621529490, "rowdata":{"code":"53", "sname":"ES"}}');
execute procedure j_json2mqtt('{"operation":"update", "table":"state", "owner":"informix", "database":"stores_demo", "txnid":12124695625976, "commit_time":1621529490, "rowdata":{"code":"53", "sname":"UK"}, "before_rowdata":{"code":"53", "sname":"ES"}}');
execute procedure j_json2mqtt('{"operation":"delete", "table":"state", "owner":"informix", "database":"stores_demo", "txnid":12124695638244, "commit_time":1621529490, "rowdata":{"code":"53", "sname":"UK"}}');
```

## Define loopback replication

This readme is not intended to explain in detail what loopback replication is or how to define it properly. This are just notes to show some examples of commands and configuration steps.

If you are not already using ER, you will need two new dbspaces – with your preferred names – to hold the ER “syscdr” database and smart blobs which contain transaction contents, as was done for this article in the Docker container as follows:

```bash
eval cd $(dirname $(onstat -c ROOTPATH))
(umask 006 ; touch cdrdbs.000 cdrsbs.000)

onspaces -c -d cdrdbs -p $(pwd)/cdrdbs.000 -o 0 -s 204800
onspaces -c -S cdrsbs -p $(pwd)/cdrsbs.000 -o 0 -s 512000 -Df "AVG_LO_SIZE=2,LOGGING=ON"
```

Corresponding configuration parameters then need updating, together with two others as usually recommended:

```bash
cdr change config "CDR_DBSPACE cdrdbs"
cdr change config "CDR_QDATA_SBSPACE cdrsbs"
cdr change config "CDR_QUEUEMEM 262144"
cdr change config "CDR_SUPPRESS_ATSRISWARN 1,2,3"
```

If CDR_DBSPACE is set, the default for the above is in fact that and not “rootdbs” as stated. Specifically, setting CDR_QHDR_DBSPACE specifies the dbspace for the following “syscdr” tables (verified by testing):

```
blkdelete
control_send_stxn
trg_receive_stxn
trg_send_srep
trg_send_stxn
```

The next prerequisite step is to add a loopback connector (red) and ER group information (blue) in the “sqlhosts” file as described here and in the following example:
```
g_informix    group       -             -       i=1
informix      onsoctcp    *localhost    9088    g=g_informix

g_loopback    group       -             -       i=2
loopback      onsoctcp    *localhost    9090    g=g_loopback
```


Each asterisk instructs IDS to listen on all IP addresses available when started, which is not an ER requirement, but is often useful.

The $ONCONFIG file should be updated with the additional alias, for example:

```
DBSERVERNAME informix
DBSERVERALIASES loopback
```

A new listener thread can then be started and tested with:

```bash
onmode -P start loopback
echo | INFORMIXSERVER=loopback dbaccess sysmaster
```


ER can now be initiated with:

```
cdr define server -I g_informix
cdr define server -I g_loopback -S g_informix
```


## Create test environment

Create the test database and table

```sql

CREATE DATABASE IF NOT EXISTS testmqtt WITH LOG;

EXECUTE PROCEDURE sqlj.install_jar ('file:$INFORMIXDIR/extend/krakatoa/PublishMQTT.jar', 'Publish2Mqtt', 1);

CREATE TABLE IF NOT EXISTS state(
  code INTEGER PRIMARY KEY,
  sname VARCHAR(40)
);

```

Define the ER replication pushing to MQTT broker. Shell commands to define and start the replicate are:

```bash
cdr define replicate repl_state -C always -S row -A -R \
    --jsonsplname=j_json2mqtt \
    "P testmqtt@g_informix:informix.state" "select * from informix.state" \
    "R testmqtt@g_loopback:informix.state" "select * from informix.state"

cdr start replicate repl_state
```

See our Loopback Replication article for a description of the standard options chosen above.

SQL for a basic test is:

```bash
INSERT INTO state VALUES (53, 'ES');
UPDATE state SET sname = 'UK' WHERE code = 53;
DELETE FROM state WHERE code = 53;
```

## Manual procedure creation

PublishMqtt.jar library is configured to automaticaly register the j_Json2Mqtt procedure in the database when required. If this autopublish method doesn't works, you can register the procedure manualy
by executing this SQL statements in your database:

```sql
DROP PROCEDURE IF EXISTS j_json2mqtt;
CREATE PROCEDURE j_json2mqtt(lvarchar)
  external name 'Publish2Mqtt.json2mqtt(java.lang.String)'
  language java;

grant execute on procedure j_json2mqtt(lvarchar) to public;
```
