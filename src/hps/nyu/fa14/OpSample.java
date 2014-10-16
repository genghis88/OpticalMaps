package hps.nyu.fa14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OpSample implements Iterable<Double> {

	/**
	 * A sorted list of the cuts in one sample
	 */
	private final List<Double> cuts = new ArrayList<Double>();
	private final List<Double> cutsFlipped = new ArrayList<Double>();
	
	public OpSample(List<Double> cuts){
		for(Double c : cuts){
			this.cuts.add(c);
			this.cutsFlipped.add(1.0 - c);
		}
		Collections.sort(this.cuts);
		Collections.sort(this.cutsFlipped);
	}
	
	public int size(){
		return cuts.size();
	}
	
	/**
	 * Calculates the absolute differences between the closest points in this and the other
	 * In general, the difference is not symmetric
	 * It is possible to map multiple points in other to the same point in this
	 * @param other
	 * @return
	 */
	public double diff(OpSample other){
		// For each cut in the other, find the closest cut in this sample, and accumulate the
		// absolute value of difference in points
		double diff = 0.0;
		List<Double> otherPoints = new ArrayList<Double>();
		for(Double d : other){ // takes into account whether it is flipped
			otherPoints.add(d);
		}
		List<Double> thisCuts = (flipped ? cutsFlipped : cuts);
		int thisPos = 0;
		double currentCut = 0.0;
		double nextCut = 0.0;
		for(int i = 0; i < otherPoints.size(); i++){
			double cutToFind = otherPoints.get(i);
			while(nextCut <= cutToFind && thisPos < thisCuts.size()){
				currentCut = nextCut;
				nextCut = thisCuts.get(thisPos++);
			}
			double cutDiff = Math.min(Math.abs(cutToFind - currentCut), Math.abs(cutToFind - nextCut));
			diff += cutDiff;
		}
		return diff;
	}
	
	/**
	 * Calculates the absolute differences between the closest points in this
	 * and the other In general, the difference is not symmetric It is possible
	 * to map multiple points in other to the same point in this
	 * 
	 * @param other
	 * @return
	 */
	public double cosine(OpSample other) {
		// For each cut in the other, find the closest cut in this sample, and
		// accumulate the
		// absolute value of difference in points

		List<Double> otherPoints = new ArrayList<Double>();
		for (Double d : other) { // takes into account whether it is flipped
			otherPoints.add(d);
		}
		// Do this with paired cut points instead
		//List<Double> alignedCutPoints = getAlignedCutPoints(otherPoints);
		List<Double> alignedCutPoints = getPairedCutPoints(otherPoints);
		double cosine = dot(alignedCutPoints, otherPoints)
				/ (getVectorLength(alignedCutPoints) * getVectorLength(otherPoints));
		//sum of all min distances
		/*double cosine = 0.0;
		for(double d:alignedCutPoints) {
		  cosine += d;
		}*/
		return cosine;
	}

	private List<Double> getAlignedCutPoints(List<Double> otherPoints) {
		List<Double> alignedCutPoints = new ArrayList<Double>();
		List<Double> thisCuts = (flipped ? cutsFlipped : cuts);
		int thisPos = 0;
		double currentCut = 0.0;
		double nextCut = 0.0;
		for (int i = 0; i < otherPoints.size(); i++) {
			double cutToFind = otherPoints.get(i);
			while (nextCut <= cutToFind && thisPos < thisCuts.size()) {
				currentCut = nextCut;
				nextCut = thisCuts.get(thisPos++);
			}
			if (Math.abs(cutToFind - currentCut) < Math
					.abs(cutToFind - nextCut)) {
				alignedCutPoints.add(currentCut);
			} else {
				alignedCutPoints.add(nextCut);
			}
		}
		return alignedCutPoints;
	}

	//TODO: Can add partial digestion parameter here
	private List<Double> getPairedCutPoints(List<Double> otherPoints) {
		List<Double> pairedCutPoints = new ArrayList<Double>();
		List<Double> thisCuts = (flipped ? cutsFlipped : cuts);
		
		// Calculate all of the pairwise distances
		double[][] dist = new double[thisCuts.size()][otherPoints.size()];
		for(int i = 0; i < thisCuts.size(); i++){
			for(int j = 0; j < otherPoints.size(); j++){
				dist[i][j] = Math.abs(thisCuts.get(i) - otherPoints.get(j));
			}
		}
		
		Set<Integer> pairedThis = new HashSet<Integer>();
		Set<Integer> pairedOther = new HashSet<Integer>();
		for (int k = 0; k < otherPoints.size(); k++) {
			double minDist = Double.MAX_VALUE;
			int minI = -1;
			int minJ = -1;
			for (int i = 0; i < thisCuts.size(); i++) {
				for (int j = 0; j < otherPoints.size(); j++) {
					if ((!pairedThis.contains(i) || !pairedOther.contains(j))) {
						if (dist[i][j] < minDist) {
							minDist = dist[i][j];
							minI = i;
							minJ = j;
						}
					}
				}
			}
			pairedCutPoints.add(thisCuts.get(minI));
			pairedThis.add(minI);
			pairedOther.add(minJ);
		}
		//if we sort here, the cut points won't match right?
		Collections.sort(pairedCutPoints);
		
		return pairedCutPoints;
	}

	private static double dot(List<Double> list1, List<Double> list2) {
		double sum = 0;
		if (list1.size() != list2.size()) {
			throw new RuntimeException();
		}
		for (int i = 0; i < list1.size(); i++) {
			sum += list1.get(i) * list2.get(i);
		}
		return sum;
	}

	private static double getVectorLength(List<Double> list) {
		double sum = 0;
		for (double l : list) {
			sum += l * l;
		}
		return Math.sqrt(sum);
	}

	/**
	 * Keeps track of all of the differences and only uses some minimum portion of them
	 * to calculate the difference.  This allows partial digestion to have less of an effect on
	 * the results
	 * @param other
	 * @return
	 */
	public double partialDiff(OpSample other, double fraction){
		// For each cut in the other, find the closest cut in this sample, and accumulate the
		// absolute value of difference in points
		List<Double> otherPoints = new ArrayList<Double>();
		for(Double d : other){ // takes into account whether it is flipped
			otherPoints.add(d);
		}
		List<Double> thisCuts = (flipped ? cutsFlipped : cuts);
		
		List<Double> diffs = new ArrayList<Double>();
		int thisPos = 0;
		double currentCut = 0.0;
		double nextCut = 0.0;
		for(int i = 0; i < otherPoints.size(); i++){
			double cutToFind = otherPoints.get(i);
			while(nextCut <= cutToFind && thisPos < thisCuts.size()){
				currentCut = nextCut;
				nextCut = thisCuts.get(thisPos++);
			}
			double cutDiff = Math.min(Math.abs(cutToFind - currentCut), Math.abs(cutToFind - nextCut));
			diffs.add(cutDiff);
		}
		// Only use the top portion of the diffs to reduce the impact of partial digestion
		Collections.sort(diffs);
		double totalDiff = 0.0;
		for(int i = 0; i < diffs.size() * fraction; i++){
			totalDiff += diffs.get(i);
		}
		return totalDiff;
	}
	
	private boolean flipped = false;
	
	public boolean isFlipped(){
		return flipped;
	}
	
	public boolean flip(){
		flipped = !flipped;
		return flipped;
	}
	
	public void flip(boolean flip){
		flipped = flip;
	}
	
	@Override
	public Iterator<Double> iterator() {
		if(!flipped){
			return cuts.iterator();
		} else {
			return cutsFlipped.iterator();
		}
	}
}
