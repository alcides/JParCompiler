source utils/library.sh
compile

MAXTASKS=2 BINARYSPLIT=true make_config
CONFIGNAME=maxtasks2_nocache_binary

test par_blackscholes 	run blackscholes.BlackScholes 100000
test par_health			run health.Health 6
test par_nbody			run nbody.NBody 10 25000
test par_pi				run pi.Pi 1500000000

MAXTASKS=2 PPS=3 make_config
CONFIGNAME=maxtasks2_nocache

test par_blackscholes 	run blackscholes.BlackScholes 100000
test par_health			run health.Health 6
test par_nbody			run nbody.NBody 10 25000
test par_pi				run pi.Pi 1500000000

MAXTASKS=2 PPS=10 make_config
CONFIGNAME=maxtasks2_nocache_pps10

test par_blackscholes 	run blackscholes.BlackScholes 100000
test par_health			run health.Health 6
test par_nbody			run nbody.NBody 10 25000
test par_pi				run pi.Pi 1500000000