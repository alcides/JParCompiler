#!/bin/sh

function compile() {
	ant jar
}

function compile_seq() {
	ant jar-seq
}


function run() {
	timeout 5m java -Xmx24G -cp dist/AeminiumRuntime.jar:dist/AeminiumFutures.jar:dist/JparCompilerExamples.jar $@
}


function runseq() {
	timeout 5m java -Xmx24G -cp dist/AeminiumRuntime.jar:dist/AeminiumFutures.jar:dist/SequentialExamples.jar $@
}

function test() {
	mkdir -p results
	for i in {1..2}
	do
		echo "Running $1 - $CONFIGNAME"
		${*:2} >> results/$1_$CONFIGNAME.log
	done
}

function run_config() {
	CONFIGNAME=$1 CONFIG=$2 bash utils/benchmark.sh
}