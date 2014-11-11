<?php

class FileDB {
	private $name;
	public static $echoing;
	
	function __construct($dbname,$echoing = false) {
		$this->name = $dbname;
		self::$echoing = $echoing;
	}

	public function getTable($tablename) {	
		if(!empty($tablename) && file_exists($this->name.'/'.$tablename)) {
			return new Table($this->name.'/'.$tablename);
		} else {
			print (FileDB::$echoing) ? "FileDB: Table '".$tablename."' does not exist in database '".$this->name."'" : "";
			return NULL;
		}
	}
	
	public function getName() {
		return $this->name;
	}
}

class Table {
	private $name;
	
	function __construct($tablename) {
		$this->name = $tablename;
	}
	
	public function selectEntryByPK($pk) {
		if(!empty($pk) && file_exists($this->name.'/'.$pk)) {
			return new Entry($this->name.'/'.$pk);
		} else {
			print (FileDB::$echoing) ? "FileDB: Entry '".$pk."' does not exist in table '".$this->name."'" : "";
			return NULL;
		}
	}
	
	public function insert($pk,$array = array()) {
		if(file_exists($this->name.'/'.$pk)) {
			print (FileDB::$echoing) ? "FileDB: Entry '".$pk."' already exists in table '".($this->name)."'" : "";
			return false;
		} else {
			$file = fopen($this->name.'/'.$pk, "w+");
			fwrite($file, json_encode($array));
			fflush($file);
			fclose($file);

			return true;
		}
	}

	public function clean() {
		if(file_exists($this->name)) {
			$dir = opendir($this->name);

			while($filename = readdir($dir))
			{
				if($filename != '.' && $filename != '..')
			  	{
			    	unlink($this->name.'/'.$filename);
			  	}
			}
		
			closedir($dir);
		} else {
			print (FileDB::$echoing) ? "FileDB: Table '".$tablename."' does not exist in database '".$this->name."'" : "";
			return false;
		}
	}
			
	public function getName() {
		return $this->name;
	}
}

class Entry {
	private $name;
	
	function __construct($pk) {
		$this->name = $pk;
	}
	
	public function remove() {
		if(file_exists($this->name)) {
			return unlink($this->name);
		} else {
			return false;
		}
	}
	
	public function update($fields) {
		if(!file_exists($this->name)) {
			return false;
		} else {
			$file = fopen($this->name, "r");

			if(flock($file,LOCK_EX)) {
				$data = json_decode(fread($file,filesize($this->name)));

				fclose($file);

				// Datei neu anlegen
				unlink($this->name);
				$file = fopen($this->name, "w+");

				foreach($fields as $key => $value) {
					$data->$key = $value;
				}
				
				fwrite($file, json_encode($data));
				fflush($file);
				flock($file, LOCK_UN);
				fclose($file);

				return true;
			} else {
				print (FileDB::$echoing) ? "FileDB: Could not get exclusive lock on '".($this->name)."'" : "";
				return false;
			}
		}
	}
	
	public function getName() {
		return $this->name;
	}

	public function get($fieldname) {
		$file = fopen($this->name, "r");

		if(flock($file,LOCK_EX)) {
        	$data = json_decode(fread($file,filesize($this->name)));
			flock($file, LOCK_UN);
			fclose($file);

			if(isset($data->$fieldname)) {
				return $data->$fieldname;
			} else {
				print (FileDB::$echoing) ? "FileDB: Field does not exist in '".($this->name)."'" : "";
				return false;
			}
		} else {
	        print (FileDB::$echoing) ? "FileDB: Could not get exclusive lock on '".($this->name)."'" : "";
    		return false;
		}
	}
}

?>
