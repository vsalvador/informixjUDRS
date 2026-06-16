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

## Requirements

### Informix Java (Krakatoa) Environment

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
CREATE FUNCTION getUUID() RETURNING CHAR(36) EXTERNAL NAME 'com.informix.judrs.IfxStrings.getUUID()' language java;
GRANT EXECUTE ON getUUID TO PUBLIC;

SELECT UUID();
```

or use the automatic deployment of all J/Foundation methods:

```sql
CREATE FUNCTION generateCreateFunctionStatements() RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.JFoundation.generateCreateFunctionStatements()' LANGUAGE JAVA;
CREATE FUNCTION generateCreateFunctionStatements(LVARCHAR) RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.JFoundation.generateCreateFunctionStatements()' LANGUAGE JAVA;
GRANT EXECUTE ON FUNCTION generateCreateFunctionStatements() TO PUBLIC;
GRANT EXECUTE ON FUNCTION generateCreateFunctionStatements(LVARCHAR) TO PUBLIC;


EXECUTE FUNCTION generateCreateFunctionStatements();
```

Output: 

```txt
-- com.informix.judrs.Explain
CREATE FUNCTION getExplain(LVARCHAR) RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.Explain.getExplain(java.lang.String)' LANGUAGE JAVA;
-- com.informix.judrs.IfxStrings
CREATE FUNCTION encodeBase64(BLOB) RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.IfxStrings.encodeBase64(java.sql.Blob)' LANGUAGE JAVA;
CREATE FUNCTION getUUID() RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.IfxStrings.getUUID()' LANGUAGE JAVA;
CREATE FUNCTION replaceAll(LVARCHAR, LVARCHAR, LVARCHAR) RETURNS LVARCHAR EXTERNAL NAME 'com.informix.judrs.IfxStrings.replaceAll( java.lang.String,java.lang.String,java.lang.String)' LANGUAGE JAVA;
```
---

# Java-Based UUID Implementation

This project provides an alternative Java-based implementation that can be deployed through the Informix Krakatoa framework.

The Java implementation may be useful when:

- Consistent UUID generation behavior is required across Informix versions.
- Additional UUID-related functionality is planned.
- Existing Java extension deployment practices are already in use within the environment.

## Compile sources
To compile source files and create jar deployment archive use the **build.sh** command. This command will compile sources, create jar file and deploy it to $INFORMIXDIR/extend/krakatoa folder.

## Deploy functions

By installing the jar file in your database, the deployment function will create the generateUUID function automatically.

SQL file:
```sql
-- install
execute procedure sqlj.install_jar ("file://home/informix/extend/krakatoa/UUIDGenerator.jar" , "UUIDGenerator_jar", 1);

-- register function - Done autonatically by previous deployment flag
--create function generateUUID() returning  CHAR(36) external name "UUIDGenerator_jar:UUIDGenerator.generateUUID" language JAVA;

-- use it
SELECT generateUUID() FROM sysmaster:sysdual;
```

Example result:

```txt
(expression)  7bdcfcfb-1a9f-4e13-98db-49d2c5932143
```

