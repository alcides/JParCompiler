package mergesort;

import java.util.Random;

import aeminium.runtime.futures.codegen.Sequential;

/**
 * 4-way split version of merge sort
 */

public class MergeSort {

	public boolean validationTest;

	// Cutoff for when to do sequential versus parallel merges
	public static int MERGE_SIZE;
	// Cutoff for when to do sequential quicksort versus parallel mergesort
	public static int QUICK_SIZE;
	// Cutoff for when to use insertion-sort instead of quicksort
	public static int INSERTION_SIZE;
	public static int SERIALIZED_CUT_OFF;

	protected int[] input;
	protected int[] result;
	protected int size;

	public static void main(String[] args) {
		int problemSize = 4194304;

		MergeSort sort = new MergeSort();

		if (args.length > 0) {
			problemSize = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			sort.validationTest = true;
		}
		sort.run(problemSize);
	}

	public MergeSort() {
		validationTest = false;
		MERGE_SIZE = 2048;
		QUICK_SIZE = 2048;
		INSERTION_SIZE = 2000;
	}

	public void run(int size) {
		long startT = System.currentTimeMillis();
		initialize(size);
		System.out.println("init time=" + (System.currentTimeMillis() - startT));
		runWorkAndTest();
	}

	public void runWorkAndTest() {
		long startT = System.currentTimeMillis();
		int output[] = sort(input, 0, input.length);
		long endT = System.currentTimeMillis();
		System.out.println("running time=" + (endT - startT));
		checkSorted(output);
	}

	@Sequential
	protected void checkSorted(int[] array) {
		System.out.println("Verifying results- size of " + array.length);
		for (int i = 1; i < array.length; i++) {
			if (array[i - 1] > array[i]) {
				System.out.println("Validation Failed.");
				return;
			}
		}
		System.out.println("VALID");
	}

	public void initialize(int size) {
		this.size = size;

		input = new int[size];
		result = new int[size];

		Random random = new Random(100100);
		for (int i = 0; i < size; ++i) {
			int k = random.nextInt();
			if (k < 0) {
				k *= -1;
			}
			k = k % 10000;
			input[i] = k;
		}
	}

	public int[] sort(int A[], int low, int high) {

		int length = high - low;

		if (length <= QUICK_SIZE) {
			int[] R = new int[length];
			int max = R.length;
			int j = low;
			for (int i = 0; i < max; i++) {
				R[i] = A[j++];
			}
			quickSort(R, 0, R.length - 1);
			return R;
		} else {

			int q = length / 4;
			int idxs0 = q + low;
			int idxs1 = (2 * q) + low;
			int idxs2 = (3 * q) + low;

			int[] A_quarters0 = sort(A, low, idxs0);
			int[] A_quarters1 = sort(A, idxs0, idxs1);
			int[] A_quarters2 = sort(A, idxs1, idxs2);
			int[] A_quarters3 = sort(A, idxs2, idxs2 + q);

			int[] R = new int[high - low];

			merge(A_quarters0, A_quarters1, A_quarters2, A_quarters3, R);
			return R;
		}

	}

	@Sequential
	protected int findSplit(int value, int B[]) {
		int low = 0;
		int high = B.length;
		while (low < high) {
			int middle = low + ((high - low) >>> 1);
			if (value <= B[middle])
				high = middle;
			else
				low = middle + 1;
		}
		return high;
	}

	/** A standard sequential quicksort **/
	@Sequential
	protected void quickSort(int array[], int lo, int hi) {
		// int lo = 0;
		// int hi = array.length - 1;
		// If under threshold, use insertion sort
		int[] arr = array;
		if (hi - lo + 1l <= INSERTION_SIZE) {
			for (int i = lo + 1; i <= hi; i++) {
				int t = arr[i];
				int j = i - 1;
				while (j >= lo && arr[j] > t) {
					arr[j + 1] = arr[j];
					--j;
				}
				arr[j + 1] = t;
			}
			return;
		}

		// Use median-of-three(lo, mid, hi) to pick a partition.
		// Also swap them into relative order while we are at it.

		int mid = (lo + hi) >>> 1;

		if (arr[lo] > arr[mid]) {
			int t = arr[lo];
			arr[lo] = arr[mid];
			arr[lo] = arr[mid];
			arr[mid] = t;
		}
		if (arr[mid] > arr[hi]) {
			int t = arr[mid];
			arr[mid] = arr[hi];
			arr[hi] = t;

			if (arr[lo] > arr[mid]) {
				t = arr[lo];
				arr[lo] = arr[mid];
				arr[mid] = t;
			}

		}

		int left = lo + 1; // start one past lo since already handled lo
		int right = hi - 1; // similarly

		int partition = arr[mid];

		while (true) {

			while (arr[right] > partition)
				--right;

			while (left < right && arr[left] <= partition)
				++left;

			if (left < right) {
				int t = arr[left];
				arr[left] = arr[right];
				arr[right] = t;
				--right;
			} else
				break;

		}

		quickSort(arr, lo, left);
		quickSort(arr, left + 1, hi);

	}


	@Sequential
	public static void merge(int[] a1, int[] a2, int[] a3, int[] a4, int[] a) {
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;
		int i4 = 0;
		int alength = a.length;
		int v1 = a1[0];
		int v2 = a2[0];
		int v3 = a3[0];
		int v4 = a4[0];
		int v1m = a1.length;
		int v2m = a2.length;
		int v3m = a3.length;
		int v4m = a4.length;
		for (int i = 0; i < alength; i++) {
			if (v1 < v2) {
				if (v1 < v3) {
					if (v1 < v4) {
						a[i] = v1;
						// v1 smallest
						if (i1 < v1m) {
							v1 = a1[i1++];
						} else {
							v1 = 2147483647;
						}
					} else {
						a[i] = v4;
						// v4 smalles
						if (i4 < v4m) {
							v4 = a4[i4++];
						} else {
							v4 = 2147483647;
						}
					}
				} else {
					if (v3 < v4) {
						a[i] = v3;
						// v3 smallest
						if (i3 < v3m) {
							v3 = a3[i3++];
						} else {
							v3 = 2147483647;
						}
					} else {
						a[i] = v4;
						// v4 smallest
						if (i4 < v4m) {
							v4 = a4[i4++];
						} else {
							v4 = 2147483647;
						}
					}
				}
			} else {
				if (v2 < v3) {
					if (v2 < v4) {
						a[i] = v2;
						// v2 smallest
						if (i2 < v2m) {
							v2 = a2[i2++];
						} else {
							v2 = 2147483647;
						}
					} else {
						a[i] = v4;
						// v4 smallest
						if (i4 < v4m) {
							v4 = a4[i4++];
						} else {
							v4 = 2147483647;
						}
					}
				} else {
					if (v3 < v4) {
						a[i] = v3;
						// v3 smallest
						if (i3 < v3m) {
							v3 = a3[i3++];
						} else {
							v3 = 2147483647;
						}
					} else {
						a[i] = v4;
						// v4 smallest
						if (i4 < v4m) {
							v4 = a4[i4++];
						} else {
							v4 = 2147483647;
						}
					}
				}
			}
		}
	}

}
