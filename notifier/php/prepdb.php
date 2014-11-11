<?php

// Import der Hilfsklassen
include_once('filedb.class.php');
include_once('helper.class.php');

$db = new FileDB('filedb',false);

$db->getTable('courses')->clean();
$db->getTable('courses')->insert('INF11',array("hash"=>md5("wie")));
$db->getTable('courses')->insert('INF12',array("hash"=>md5("geht")));
$db->getTable('courses')->insert('INF13',array("hash"=>md5("es")));
$db->getTable('courses')->insert('INF14',array("hash"=>md5("dir")));

?>
