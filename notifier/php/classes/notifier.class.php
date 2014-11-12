<?php ob_end_clean();

include_once './classes/filedb.class.php';
include_once './classes/helper.class.php';

class Notifier {
	
	public function determineTask() {		
		switch (strtolower($this->getParam('method'))) {
			case "register": $this->register(); break;
			case "unregister": $this->unregister(); break;
			case "ack":	$this->ack(); break;
			case "status": $this->status(); break;
			case "hash": $this->hash(); break;
			case "":
			default:
				Helper::printAll();
		}
	}
	
	private function getParam($pname) {
		if($_SERVER['REQUEST_METHOD'] === 'GET') {
			return $_GET[$pname];
		} else if($_SERVER['REQUEST_METHOD'] === 'POST') {
			return $_POST[$pname];
		}
		
		return "";
	}
	
	private function register() {
		$db = new FileDB('filedb',false);
		$regId = $this->getParam('regId');
		$course = $this->getParam('course');
		
		// Pruefen, ob die zur Registrierung benoetigten Variablen gesetzt sind
		if(isset($regId) && !empty($regId) && isset($course) && !empty($course)) {
			if(is_null($db->getTable('courses')->selectEntryByPK($course))) {
				$this->warning("COURSE_DOES_NOT_EXIST");
			}
		
			$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));
		
			if(!is_null($entry)) {
				$result = $entry->update(array('course'=>$course,'counter'=>1));
				$this->depends($result, "DEV_UPDATED_SUCCESSFULLY", "DB_ERROR_ON_UPDATE", true);
			} else {
				$result = $db->getTable('devices')->insert(md5($regId),array('regId'=>$regId,'course'=>$course,'counter'=>1));
				$this->depends($result, "DEV_REGISTERED_SUCCESSFULLY", "DB_ERROR_ON_CREATE", true);
			}
		} else {
			$this->warning("PARAMETERS_MISSING");
		}
		
		// Das Skript sollte diese Zeile eigentlich niemals erreichen
		$this->error("UNKOWN_PROBLEM");
	}
	
	private function ack() {
		$db = new FileDB('filedb',false);
		$regId = $this->getParam('regId');
		
		// Pruefen, ob die zur Statusausgabe benoetigten Variablen gesetzt sind
		if(isset($regId) && !empty($regId)) {
			$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));
		
			if(!is_null($entry)) {
				$result = $entry->update(array('counter'=>0));
				$this->depends($result, "UNREACHABLE_COUNT_RESET", "DB_ERROR_ON_UPDATE", true);
			} else {
				$this->warning("DEV_NOT_REGISTERED");
			}
		} else {
			$this->warning("PARAMETERS_MISSING");
		}

		// Das Skript sollte diese Zeile eigentlich niemals erreichen
		$this->error("UNKOWN_PROBLEM");
	}
	
	public function hash() {
		$db = new FileDB('filedb',false);
		$course = $this->getParam('course');
		$hash = $this->getParam('hash');
		
		// Pruefen, ob die zur Statusausgabe benoetigten Variablen gesetzt sind
		if(isset($course) && !empty($course) && isset($hash) && !empty($hash)) {
			$entry = $db->getTable('courses')->selectEntryByPK($course);
		
			if(!is_null($entry)) {
				$this->depends(($entry->get('hash') == $hash), "HASH_CORRECT", "HASH_INCORRECT");
			} else {
				$this->warning("COURSE_NOT_FOUND");
			}
		} else {
			$this->warning("PARAMETERS_MISSING");
		}

		// Das Skript sollte diese Zeile eigentlich niemals erreichen
		$this->error("UNKOWN_PROBLEM");
	}
	
	public function unregister() {
		$db = new FileDB('filedb',false);
		$regId = $this->getParam('regId');
		
		// Pruefen, ob die zur Loeschung benoetigten Variablen gesetzt sind
		if(isset($regId) && !empty($regId)) {
			$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));
		
			if(!is_null($entry)) {
				$result = $entry->remove();
				$this->depends(result, "DEV_UNREGISTERED_SUCCESSFULLY", "DB_ERROR_ON_REMOVE", true);
			} else {
				$this->warning("DEV_NOT_REGISTERED");
			}
		} else {
			$this->warning("PARAMETERS_MISSING");
		}

		// Das Skript sollte diese Zeile eigentlich niemals erreichen
		$this->error("UNKOWN_PROBLEM");
	}
	
	public function status() {
		$db = new FileDB('filedb',false);
		$regId = $this->getParam('regId');
		
		// Pruefen, ob die zur Statusausgabe benoetigten Variablen gesetzt sind
		if(isset($regId) && !empty($regId)) {
			$entry = $db->getTable('devices')->selectEntryByPK(md5($regId));
		
			if(!is_null($entry)) {
				$result = $entry->update(array('counter'=>1));
				$this->depends($result,"DEV_REGISTERED|".$entry->get('course'), "DB_ERROR_ON_UPDATE", true);
			} else {
				$this->warning("DEV_NOT_REGISTERED");
			}
		} else {
			$this->warning("PARAMETERS_MISSING");
		}

		// Das Skript sollte diese Zeile eigentlich niemals erreichen
		$this->error("UNKOWN_PROBLEM");
	}
	
	private function error($msg) {
		$this->printMsg("ERROR|".$msg);
	}
	
	private function warning($msg) {
		$this->printMsg("WARNING|".$msg);
	}
	
	private function ok($msg) {
		$this->printMsg("OK|".$msg);
	}
	
	private function depends($bool,$trueMsg,$falseMsg,$error = false) {
		if($bool) {
			$this->ok($trueMsg);
		} else {
			if($error) {
				$this->error($falseMsg);
			} else {
				$this->warning($falseMsg);
			}
		}
	}
	
	private function printMsg($msg) {
		ob_start();
		echo $msg;
		ob_end_flush();
		exit(0);
	}
}

?>