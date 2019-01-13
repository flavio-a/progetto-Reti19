#!/bin/bash

DBFOLDER="TURING_db/"

case $1 in
	clean)
		rm -rf $DBFOLDER/*
		;;
	cleanall)
		$0 clean
		find server -name '*.class' -delete
		;;
	compile)
		javac server/TURINGServer.java
		;;
	runserver)
		$0 compile
		java server.TURINGServer 12345
		;;
esac
