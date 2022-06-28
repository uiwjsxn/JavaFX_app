/*one client allows only one user login*/
CREATE TABLE client_info(
	/*should contain only one row with id 1 in the table*/
	id int primary key,
	/*these will be set after the app started for the first time*/
	/*passwordKeyStore blob,*/
	/*after sucessfully login for the first time, userID will be set
	them for later login, you should check whether the userID or emailAddress is equal to the relevant info of the user logging in, 
	if not, login failed and show do not allow multiple users login*/
	userID int,
	/*length + userID string or emailAddress bytes saved by user choice + length + passwordBytes, if user choose to save password, these filed will be set*/
	userInfo blob,
	/*the public private key used to transfer messages with other contacts and encrypt the msgToServer with private key*/
	/*keyPair blob,*/
	profileIcon blob
);

INSERT INTO client_info(id) values(1);

CREATE TABLE contact_info(
	id int primary key,
	/*messages sent to the peer should be encrypted by pubKey of the peer, however, the file sent will not be encrypted by the pubKey*/
	/*pubKey blob NOT NULL,*/
	/*original nickname of the peer*/
	alias varchar(100) NOT NULL,
	/*nickname set by the client user default: same as alias*/
	aliasByUser varchar(100) NOT NULL,
	profileIcon blob,
	/*the message_id of the last message received from this peer*/
	last_rev_msg_id int default 0 NOT NULL,
	last_sent_msg_id int default 0 NOT NULL
);

/*received messages*/
CREATE TABLE message_from(
	id bigint primary key GENERATED ALWAYS AS IDENTITY(Start with 1, Increment by 1),
	peerID int NOT NULL,
	message_id int NOT NULL,
	/*format for common message:
	peerSentTime(long) + messageBytes*/
	message blob NOT NULL,
	isFile boolean NOT NULL,
	
	CONSTRAINT mffk foreign key(peerID) references contact_info(id) on delete cascade on update restrict
);
CREATE INDEX mfi ON message_from (peerID, message_id);

/*sent messages*/
CREATE TABLE message_to(
	id bigint primary key GENERATED ALWAYS AS IDENTITY(Start with 1, Increment by 1),
	peerID int NOT NULL,
	message_id int NOT NULL,
	/*encrypted message from peer or file info(also encrypted by the peer privateKey)*/
	/*0 is a message, 1 is a file, then the message blob will be:
	peerSentTime(long) + filenameLength(int) + filenameBytes
	the file will be stored in /{$workspace}/resources/receivedFiles/filename*/
	message blob NOT NULL,
	isFile boolean NOT NULL,
	
	CONSTRAINT mtfk foreign key(peerID) references contact_info(id) on delete cascade on update restrict
);
CREATE INDEX mti ON message_to (peerID, message_id);

CREATE TABLE contract_adding_request(
	sentTo int primary key NOT NULL,
	peerAlias varchar(100) NOT NULL,
	aliasByUser varchar(100) NOT NULL
);

DISCONNECT;