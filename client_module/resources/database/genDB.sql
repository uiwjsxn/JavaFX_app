connect 'jdbc:derby:clientDB;create=true;dataEncryption=true;encryptionAlgorithm=Blowfish/CBC/NoPadding;bootPassword=jfioaJKLFaijq3oAJFIL839ojjqf398ujalkfJFAjld83207Q#@RJFIa8p3fai;user=client';

CALL SYSCS_UTIL.SYSCS_CREATE_USER( 'client', '893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$' );

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication','true');

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.propertiesOnly','true');

connect 'jdbc:derby:clientDB;shutdown=true';

connect 'jdbc:derby:clientDB;user=client;password=893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$;bootPassword=jfioaJKLFaijq3oAJFIL839ojjqf398ujalkfJFAjld83207Q#@RJFIa8p3fai';