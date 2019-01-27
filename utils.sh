#!/bin/bash

DBFOLDER="TURINGdb/"
RMI_PORT=12345
SERVER_PORT=55000

case $1 in
	cleandb)
		rm -rf $DBFOLDER/*
		;;
	cleanbuild)
		rm -rf dist/server/* dist/client/*
		;;
	compileclient)
		javac -Xlint:unchecked -d dist/client TuringGUI/src/*/*.java server/lib/*.java
		;;
	compileserver)
		javac -Xlint:unchecked -d dist/server server/TURINGServer.java
		;;
	compile)
		$0 compileclient && $0 compileserver
		;;
	createdocs)
		javadoc server server.lib -link https://docs.oracle.com/javase/8/docs/api/ -d docs
		;;
	cleandocs)
		rm -rf docs/*
		;;
	runserver)
		$0 compileserver && java -cp dist/server server.TURINGServer $RMI_PORT $SERVER_PORT
		;;
	runtest)
		$0 cleandb
		$0 compileserver && java -cp dist/server server.TURINGServer $RMI_PORT $SERVER_PORT "test"
		;;
	runclient)
		$0 compileclient && java -cp dist/client turinggui.InteractionWindow $RMI_PORT $SERVER_PORT
		;;
	cleanall)
		$0 cleandb
		$0 cleanbuild
		$0 cleandocs
		;;
esac
