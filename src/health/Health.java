package health;

public class Health {
	public static int sim_level = 5; // 6
	public static int sim_cities = 12;
	public static int sim_population_ratio = 10;
	public static int sim_time = 365;
	public static int sim_assess_time = 2;
	public static int sim_convalescence_time = 12;
	public static int sim_seed = 23;
	public static double sim_get_sick_p = 0.002; 
	public static double sim_convalescence_p = 0.100;
	public static double sim_realloc_p = 0.150;
	public static long res_population = 7238720;
	public static long res_hospitals = 346201;
	public static long res_personnel = 723872;
	public static long res_checkin = 5267413;
	public static long res_village = 7121156;
	public static long res_waiting = 73923;
	public static long res_assess = 28969;
	public static long res_inside = 14672;
	public static double res_avg_stay = 5.230891;
	
	public static int sim_id;
	
	public static int IQ = 127773;
	
	public static void main(String[] args) {

		int size = Health.sim_time;
		if (args.length > 0) {
			size = Integer.parseInt(args[0]);
		}

		Village village = Health.allocate_village(Health.sim_level, 0, null);

		for (int i=0; i<size; i++) {
			sim_village(village);
		}
		
	}

	protected static void sim_village(Village village) {
		for (int i=0; i<village.children.size(); i++) {
			sim_village(village.children.get(i));
		}
		village.tick();
	}
	
	public static Village allocate_village(int level, int vid, Village back) {		
		int personnel = (int) Math.pow(2, level);
		int population = personnel * Health.sim_population_ratio;
		
		Village current;
			current = new Village();
		current.id = vid;
		current.level = level;
		current.seed = vid * (IQ + Health.sim_seed);
		current.root = back;
		for (int i=0; i<population; i++) {
			Patient p = new Patient();
			p.id = Health.sim_id++;
			p.seed = current.seed;
			p.home_village = current;
			current.population.add(p);
		}
		current.hosp.personnel = personnel;
		current.hosp.free_personnel = personnel;
		if (level > 1) {
			for (int i=sim_cities; i>0; i--) {
				Village curr = Health.allocate_village(level-1, i, current);
				current.children.add(curr);
			}
		}
		return current;
	}

}
