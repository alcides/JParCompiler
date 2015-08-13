#!/bin/sh

function compile() {
	ant fetchruntime
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

function make_config() {
	cp utils/aeminiumrt.config .
	
	if [ -z "$BINARYSPLIT" ]; then
		$BINARYSPLIT="false"
	fi
	echo "Binary split: $BINARYSPLIT"
	echo "ForHelper.BinarySplitting = $BINARYSPLIT" >> aeminiumrt.config
	
	if [ -z "$PPS" ]; then
		$PPS="false"
	fi
	echo "PPS: $PPS"
	echo "ForTask.LazyBinarySplittingPPS = $PPS" >> aeminiumrt.config
	
	if [ -z "$CACHE" ]; then
		$CACHE="0"
	fi
	echo "Cache: $CACHE"
	echo "ImplicitWorkStealingRuntime.parallelizeCacheSize = $CACHE" >> aeminiumrt.config
	
	if [ -z "TIMER" ]; then
		$USETIMER="false"
		$TIMER="0"
	fi
	echo "Timer: $TIMER"
	echo "ImplicitWorkStealingRuntime.parallelizeUseTimer = $USETIMER" >> aeminiumrt.config
	echo "ImplicitWorkStealingRuntime.parallelizeUpdateTimer = $TIMER" >> aeminiumrt.config
	
	
	if [ -n "$ATC" ]; then
		echo "MaxTasks with SS: $ATC with $LEVEL"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.ATC" >> aeminiumrt.config
		echo "ATC.maxTotalTasksPerCoreThreshold = $ATC" >> aeminiumrt.config
		echo "ATC.maxLevelThreshold = $LEVEL" >> aeminiumrt.config
	fi
	
	if [ -n "$MAXTASKS" ]; then
		echo "MaxTasks: $MAXTASKS"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.MaxTasks" >> aeminiumrt.config
		echo "MaxTasks.maxTotalTasksPerCoreThreshold = $MAXTASKS" >> aeminiumrt.config
	fi
	
	if [ -n "$MAXLEVEL" ]; then
		echo "MaxLevel: $MAXLEVEL"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.MaxLevel" >> aeminiumrt.config
		echo "MaxLevel.maxLevelThreshold = $MAXLEVEL" >> aeminiumrt.config
	fi
	
	if [ -n "$LOADBASED" ]; then
		echo "LoadBased"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.LoadBased" >> aeminiumrt.config
	fi
	
	if [ -n "$MAXTASKSSS" ]; then
		echo "MaxTasks with SS: $MAXTASKSSS with $SS"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.MaxTasksWithStackSize" >> aeminiumrt.config
		echo "MaxTasksWithStackSize.maxTotalTasksPerCoreThreshold = $MAXTASKSSS" >> aeminiumrt.config
		echo "MaxTasksWithStackSize.maxStackSize = $SS" >> aeminiumrt.config
	fi
	
	if [ -n "$MAXTASKSINQ" ]; then
		echo "MaxTasksInQueue: $MAXTASKSINQ"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.MaxTasksInQueue" >> aeminiumrt.config
		echo "MaxTasksInQueue.maxTotalTasksInQueueThreshold = $MAXTASKSINQ" >> aeminiumrt.config
	fi
	
	if [ -n "$STACKSIZE" ]; then
		echo "StackSize: $STACKSIZE"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.StackSize" >> aeminiumrt.config
		echo "StackSize.maxStackSize = $SS" >> aeminiumrt.config
	fi
	
	if [ -n "$SURPLUS" ]; then
		echo "Surplus: $SURPLUS"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.Surplus" >> aeminiumrt.config
		echo "Surplus.surplusThreshold = $SURPLUS" >> aeminiumrt.config
	fi
	
	if [ -n "$SYSMON" ]; then
		echo "Sysmon: $SYSMON"
		echo "DeciderFactory.implementation = aeminium.runtime.implementations.implicitworkstealing.decider.SysMon2" >> aeminiumrt.config
		echo "SysMon2.parallelizeThreshold = $SYSMON" >> aeminiumrt.config
		echo "SysMon2.memoryThreshold = $MEM" >> aeminiumrt.config
	fi
}

function run_config() {	
	CONFIGNAME=$1 bash utils/benchmark.sh
}