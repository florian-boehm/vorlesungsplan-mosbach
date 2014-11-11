<?php

class Helper {
	public static function printAll() {
		if(isset($_GET['help']) || isset($_POST['help'])) {
			echo "<h1>Vorlesungsplan-Notifier API</h1>";
			echo "<hr>";
			self::printRegister();
			echo "<hr>";
			self::printUnregister();
			echo "<hr>";
			exit(0);
		}
	}

	public static function printRegister() {
		$header	= array("Pfad","Parametername","Notwendigkeit","Beschreibung");
		$data 	= array(	array("/register.php?","regId","ja","Die eindeutige GCM ID des Ger&auml;tes von Google"),
							array("","course","ja","Der gew&uuml;nschte Kurs, f&uuml;r den das Ger&auml;t registriert wird"));

		echo "<h2>Ger&auml;t f&uuml;r die Push-Benachrichtigungen registrieren und updaten</h2>";
		echo "<table border=1><thead>";
		foreach($header as $entry) {
			echo "<td>$entry</td>";
		}
	
		foreach($data as $row) {
			echo "<tr>";

			foreach($row as $entry) {
				echo "<td>$entry</td>";
			}

			echo "</tr>";
		}

		echo "</table>";
	}

	public static function printUnregister() {
		$header	= array("Pfad","Parametername","Notwendigkeit","Beschreibung");
		$data 	= array(	array("/unregister.php?","regId","ja","Die eindeutige GCM ID des Ger&auml;tes von Google"));

		echo "<h2>Ger&auml;t von den Push-Benachrichtigungen abmelden</h2>";
		echo "<table border=1><thead>";
		foreach($header as $entry) {
			echo "<td>$entry</td>";
		}
	
		foreach($data as $row) {
			echo "<tr>";

			foreach($row as $entry) {
				echo "<td>$entry</td>";
			}

			echo "</tr>";
		}

		echo "</table>";
	}
}

?>
