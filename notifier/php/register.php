<?php

// Import der Hilfsklassen
include_once('filedb.class.php');
include_once('helper.class.php');

$db = new FileDB('filedb',false);

// Pruefen, ob es sich um einen POST oder GET Request handelt
// Entsprechend werden die Variablen belegt
if($_SERVER['REQUEST_METHOD'] === 'POST') {
	$regId = $_POST['regId'];
	$course = $_POST['course'];
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
	$regId = $_GET['regId'];
	$course = $_GET['course'];
}

Helper::printAll();

// Pruefen, ob die zur Registrierung benoetigten Variablen gesetzt sind
if(isset($regId) && !empty($regId) && isset($course) && !empty($course)) {
	if(is_null($db->getTable('courses')->selectEntryByPK($course))) {
		echo "KNOWN_FAILURE|COURSE_DOES_NOT_EXIST";
		exit(0);
	}
	
	$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));

	if(!is_null($entry)) {
		$result = $entry->update(array('course'=>$course,'counter'=>1));
		echo ($result) ? "DEV_UPDATE_SUCCESSFULL" : "KNOWN_FAILURE|DB_ERROR_ON_UPDATE";
		exit(0);
	} else {
		$result = $db->getTable('devices')->insert(md5($regId),array('regId'=>$regId,'course'=>$course,'counter'=>1));
		echo ($result) ? "DEV_REGISTER_SUCCESSFULL" : "KNOWN_FAILURE|DB_ERROR_ON_CREATE";
		exit(0);
	}
} else {
	echo "KNOWN_FAILURE|PARAMETERS_MISSING";
	exit(0);
}

// Das Skript sollte diese Zeile eigentlich niemals erreichen
echo "UNKOWN_FAILURE";

?>
