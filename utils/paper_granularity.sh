source utils/library.sh

ant jar
#CONFIGNAME=par_aggregated	test par_fft			run fft.FFT 16777216
#CONFIGNAME=par_aggregated	test par_fft8			run fft.FFT 8388608
#CONFIGNAME=par_aggregated	test par_nbody			run nbody.NBody 10 25000
#CONFIGNAME=par_aggregated	test par_blackscholes 	run blackscholes.BlackScholes 1000
#CONFIGNAME=par_aggregated	test par_integrate 	run -Xss208m integrate.Integrate 14 1700
CONFIGNAME=par_aggregated	test par_pi				run pi.Pi 1500000000
CONFIGNAME=par_aggregated	test par_health			run health.Health 6
CONFIGNAME=par_aggregated	test par_fib			run fib.Fib 51
CONFIGNAME=par_aggregated	test par_mergesort		run mergesort.MergeSort 251658240

PARALLELIZE=all ant jar
#CONFIGNAME=par_all 	test par_fft			run fft.FFT 16777216
#CONFIGNAME=par_all	test par_fft8			run fft.FFT 8388608
#CONFIGNAME=par_all		test par_nbody			run nbody.NBody 10 25000
#CONFIGNAME=par_all		test par_blackscholes 	run blackscholes.BlackScholes 1000
#CONFIGNAME=par_all	test par_integrate 	run -Xss208m integrate.Integrate 14 1700
CONFIGNAME=par_all	test par_pi				run pi.Pi 1500000000
CONFIGNAME=par_all	test par_health			run health.Health 6
CONFIGNAME=par_all	test par_fib			run fib.Fib 51
CONFIGNAME=par_all	test par_mergesort		run mergesort.MergeSort 251658240

#MEMORYMODEL=1 PARALLELIZE=all ant jar
#CONFIGNAME=par_memory_all 	test par_fft			run fft.FFT 16777216

#MEMORYMODEL=1 ant jar
#CONFIGNAME=par_memory_aggregated 	test par_fft			run fft.FFT 16777216


PARALLELIZE=auto ant jar

#CONFIGNAME=par_auto	test par_fft8			run fft.FFT 8388608
#CONFIGNAME=par_auto	test par_nbody			run nbody.NBody 10 25000
#CONFIGNAME=par_auto	test par_blackscholes 	run blackscholes.BlackScholes 1000
#CONFIGNAME=par_auto	test par_integrate 	run -Xss208m integrate.Integrate 14 1700
CONFIGNAME=par_auto	test par_pi				run pi.Pi 1500000000
CONFIGNAME=par_auto	test par_health			run health.Health 6
CONFIGNAME=par_auto	test par_fib			run fib.Fib 51
CONFIGNAME=par_auto	test par_mergesort		run mergesort.MergeSort 251658240