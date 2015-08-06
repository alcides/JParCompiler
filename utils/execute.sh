#!/bin/sh

function compile() {
	ant jar
}


function run() {
	java -Xmx24G -cp dist/AeminiumRuntime.jar:dist/AeminiumFutures.jar:dist/JparCompilerExamples.jar $@
}
