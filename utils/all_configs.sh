source utils/library.sh

ATC=3 LEVEL=12 run_config atc3l12_nocache
LOADBASED=1 run_config loadbased_nocache
MAXLEVEL=12 run_config maxlevel12_nocache
MAXTASKS=3 run_config maxtasks3_nocache
MAXTASKSSS=3 SS=16 run_config maxtasks3ss16_nocache
MAXTASKINQ=3 run_config maxtaskinqueue3_nocache
STACKSIZE=16 run_config stacksize16_nocache
SURPLUS=3 run_config surplus3_nocache
SYSMON=70 MEM=70 run_config sysmon2-70-70_nocache
