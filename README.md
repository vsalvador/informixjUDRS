# Informix Java UDR Collection

A collection of IBM Informix Java User Defined Routines (UDRs), triggers, and utility functions designed to extend database functionality and enable integration with external systems such as MQTT brokers and event-driven architectures.

## Overview

This project contains reusable Java-based UDRs for IBM Informix, including:

- MQTT integration for database event publishing
- Post-commit trigger framework
- UUID generation functions
- MD5 hashing functions
- JSON utilities
- Database helper libraries

These UDRs enable Informix databases to participate in modern event-driven and microservice architectures while maintaining transactional integrity.

These UDRs require enablement of Krakatoa Java Environment in Informix server.

## Informix Java (Krakatoa) Environment

Beginning with Informix 14.10, the embedded Java runtime is no longer distributed with the database server. Informix now relies on a standard Java Development Kit (JDK) installed on the operating system.

Ensure that Java 11 or Java 17 is installed and accessible to the Informix server.

Verify that `$INFORMIXDIR/extend/krakatoa/jre` points to a valid JDK installation:

```bash
ls -l $INFORMIXDIR/extend/krakatoa/jre

$INFORMIXDIR/extend/krakatoa/jre/bin/java -version
```

Example output:

```text
openjdk version "17.0.x"
```

## Configure the Java Virtual Processor

Update the following parameters in `$INFORMIXDIR/etc/$ONCONFIG`:

```text
VPCLASS         jvp,num=1

JVPJAVAHOME     $INFORMIXDIR/extend/krakatoa/jre
JVPJAVAVM       jvm
JVPJAVALIB      /lib/server

JVPPROPFILE     $INFORMIXDIR/extend/krakatoa/.jvpprops
JVPCLASSPATH    $INFORMIXDIR/extend/krakatoa/krakatoa.jar
```

### Notes

Although the `ONCONFIG` documentation describes `JVPJAVAVM` as:

> Path relative to `JVPJAVAHOME` that points to the JVM shared library.

In current Informix releases, the value:

```text
JVPJAVAVM jvm
```

is the recommended and commonly used configuration when `JVPJAVAHOME` and `JVPJAVALIB` are correctly defined.

## Configure the JVP Properties File

Ensure that the file specified by `JVPPROPFILE` exists.

If it is not present, create it from the template supplied with Informix:

```bash
cp $INFORMIXDIR/extend/krakatoa/.jvpprops.template \
   $INFORMIXDIR/extend/krakatoa/.jvpprops
```

Review and customize the file as required for your environment.

## Restart and Verify the Java Virtual Processor

After updating the configuration, restart the Informix instance:

```bash
onmode -ky
oninit
```

Verify that a Java Virtual Processor (JVP) has been started:

```bash
onstat -g sch | grep jvp
```

Example output:

```text
jvp      1      running
```

