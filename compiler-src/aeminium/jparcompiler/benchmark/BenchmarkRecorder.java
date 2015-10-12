package aeminium.jparcompiler.benchmark;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import aeminium.runtime.Body;
import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.implementations.Factory;

public class BenchmarkRecorder implements Runnable{
	
	HashMap<String, Long> p = new HashMap<>();
	
	public void run() {
		
		int N = 10000;
		double a = 0;
		long st = System.nanoTime();
		for (int i=0; i<N; i++) {
			// Do Nothing
		}
		long t = System.nanoTime() - st;
		long op = (t/(N*2));
		
		save("access", 1);
		save("arrayaccess", 1);
		save("op", op);
		
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.sin(i);
		}
		t = System.nanoTime() - st;
		long sin = t - (op*2);
		save("java.lang.Math#sin(double)", sin);
		save("java.lang.Math#cos(double)", sin);
		save("java.lang.Math#tan(double)", sin);
		save("java.lang.Math#sinh(double)", sin);
		save("java.lang.Math#cosh(double)", sin);
		save("java.lang.Math#hypot(double, double)", sin);
		save("java.lang.Math#atan2(double, double)", sin);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.log(i);
		}
		t = System.nanoTime() - st;
		long log = t - (op*2);
		save("java.lang.Math#log(double)", log);
	
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.exp(i);
		}
		t = System.nanoTime() - st;
		long exp = t - (op*2);
		save("java.lang.Math#exp(double)", exp);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.sqrt(i);
		}
		t = System.nanoTime() - st;
		long sqrt = t - (op*2);
		save("java.lang.Math#sqrt(double)", sqrt);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.ceil(i);
		}
		t = System.nanoTime() - st;
		long ceil = t - (op*2);
		save("java.lang.Math#ceil(double)", ceil);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.pow(i, 10);
		}
		t = System.nanoTime() - st;
		long pow = t - (op*2);
		save("java.lang.Math#pow(double, double)", pow);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = Math.abs(i);
		}
		t = System.nanoTime() - st;
		long abs = t - (op*2);
		save("java.lang.Math#abs(double)", abs);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = ThreadLocalRandom.current().nextDouble();
		}
		t = System.nanoTime() - st;
		long rand = t - (op*2);
		save("java.util.Random#nextDouble()", rand);
		save("java.util.concurrent.ThreadLocalRandom#nextDouble()", rand);
		save("java.lang.Math#random()", rand);
		save("java.util.concurrent.ThreadLocalRandom#nextInt()", rand);
		
		st = System.nanoTime();
		ArrayList<Integer> n = new ArrayList<>();
		for (int i=0; i<N; i++) {
			n.add(i);
		}
		t = System.nanoTime() - st;
		long alAdd = t - (op*2);
		save("java.util.List#add(E)", alAdd);

		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = n.get(i);
		}
		t = System.nanoTime() - st;
		long get = t - (op*2);
		save("java.util.List#get(int)", get);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			a = n.size();
		}
		t = System.nanoTime() - st;
		long size = t - (op*2);
		save("java.util.List#size()", size);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			n.remove(N-i-1);
		}
		t = System.nanoTime() - st;
		long alRem = t - (op*2);
		save("java.util.List#remove(java.lang.Object)", alRem);
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			//System.out.println(i + "");
		}
		t = System.nanoTime() - st;
		long print = 183465751; //t - (op*2);
		save("java.io.PrintStream#println(java.lang.String)", print);
		
		
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			if (i % 2 == 0) a++;
		}
		t = System.nanoTime() - st;
		long ifop = t - (op*4);
		save("if", ifop);
		
		
		Runtime ae = Factory.getRuntime();
		ae.init();
		st = System.nanoTime();
		for (int i=0; i<N; i++) {
			Task task = ae.createBlockingTask(new Body() {

				@Override
				public void execute(Runtime rt, Task current) throws Exception {
					
				}
				
			}, Runtime.NO_HINTS);
			ae.schedule(task, Runtime.NO_PARENT, Runtime.NO_DEPS);
			task.getResult();
		}
		t = System.nanoTime() - st;
		ae.shutdown();
		long parallel = t - (op*4);
		save("parallel", parallel);
		save("recursion", parallel+1);
		
		System.out.println(a);
	}
	
	public void save(String name, long value) {
		System.out.println(name + ": " + value);
		p.put(name, value);
	}
	
	public void saveFile() {
		try {
			FileOutputStream fos = new FileOutputStream("benchmark.data");
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(p);
		    oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		BenchmarkRecorder rec = new BenchmarkRecorder();		
		rec.run();
		rec.saveFile();
	}
}
