<?php

// Import der Hilfsklassen
include_once('filedb.class.php');
include_once('helper.class.php');

$db = new FileDB('filedb',false);

// Pruefen, ob es sich um einen POST oder GET Request handelt
// Entsprechend werden die Variablen belegt
if($_SERVER['REQUEST_METHOD'] === 'POST') {
	$course = $_POST['course'];
	$hash = $_POST['hash'];
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
	$course = $_GET['course'];
	$hash = $_GET['hash'];
}

Helper::printAll();

// Pruefen, ob die zur Statusausgabe benoetigten Variablen gesetzt sind
if(isset($course) && !empty($course) && isset($hash) && !empty($hash)) {
	$entry = $db->getTable('courses')->selectEntryByPK($course);

	if(!is_null($entry)) {
		echo ($entry->get('hash') == $hash) ? "TRUE" : "FALSE";
		exit(0);
	} else {
		echo "KNOWN_FAILURE|COURSE_NOT_FOUND";
		exit(0);
	}
} else {
	echo "KNOWN_FAILURE|PARAMETERS_MISSING";
	exit(0);
}

// Das Skript sollte diese Zeile eigentlich niemals erreichen
echo "UNKOWN_FAILURE";

?>
