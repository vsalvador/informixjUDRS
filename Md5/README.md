# Hash Digest calculate



una vez compilado, que puede hacerse en cualquier carpeta. Eliminar el fuente .java y la carpeta build. 

## Registrar jar  en la BD

DATABASE <nom_db>;

EXECUTE PROCEDURE sqlj.install_jar(
    'file:/home/informix/extend/krakatoa/ifxhash.jar',
    'ifxhash_jar',
    0
);

## Registro de las funciónes

```sql
DATABASE <nom_db>;

CREATE FUNCTION digest(LVARCHAR, LVARCHAR) RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.digest(java.lang.String, java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION md5(LVARCHAR) RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.md5(java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION sha256nex(LVARCHAR) RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.sha256Hex(java.lang.String)'
LANGUAGE JAVA;

CREATE FUNCTION sha512nex(LVARCHAR) RETURNING LVARCHAR
WITH (NOT VARIANT, CLASS = 'jvp')
EXTERNAL NAME 'ifxhash_jar:com.deister.udr.IfxHash.sha5212ex(java.lang.String)'
LANGUAGE JAVA;

```


Ejemplos de uso/validación

```sql
SELECT sha256_hex("46137536L");
```

Example output:

```txt
(constant)  a424ea130b49f88e8a29f0348852befd739a2c9a8fd66fb80ef6a2ff6d78d6d4
```

