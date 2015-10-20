source utils/library.sh

function tests {
	#test par_blackscholes 	run blackscholes.BlackScholes 10000000
	#test par_fft			run fft.FFT $[16*1024*1024]
	#test par_fib			run fib.Fib 51
	#test par_health			run health.Health 6
	#test par_integrate		run integrate.Integrate 14 1700
	#test par_mergesort		run mergesort.MergeSort $[4194304*60]
	#test par_nbody			run nbody.NBody 10 25000
	test par_pi				run pi.Pi 1500000000
}

ant jar
CONFIGNAME=par_aggregated tests


PARALLELIZE=all
ant jar
CONFIGNAME=par_all tests