#!/bin/bash

DBFOLDER="TURING_db/"

case $1 in
	cleandb)
		rm -rf $DBFOLDER/*
		;;
	cleanbuild)
		find server -name '*.class' -delete
		;;
	cleanall)
		$0 cleandb
		$0 cleanbuild
		;;
	compile)
		javac -Xlint:unchecked server/TURINGServer.java
		;;
	runserver)
		$0 compile
		java server.TURINGServer 12345
		;;
	runtest)
		$0 cleandb
		$0 compile
		java server.TURINGServer 12345 "test"
		;;
esac
