/*You have create a user with name "communication" in MySQL, run this script as root*/
/*apart from the auto_increment primary key should be set to unsigned, which will not be accessed by jdbc,all the other data type should not be since there is not type in java
and something may went wrong for data*/
/*there is no data type: "long" in mysql, use bigint instead*/
\W
create database serverDB;

use serverDB;
/* get all the publicKey of the contacts of person_id 100001
select person_id,publicKey
from users as u
where exists(select null from relations where person1=100001 and person_2=u.person_id)*/


create table users(
	person_id int auto_increment not null primary key,
	/*just an alias, nickname of the user account*/
	alias varchar(100) character set UTF8MB4 not null,
	email varchar(100) character set ascii not null,
	/*hashed password*/
	hash blob not null,
	user_icon blob,
	unique index(email)
);
alter table users auto_increment=100001;

create table bannedUsers(
	person_id int not null primary key
);

create table relations(
	/*when you add a pair of relations, you should add two idems in table relations, personA personB and personB personA*/
	person1 int not null,
	person2 int not null,
	/*person2_alias is set by person1*/
	person2_alias varchar(100) character set UTF8MB4 not null default("Unknown"),

	/*The last record read by person2, generated by person1*/
	last_received_msg_id int not null default(0),
	/*the publicKey of person2*/
	/*publicKey tinyblob,*/
	
	unique index (person1,person2),
	/*when person2 login, it needs to check all the last_send of others in table relations*/
	/*index(person2),*/
	
	foreign key (person1) references users(person_id) on delete cascade,
	foreign key (person2) references users(person_id) on delete cascade
);
/*store all the messeage that has not been read by client*/
create table message(
	sent_by int not null,
	sent_to int not null,
	/*set by "sent_by", record_id is just equal to the number of messages sent by "sent_by" person, the record_id will +1 for every new message send by "sent_by" in its client*/
	message_id int not null,
	/*encrypted message from peer or file info(also encrypted by the peer privateKey)*/
	/*0 is a message, 1 is a file, then the message blob will be:
	peerSentTime(long) + filenameLength(int) + filenameBytes
	the file will be stored in /{$workspace}/resources/receivedTmpFiles/filename(format string: send_to + sent_by + record_id)*/
	isFile tinyint not null,
	message blob not null,	
	/*create_time is signed by server, not client, the time in client may be different, you can not use client time in server*/
	/*index (sent_to,sent_by,record_id DESC),*/
	unique index (sent_to,sent_by,message_id),
	foreign key (sent_by) references users(person_id) on delete cascade,
	foreign key (sent_to) references users(person_id) on delete cascade
);

/*when server has done dealing with the request, just delete it*/
create table contact_adding_request( /*一方提出添加联系人请求后要得到另一方的同意才行，这个表保存这种请求，在用户登陆时搜索这个表，向用户展示新增的请求*/
	sent_by int not null,
	sent_to int not null,
	/*alias and info is set by "sent_by" person*/
	/*alias will be inserted to relations table, alias is set by sent_by, the alias of "sent_to" person for "sent_by" person*/
	alias varchar(100) character set UTF8MB4 not null, 
	/*the information is shown to "sent_to" person which explaining the request from "sent_by" person*/
	info varchar(100) character set UTF8MB4 not null,
	
	unique index (sent_to,sent_by),
	foreign key (sent_by) references users(person_id) on delete cascade,
	foreign key (sent_to) references users(person_id) on delete cascade
);

grant select,insert,update,delete
on serverDB.*
to 'server'@'localhost';

/*DELIMITER $$
DROP PROCEDURE IF EXISTS login $$
CREATE PROCEDURE login(in person_id_ int unsigned)
BEGIN
	
END $$
DELIMITER ;
*/

\w