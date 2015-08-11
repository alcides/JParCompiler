source utils/execute.sh

compile_seq

test seq_blackscholes 	runseq blackscholes.BlackScholes 100000
test seq_fft			runseq fft.FFT $[16*1024*1024]
test seq_fib			runseq fib.Fib 51
test seq_health			runseq health.Health 6
test seq_integrate		runseq integrate.Integrate 14 1700
test seq_mergesort		runseq mergesort.MergeSort $[4194304*60]
test seq_nbody			runseq nbody.NBody 10 25000
test seq_pi				runseq pi.Pi 1500000000