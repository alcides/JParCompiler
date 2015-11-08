source utils/library.sh

ant jar
#CONFIGNAME=par_aggregated	test par_fft			run fft.FFT 16777216
#CONFIGNAME=par_aggregated	test par_nbody			run nbody.NBody 10 25000
CONFIGNAME=par_aggregated	test par_blackscholes 	run blackscholes.BlackScholes 1000000

PARALLELIZE=all ant jar
#CONFIGNAME=par_all 	test par_fft			run fft.FFT 16777216
#CONFIGNAME=par_all		test par_nbody			run nbody.NBody 10 25000
CONFIGNAME=par_aggregated	test par_blackscholes 	run blackscholes.BlackScholes 1000000

#MEMORYMODEL=1 PARALLELIZE=all ant jar
#CONFIGNAME=par_memory_all 	test par_fft			run fft.FFT 16777216

#MEMORYMODEL=1 ant jar
#CONFIGNAME=par_memory_aggregated 	test par_fft			run fft.FFT 16777216