package hps.nyu.fa14.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import hps.nyu.fa14.BinCounter;
import hps.nyu.fa14.BinRefiner;
import hps.nyu.fa14.CosineScorer;
import hps.nyu.fa14.ISolutionFinder;
import hps.nyu.fa14.ISolutionViewer;
import hps.nyu.fa14.OpSample;
import hps.nyu.fa14.OpSolution;
import hps.nyu.fa14.SampleNeighborhood;
import hps.nyu.fa14.SampleSet;

public class OpticalMapSolver implements ISolutionFinder {

  private final int BIN_COUNT = 1000;
  private final int COLLAPSE_BIN_COUNT = 15;
  private final int TARGET_BIN_COUNT = 40;

  private final ISolutionViewer viewer;

  public OpticalMapSolver(ISolutionViewer viewer) {
    this.viewer = viewer;
  }

  @Override
  public OpSolution generateSolution(SampleSet set) {
    OpSolution solution0 = OpSolution.trivial(set);
    viewer.update(solution0);

    // Bin the whole data set
    BinCounter counter = new BinCounter(OpSolution.trivial(set));

    // Choose the top some percent of the bins and create a small target
    int[] topBins = BinCounter.getPercentTopBins(counter.count(BIN_COUNT),
        .005, COLLAPSE_BIN_COUNT);
    OpSample newBinned = BinCounter.newSampleFromBins(BIN_COUNT, topBins);
    System.out.println("size of bins " + newBinned.size());
    System.out.println("BINS: ");
    Iterator<Double> it = newBinned.iterator();
    while(it.hasNext()) {
      System.out.print(it.next() + " ");
    }
    System.out.println("");

    // Refine the solution based on finding the samples that are most
    // similar to the small target
    BinRefiner refiner = new BinRefiner(newBinned);
    solution0 = refiner.genSolution(set); // mostly eliminates noise, we hope

    // Iterate by counting the bins for only the samples that remain
    // included
    OpSolution solution = solution0;
    viewer.update(solution);

    int iterations = 10;
    for (int j = 0; j < iterations; j++) {
      counter = new BinCounter(solution);
      int targetCount = (int) (j + 1) * TARGET_BIN_COUNT / iterations;
      topBins = BinCounter.getTopBins(counter.count(BIN_COUNT),
          targetCount, COLLAPSE_BIN_COUNT);
      newBinned = BinCounter.newSampleFromBins(BIN_COUNT, topBins);
      
      /*it = newBinned.iterator();
      while(it.hasNext()) {
        System.out.print(it.next() + " ");
      }
      System.out.println("");*/
      
      OpSolution nextSolution = new OpSolution(set);
      nextSolution.ideal = newBinned;

      // and record if flipped
      List<RankedOpSample> rankedSamples = new ArrayList<RankedOpSample>(
          set.size());
      for (int i = 0; i < set.size(); i++) {
        OpSample s = set.get(i);
        s.flip(false);
        //System.out.println("cuts "+s.size()+" "+newBinned.size());
        double diff = s.cosine(newBinned);
        s.flip(true);
        double flipDiff = s.cosine(newBinned);
        RankedOpSample ranked = new RankedOpSample();
        ranked.sample = s;
        ranked.sampleIndex = i;
        if (diff < flipDiff) {
          ranked.diff = flipDiff;
          ranked.flipped = true;
        } else {
          ranked.diff = diff;
        }
        /*if (diff <= flipDiff) {
          ranked.diff = diff;
        } else {
          ranked.diff = flipDiff;
          ranked.flipped = true;
        }*/
        rankedSamples.add(ranked);
      }

      // sort them by similarity (descending)
      Collections.sort(rankedSamples, RankedOpSample.RANK_BY_DIFF);
      //Collections.reverse(rankedSamples);
      //System.out.println(rankedSamples.get(0).diff + " " + rankedSamples.get(1).diff);

      // Calculate the second derivative
      List<Double> diffs = new ArrayList<Double>();
      for (RankedOpSample s : rankedSamples) {
        diffs.add(s.diff);
      }
      List<Double> rankDt = dt(dt(diffs));
      // Find the maximum/minimum and set the cut off
      int cutoff = maxIndex(rankDt);
      System.out.println("Cut off "+cutoff);

      // choose the top x percent, then mark the others garbage
      for (int i = 0; i < set.size(); i++) {
        nextSolution.isTarget[i] = false;
      }
      for (int i = 0; i < set.size(); i++) {
        RankedOpSample s = rankedSamples.get(i);
        if (i < cutoff) {
          nextSolution.isTarget[s.sampleIndex] = true;
          nextSolution.isFlipped[s.sampleIndex] = s.flipped;
        }
      }

      nextSolution.ideal = newBinned;

      viewer.update(nextSolution);
      // See how much the solution changed
      // Compare solution / nextSolution
      solution = nextSolution;
    }

    
<<<<<<< HEAD
    // Implement Local search here to find the best scoring ideal target
    /*OpSolution bestSolution = solution;
    OpSample bestTarget = bestSolution.ideal;
    CosineScorer scorer = new CosineScorer();
    double nDist = 0.1;
    double gain = 1.0;
    int gIter = 0;
    while(gain > 0.0){
    	gain = 0.0;
    	gIter++;
    	bestSolution.ideal = bestTarget;
    	double best = scorer.score(bestSolution);
    	SampleNeighborhood neighborhood = new SampleNeighborhood(bestTarget);
//    	double bestNeighbor = 0.0;
    	for(OpSample t : neighborhood.genNeighbors(nDist)){
    		bestSolution.ideal = t;
    		double nBest = scorer.score(bestSolution);
    		if(nBest > best){
    			gain = nBest - best;
    			best = nBest;
    			bestTarget = t;
    		}
    	}
    	System.out.println("Best: " + best + " gain: " + gain);
    }
    System.out.println("Optimize over " + gIter + " iterations");
    bestSolution.ideal = bestTarget;*/
    
    OpSolution bestSolution = solution;
    OpSample bestTarget = bestSolution.ideal;
    CosineScorer scorer = new CosineScorer();
    double score = scorer.score(bestSolution);
    double best = score;
    double nDist = 0.01;
    double gain = 1.0;
    int gIter = 0;
    while(gain > 0.0){
      gain = 0.0;
      gIter++;
      solution.ideal = bestTarget;
      best = scorer.score(solution);
      SampleNeighborhood neighborhood = new SampleNeighborhood(bestTarget);
      for(OpSample t : neighborhood.genNeighbors(nDist)){
        solution.ideal = t;
        score = scorer.score(solution);
        if(score > best){
          gain = score - best;
          best = score;
          bestTarget = t;
        }
      }
      System.out.println("Best: " + best + " gain: " + gain);
    }
    System.out.println("Optimize over " + gIter + " iterations");
    bestSolution.ideal = bestTarget;
=======
    solution = localSearchSolution(solution);
    viewer.update(solution);
>>>>>>> 2a96418d655ebb3dc250dbaab5518dfe1e773b96
    
    return solution;
  }
  
