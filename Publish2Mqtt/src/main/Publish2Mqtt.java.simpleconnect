//package com.deister.judr;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

/**
 * MQTT publisher utility.
 *
 * This class loads MQTT connection parameters from
 * $INFORMIXDIR/etc/mqtt.properties and publishes a JSON payload
 * to an MQTT topic.
 *
 * Topics can contain placeholders prefixed with '%'
 * that are dynamically replaced with values from the JSON
 * message before publishing.
 *
 * Example:
 *
 * Topic template:
 *     devices/%device/events
 *
 * JSON payload:
 *     {"device":"sensor01","value":25}
 *
 * Generated topic:
 *     devices/sensor01/events
 */
public class Publish2Mqtt {

    /** MQTT broker URL (e.g. tcp://host:1883) */
    private static String brokerUrl;

    /** MQTT client identifier */
    private static String clientId;

    /** MQTT username */
    private static String username;

    /** MQTT password */
    private static String password;

    /** Topic template loaded from configuration */
    private static String topic;

    /** MQTT Quality of Service level*/
    private static int qosLevel;

    /** Enables verbose logging when true */
    private static boolean debug;

    /**
     * Publishes a JSON payload to the configured MQTT broker.
     *
     * Processing flow:
     *  1. Load configuration.
     *  2. Determine clientId.
     *  3. Connect to broker.
     *  4. Build topic from JSON values.
     *  5. Publish message.
     *  6. Disconnect.
     *
     * @param jsonString JSON message to publish.
     */
    public static void json2mqtt(String jsonString) {

        debugLog("**** Start");

        // Load configuration from mqtt.properties
        loadConfiguration();

        debugLog("Configuration loaded");

        /*
         * If no clientId has been configured,
         * use the local hostname.
         */
        try {
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = InetAddress.getLocalHost().getHostName();
            }
        } catch (UnknownHostException e) {

            // Fallback value if hostname cannot be determined
            debugLog( "Could not retrieve hostname, using a default clientId.", true);

            clientId = "defaultClientId";
        }


        try {

            /*
             * Create MQTT client instance.
             *
             * Parameters:
             *   brokerUrl -> MQTT broker endpoint
             *   clientId  -> MQTT client identifier
             */
            debugLog("Creating MQTT URL: " +  brokerUrl + " client:" + clientId + " User dir: " + System.getProperty("user.dir"));
            MqttClient client = new MqttClient(brokerUrl,
                                               clientId,
                                               new MemoryPersistence()
                                               //new MqttDefaultFilePersistence("/tmp/mqtt")
                                              );

            /*
             * Configure MQTT connection.
             *
             * Clean session means that subscriptions and
             * session state are not preserved between
             * connections.
             */
            debugLog("Configuring connection options");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            /*
             * Configure authentication if password is defined.
             */
            if (password != null) {
                connOpts.setUserName(username);
                connOpts.setPassword(password.toCharArray());
            }

            // Connect to MQTT broker
            debugLog("Connecting to broker: " + brokerUrl);

            client.connect(connOpts);

            debugLog("Connected");

            /*
             * Build final MQTT topic.
             *
             * Example:
             *   topic template = devices/%device
             *   JSON           = {"device":"sensor01"}
             *
             * Result:
             *   devices/sensor01
             */
            String fullTopic = constructFullTopic(topic, jsonString);

            /*
             * Create MQTT message using the original
             * JSON payload.
             */
            MqttMessage mqttMessage = new MqttMessage(jsonString.getBytes());

            // Apply configured QoS level
            mqttMessage.setQos(qosLevel);

            debugLog( "Publishing topic: " + fullTopic + " with QoS: " + qosLevel);
            debugLog( "Payload: " + jsonString);

            // Publish message to topic
            client.publish(fullTopic, mqttMessage);

            // Disconnect from broker
            client.disconnect();

            debugLog("Disconnected");

        } catch (MqttException e) {
            errorLog("Reason code: " + e.getReasonCode());
            errorLog("Message: " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                errorLog(ste.toString());
            }
        } catch (Exception e) {
            // Catch any MQTT-related exception
            errorLog("Exception: " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                errorLog(ste.toString());
            }
        }
    }

    /**
     * Loads MQTT configuration from:
     *
     *   $INFORMIXDIR/etc/mqtt.properties
     *
     * Expected properties:
     *
     *   brokerUrl
     *   clientId
     *   username
     *   password
     *   topic
     *   debug
     */
    private static void loadConfiguration() {

        Properties properties = new Properties();

        String configFilePath = System.getenv("INFORMIXDIR") + "/etc/mqtt.properties";

        try (InputStream input = new FileInputStream(configFilePath)) {

            properties.load(input);

            brokerUrl = properties.getProperty("brokerUrl");
            clientId = properties.getProperty("clientId");
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            topic = properties.getProperty("topic");

            // Optional Quality of Service (default=0)
            qosLevel = Integer.parseInt(
                properties.getProperty("qosLevel", "0")
            );

            // Optional debug flag (default=false)
            debug = Boolean.parseBoolean(
                properties.getProperty("debug", "false")
            );

        } catch (IOException ex) {
            errorLog( "Error loading configuration file: " + ex.getMessage());
        }
    }

    /**
     * Builds the final MQTT topic by replacing
     * placeholders found in the topic template.
     *
     * Placeholders begin with '%'.
     *
     * Example:
     *
     * Topic:
     *     devices/%device/events
     *
     * JSON:
     *     {"device":"sensor01"}
     *
     * Result:
     *     devices/sensor01/events
     *
     * @param topic Topic template.
     * @param jsonMessage JSON payload.
     *
     * @return Fully resolved topic.
     */
    private static String constructFullTopic(
        String topic,
        String jsonMessage
    ) {

        String fullTopic = topic;

        // Only process replacements if placeholders exist
        if (topic.contains("%")) {

            try {
                // Parse JSON payload
                JSONObject jsonObject = new JSONObject(jsonMessage);

                /*
                 * Replace every placeholder found
                 * in the topic template.
                 */
                for (String key : jsonObject.keySet()) {

                    String value = String.valueOf(jsonObject.get(key));

                    fullTopic = fullTopic.replace( "%" + key, value);
                }

            } catch (Exception e) {
                errorLog("Exception: " + e.getMessage());
                errorLog("Parsing bad JSON: " + jsonMessage);
            }
        }

        return fullTopic;
    }

    /**
     * Prints debug messages only when the
     * debug configuration property is enabled.
     *
     * @param message Message to print.
     */
    private static void debugLog(String message) {
        debugLog(message, false);
    }
    private static void errorLog(String message) {
        debugLog(message, true);
    }
    private static void debugLog(String message, boolean force) {

        if (debug || force) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[3];

            System.out.print(Publish2Mqtt.class.getSimpleName() + "." + caller.getMethodName() + ": " + message);
        }
    }
}
