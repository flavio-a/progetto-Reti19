#!/bin/bash

DBFOLDER="TURING_db/"

case $1 in
	cleandb)
		rm -rf $DBFOLDER/*
		;;
	cleanbuild)
		find server -name '*.class' -delete
		;;
	compile)
		javac -Xlint:unchecked server/TURINGServer.java
		;;
	createdocs)
		javadoc server server.lib -link https://docs.oracle.com/javase/8/docs/api/ -d docs
		;;
	cleandocs)
		rm -rf docs/*
		;;
	runserver)
		$0 compile && java server.TURINGServer 12345 55000
		;;
	runtest)
		$0 cleandb
		$0 compile && java server.TURINGServer 12345 55000 "test"
		;;
	cleanall)
		$0 cleandb
		$0 cleanbuild
		$0 cleandocs
		;;
esac
