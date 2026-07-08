## Logs Reader to publish to MQTT Broker

This tools reads the log files produced by IfxLogger utility and push each line as a message to a MQTT broker.

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

The application loads its configuration from the same configuration file used by IfxLogger tool: 

```text
$INFORMIXDIR/etc/ifxlogger.properties
```

### Configuration Parameters

| Property | Description |
|-----------|-------------|
| `log.directory` | Directory where files will be created |
| `log.basename`  | Log basename |

### Example Configuration

```properties
log.directory=/tmp
log.basename=IfxLog
```

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

This script copies main jar file "IfxLogger.jar" to $INFORMIXDIR and the dependencies jars to $INFORMIXDIR/lib

### Test

```bash
make test
```

### Configure ifxlogger.properties file

Copy the ifxlogger.properties file to $INFORMIXDIR/etc/ifxlogger.properties
Edit ifxlogger.properties file and set up the proper parameters to connect to defile log file path

### Install jar and dependencies

Copy build/dist/Logs2Mqtt.jar to your installation folder and copy dependencies to a lib subfolder.

```bash
INST_FOLDER=$HOME

cp build/dist/Logs2Mqtt.jar $INST_FOLDER
mkdir -p $INST_FOLDER/lib
cp lib/*.jar $INST_FOLDER/lib
```

### Run the program

To execute the program, you should have your main jar in a folder and all the dependencies in a child lib folder.

```bash
java -jar ./Logs2Mqtt.jar
```

