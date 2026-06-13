//package com.deister.judr;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * MQTT publisher utility.
 *
 * This class loads MQTT connection parameters from
 * $INFORMIXDIR/etc/mqtt.conf and publishes a JSON payload
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

    /** Enables verbose logging when true */
    private static boolean debug;

    /** Default MQTT Quality of Service level */
    private static final int DEFAULT_QOS_LEVEL = 0;

    /**
     * Test entry point.
     *
     * Publishes a sample JSON message.
     */
    //public static void main(String[] args) {
    //    Json2Mqtt("{\"message\": \"Cadena de prueba ejecutada desde main()\"}");
    //}

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
    public static void Json2Mqtt(String jsonString) {

        debugLog("**** Start");

        // Load configuration from mqtt.conf
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

        debugLog("Creating MQTT client");

        try {

            /*
             * Create MQTT client instance.
             *
             * Parameters:
             *   brokerUrl -> MQTT broker endpoint
             *   clientId  -> MQTT client identifier
             */
            MqttClient sampleClient = new MqttClient(brokerUrl, clientId);

            /*
             * Configure MQTT connection.
             *
             * Clean session means that subscriptions and
             * session state are not preserved between
             * connections.
             */
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

            sampleClient.connect(connOpts);

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
            mqttMessage.setQos(DEFAULT_QOS_LEVEL);

            // Publish message to topic
            sampleClient.publish(fullTopic, mqttMessage);

            debugLog(
                "Message published to topic: "
                    + fullTopic
                    + " with QoS: "
                    + DEFAULT_QOS_LEVEL
            );

            // Disconnect from broker
            sampleClient.disconnect();

            debugLog("Disconnected");

        } catch (Exception e) {

            // Catch any MQTT-related exception
            errorLog( "MQTT Exception: " + e.getMessage());
        }
    }

    /**
     * Loads MQTT configuration from:
     *
     *   $INFORMIXDIR/etc/mqtt.conf
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

        String configFilePath = System.getenv("INFORMIXDIR") + "/etc/mqtt.conf";

        try (InputStream input = new FileInputStream(configFilePath)) {

            properties.load(input);

            brokerUrl = properties.getProperty("brokerUrl");
            clientId = properties.getProperty("clientId");
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            topic = properties.getProperty("topic");

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

                debugLog("Parsing as JSON: " + jsonMessage);

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
                errorLog( "Exception: " + e.getMessage());
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

            System.out.println(MqttPublisher.class.getSimpleName() + "." + caller.getMethodName() + ": " + message);
        }
    }
}
