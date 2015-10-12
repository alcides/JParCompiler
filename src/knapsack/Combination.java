package knapsack;

public class Combination {
	public int indices[];
	public int items;
	public int weight = 0;
	public int value = 0;
	
	public Combination(int n) {
		items = n;
		indices = new int[n];
	}

	public void apply(KObject[] items) {
		weight = 0;
		value = 0;
		for (int ind : indices) {
			weight += items[ind].weight;
			value += items[ind].value;
		}
	}
}
