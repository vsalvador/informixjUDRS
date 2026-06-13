# Hash Digest Functions

This extension provides hash digest functions for Informix based on the Java `MessageDigest` API. Supported algorithms include MD5, SHA-256, and SHA-512.

## Deployment

After compiling the project, copy the generated JAR file to the Krakatoa extension directory.

The source files (`.java`) and temporary build artifacts are not required at runtime and may be removed after deployment.

Example deployment location:

```text
$INFORMIXDIR/extend/krakatoa/ifxhash.jar
```

## Register the JAR in the Database

Connect to the target database and register the JAR file using the SQLJ package:

```sql
DATABASE <database_name>;

EXECUTE PROCEDURE sqlj.install_jar(
    'file:/home/informix/extend/krakatoa/ifxhash.jar',
    'ifxhash_jar',
    0
);
```

## Register the SQL Functions

Create the SQL wrapper functions that expose the Java methods to Informix.

```sql
DATABASE <database_name>;

CREATE FUNCTION digest(LVARCHAR, LVARCHAR)
RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.digest(java.lang.String, java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION md5(LVARCHAR)
RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.md5(java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION sha256_hex(LVARCHAR)
RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.sha256Hex(java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION sha512_hex(LVARCHAR)
RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.sha512Hex(java.lang.String)'
LANGUAGE JAVA;
```

## Usage Examples

### Generic Digest Function

The `digest()` function accepts the algorithm name and the input string.

```sql
SELECT digest('SHA-1', '46137536L') FROM sysmaster:sysdual;
```

### MD5

```sql
SELECT md5('46137536L') FROM sysmaster:sysdual;
```

### SHA-256

```sql
SELECT sha256_hex('46137536L') FROM sysmaster:sysdual;
```

Example output:

```text
a424ea130b49f88e8a29f0348852befd739a2c9a8fd66fb80ef6a2ff6d78d6d4
```

### SHA-512

```sql
SELECT sha512_hex('46137536L') FROM sysmaster:sysdual;
```

## Supported Algorithms

The generic `digest()` function supports any algorithm available through the Java Runtime Environment configured for Informix.

Typical algorithms available in Java 11 and Java 17 include:

* MD5
* SHA-1
* SHA-224
* SHA-256
* SHA-384
* SHA-512
* SHA-512/224
* SHA-512/256
* SHA3-224
* SHA3-256
* SHA3-384
* SHA3-512

Example:

```sql
SELECT digest('SHA3-256', 'Hello World') FROM sysmaster:sysdual;
```

## Uninstall

First you should drop the registered functions using the registered jar:

```sql
DROP FUNCTION IF EXISTS digest;
DROP FUNCTION IF EXISTS md5;
DROP FUNCTION IF EXISTS sha256_hex;
DROP FUNCTION IF EXISTS sha512_hex;
```

Then remove the JAR from the database:

```sql
EXECUTE PROCEDURE sqlj.remove_jar('ifxhash_jar');
```

To check if other functions have been registered using this jar file, you can execute this SQL statement:

```sql
select 'DROP FUNCTION ' || procname || ';' from sysprocedures where externalname matches 'ifxhash_jar*'

```
