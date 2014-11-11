<?php

// Import der Hilfsklassen
include_once('filedb.class.php');
include_once('helper.class.php');

$db = new FileDB('filedb',false);

// Pruefen, ob es sich um einen POST oder GET Request handelt
// Entsprechend werden die Variablen belegt
if($_SERVER['REQUEST_METHOD'] === 'POST') {
	$regId = $_POST['regId'];
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
	$regId = $_GET['regId'];
}

Helper::printAll();

// Pruefen, ob die zur Statusausgabe benoetigten Variablen gesetzt sind
if(isset($regId) && !empty($regId)) {
	$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));

	if(!is_null($entry)) {
		$result = $entry->update(array('counter'=>1));
		echo ($result) ? "DEV_REGISTERED|".$entry->get('course') : "KNOWN_FAILURE|DB_ERROR_ON_REMOVE";
		exit(0);
	} else {
		echo "DEV_NOT_REGISTERED";
		exit(0);
	}
} else {
	echo "KNOWN_FAILURE|PARAMETERS_MISSING";
	exit(0);
}

// Das Skript sollte diese Zeile eigentlich niemals erreichen
echo "UNKOWN_FAILURE";

?>
