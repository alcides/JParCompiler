source utils/library.sh

bash utils/pull_deps.sh
ant fetchruntime
compile

function program_with_different_configs() {
	MAXTASKS=2 CONFIGNAME=maxtasks2_nocache test_config $@
	MAXTASKSSS=2 SS=16 CONFIGNAME=maxtasks2ss16_nocache test_config $@
	MAXTASKINQ=2 CONFIGNAME=maxtaskinqueue2_nocache test_config $@
	ATC=2 LEVEL=12 CONFIGNAME=atc2l12_nocache test_config $@
	LOADBASED=1 CONFIGNAME=loadbased_nocache test_config $@
	MAXLEVEL=12 CONFIGNAME=maxlevel12_nocache test_config $@
	STACKSIZE=16 CONFIGNAME=stacksize16_nocache test_config $@
	SURPLUS=3 CONFIGNAME=surplus3_nocache test_config $@
	SYSMON=70 MEM=70 CONFIGNAME=sysmon2-70-70_nocache test_config $@
	
	BINARYSPLIT="true" MAXTASKS=2 CONFIGNAME=maxtasks2_nocache test_config $@
	BINARYSPLIT="true" MAXTASKSSS=2 SS=16 CONFIGNAME=maxtasks2ss16_nocache test_config $@
	BINARYSPLIT="true" MAXTASKINQ=2 CONFIGNAME=maxtaskinqueue2_nocache test_config $@
	BINARYSPLIT="true" ATC=2 LEVEL=12 CONFIGNAME=atc2l12_nocache test_config $@
	BINARYSPLIT="true" LOADBASED=1 CONFIGNAME=loadbased_nocache test_config $@
	BINARYSPLIT="true" MAXLEVEL=12 CONFIGNAME=maxlevel12_nocache test_config $@
	BINARYSPLIT="true" STACKSIZE=16 CONFIGNAME=stacksize16_nocache test_config $@
	BINARYSPLIT="true" SURPLUS=3 CONFIGNAME=surplus3_nocache test_config $@
	BINARYSPLIT="true" SYSMON=70 MEM=70 CONFIGNAME=sysmon2-70-70_nocache test_config $@
}


program_with_different_configs par_blackscholes 	run blackscholes.BlackScholes 100000
program_with_different_configs par_fft				run fft.FFT $[16*1024*1024]
program_with_different_configs par_fib				run fib.Fib 51
program_with_different_configs par_health			run health.Health 6
program_with_different_configs par_integrate		run integrate.Integrate 14 1700
program_with_different_configs par_mergesort		run mergesort.MergeSort $[4194304*60]
program_with_different_configs par_nbody			run nbody.NBody 10 25000
program_with_different_configs par_pi				run pi.Pi 1500000000



