package hps.nyu.fa14;

import hps.nyu.fa14.solver.CutClusterSolver;
import hps.nyu.fa14.solver.OpticalMapSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MapResolver implements Runnable {

	private final SampleSet set;

	public MapResolver(SampleSet set) {
		this.set = set;
	}

	public ISolutionViewer viewer;
	
	public OpSolution resolve(){
		if(viewer == null){
			viewer = new NullSolutionViewer();
		}
		// TODO: This needs to be implemented better
		// Instantiate whatever type of solver you want here
		// ISolutionFinder solver = new RandomSolver();
		ISolutionFinder solver = new OpticalMapSolver(viewer);
		
		// Generate and return an OpSolution
		OpSolution solution = solver.generateSolution(set);
		return solution;
	}
	
	public static void main(String[] args) throws NumberFormatException,
			FileNotFoundException, IOException {
		if (args.length != 3) {
			usage();
		}
		// first parameter is input
		String inputFile = args[0];
		String outputFile = args[1];
		String goldFile = args[2];

		SampleSet inputSet = SampleSet.parse(new FileInputStream(new File(
				inputFile)));

		MapResolver resolver = new MapResolver(inputSet);
		OpSolution solution = resolver.resolve();
		
		solution.write(new FileOutputStream(new File(outputFile)));
		SolutionEvaluator solutionEvaluator = new SolutionEvaluator(goldFile);
		System.out.println(solutionEvaluator.evaluateSolution(args[1]));
	}

	private static void usage() {
		// How to use it
		System.out.println("java -jar MapResolver <input> <output> <gold file>");
		System.exit(1);
	}

	@Override
	public void run() {
		resolve();
		
	}

}
