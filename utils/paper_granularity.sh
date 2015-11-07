source utils/library.sh

#ant jar
#CONFIGNAME=par_aggregated	test par_fft			run fft.FFT 16777216

#PARALLELIZE=all ant jar
#CONFIGNAME=par_all 	test par_fft			run fft.FFT 16777216

MEMORYMODEL=1 PARALLELIZE=all ant jar
CONFIGNAME=par_memory 	test par_fft			run fft.FFT 16777216