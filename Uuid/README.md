# UUID Generation

This extension provides RFC-compliant UUID generation directly from Informix SQL.

## Usage

Generate a UUID value using:

```sql
SELECT uuid_generate();
```

Example result:

```text
550e8400-e29b-41d4-a716-446655440000
```

---

# Requirements

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

---

# Native Informix UUID Support (Informix 14+)

Recent Informix releases provide a built-in UUID function.

To verify native UUID generation, execute:

```sql
SELECT UUID() FROM sysmaster:sysdual;
```

Example result:

```text
550e8400-e29b-41d4-a716-446655440000
```

For environments running Informix 14.x and later, the built-in implementation may be sufficient for most use cases.

If you want to install the UUID procedure in your own database, declare the function in your database:

```sql
CREATE FUNCTION UUID() RETURNING CHAR(36) EXTERNAL NAME 'com.informix.judrs.IfxStrings.getUUID()' language java;
GRANT EXECUTE ON UUID TO PUBLIC;

SELECT UUID();
```

---

# Java-Based UUID Implementation

This project provides an alternative Java-based implementation that can be deployed through the Informix Krakatoa framework.

The Java implementation may be useful when:

- Consistent UUID generation behavior is required across Informix versions.
- Additional UUID-related functionality is planned.
- Existing Java extension deployment practices are already in use within the environment.

Refer to the installation instructions to deploy the extension and register the SQL function.

Compile it:
```bash
javac --release 11 UUIDGenerator.java
```

Create jar file:
```bash
jar cf UUIDGenerator.jar UUIDGenerator.class
```

SQL file:
```sql
-- install
execute procedure sqlj.install_jar ("file://home/informix/profiles/uuid/UUIDGenerator.jar" , "UUIDGenerator_jar");

-- register function
create function generate_uuid() returning  CHAR(36) external name "UUIDGenerator_jar:UUIDGenerator.generateUUID" language JAVA;

-- use it
SELECT generate_uuid() FROM sysmaster:sysdual;
```

Result:
```txt
(expression)  7bdcfcfb-1a9f-4e13-98db-49d2c5932143
```

