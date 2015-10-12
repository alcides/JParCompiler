package knapsack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class KnapsackMO {

		public static void main(String[] args) {
			KObject[] possible = generateRandomPossibleObjects(20);
			long t = System.nanoTime();
			List<Combination> paretoFront = computePareto(possible);
			for (Combination p : paretoFront) {
				for (int i : p.indices) {
					System.out.print(i + ",");
				}
				System.out.println(".");
			}
			
			t = System.nanoTime() - t;
			System.out.println(t / 1000000000.0);
			
		}
		
		private static List<Combination> generateCombinations(KObject[] possible) {
			List<Combination> combinations = new ArrayList<Combination>();
			combinations.add(new Combination(0));
			for (int i=0; i<possible.length; i++) {
				Combination[] tmp = new Combination[combinations.size()];
				int k = 0;
				for (Combination c : combinations) {
					Combination d = new Combination(c.items+1);
					d.indices = Arrays.copyOf(c.indices, c.items + 1);
					d.indices[c.items] = i;
					d.apply(possible);
					tmp[k++] = d;
				}
				for (Combination t : tmp) {
					combinations.add(t);
				}
				
			}
			return combinations;
		}

		private static List<Combination> computePareto(KObject[] possible) {
			List<Combination> combinations = generateCombinations(possible);
			List<Combination> paretoFront = new ArrayList<Combination>(); 
			
			for (Combination c1 : combinations) {
				boolean dominated = false;
				for (Combination c2 : combinations) {
					if (c1 == c2) continue;
					if (c2.weight < c1.weight && c2.value > c1.value) {
						dominated = true;
						break;
					}
				}
				if (!dominated) paretoFront.add(c1);
			}
			return paretoFront;
		}

		private static KObject[] generateRandomPossibleObjects(int n) {
			KObject[] possible = new KObject[n];
			Random r = new Random(2L);
			for (int i=0; i<n; i++) {
				KObject o = new KObject();
				o.value = r.nextInt() % 1000;
				o.weight = r.nextInt() % 1000;
				possible[i] = o;
			}
			return possible;
		}
	
}