	// Implement Local search here to find the best scoring ideal target
	private OpSolution localSearchSolution(OpSolution guess) {

		// TODO: Should probably clone the solution here
		OpSolution bestSolution = guess;
		OpSample bestTarget = bestSolution.ideal;
		CosineScorer scorer = new CosineScorer();
		double nDist = 0.1;
		double gain = 1.0;
		int gIter = 0;
		while (gain > 0.0) {
			gain = 0.0;
			gIter++;
			bestSolution.ideal = bestTarget;
			double best = scorer.score(bestSolution);
			SampleNeighborhood neighborhood = new SampleNeighborhood(bestTarget);
			// double bestNeighbor = 0.0;
			for (OpSample t : neighborhood.genNeighbors(nDist)) {
				bestSolution.ideal = t;
				double nBest = scorer.score(bestSolution);
				if (nBest > best) {
					gain = nBest - best;
					best = nBest;
					bestTarget = t;
				}
			}
			System.out.println("Best: " + best + " gain: " + gain);
		}
		System.out.println("Optimize over " + gIter + " iterations");
		bestSolution.ideal = bestTarget;
		return bestSolution;
	}

  private static List<Double> dt(List<Double> points) {
    int window = 1;
    List<Double> derivatives = new ArrayList<Double>();
    for (int i = 0; i < points.size(); i++) {
      if (i != (points.size() - 1)) { // Make sure to return a vector of the same length
        double d = (points.get(i+window) - points.get(i));
        derivatives.add(d);
      }
      else {
        double d = (points.get(i) - points.get(i - window));
        derivatives.add(d);
      }
    }
    return derivatives;
  }

  private static int maxIndex(List<Double> points) {
    double max = points.get(0);
    int maxIndex = 0;
    for (int i = 0; i < points.size(); i++) {
      if (points.get(i) > max) {
        maxIndex = i;
      }
    }
    return maxIndex;
  }

  private static class RankedOpSample {
    public double diff;
    public boolean flipped;
    public OpSample sample;
    public int sampleIndex;

    public static Comparator<RankedOpSample> RANK_BY_DIFF = new Comparator<RankedOpSample>() {

      @Override
      public int compare(RankedOpSample o1, RankedOpSample o2) {
        return (int) Math.signum(o1.diff - o2.diff);
      }
    };
  }

}
