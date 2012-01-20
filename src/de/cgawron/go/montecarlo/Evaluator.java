/*
 * Copyright (C) 2011 Christian Gawron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cgawron.go.montecarlo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttribute;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * Evaluate a Node using Monte Carlo simulation.
 * 
 * @author Christian Gawron
 */
public class Evaluator
{
	public static class AnalysisResult
	{
		public int depth;
		public int score;
		public double[][] territory;

		public AnalysisResult(double[][] territory)
		{
			this.territory = territory;
		}
	}

	public class EvaluatorEvent extends EventObject
	{
		private static final long serialVersionUID = 1L;
		public int outstanding;
		public int total;
		public AnalysisNode root;

		public EvaluatorEvent(AnalysisNode root, int outstanding, int total)
		{
			super(Evaluator.this);
			this.root = root;
			this.outstanding = outstanding;
			this.total = total;
		}

	}

	public interface EvaluatorListener extends EventListener
	{
		public void stateChanged(EvaluatorEvent event);
	}

	public static class EvaluatorParameters 
	{
		@XmlAttribute
		public int maxMoves = 200;
		
		@XmlAttribute
		public int numSimulations = 10000;
		
		@XmlAttribute
		public int numThreads = 1;
		
		@XmlAttribute
		public int steepness = 1;
		
		@XmlAttribute
		public boolean checkTerritory = true;

		@Override
		public String toString() {
			return "EvaluatorParameters [maxMoves=" + maxMoves
					+ ", numSimulations=" + numSimulations + ", numThreads="
					+ numThreads + ", steepness=" + steepness + "]";
		}
	}

	public class RandomSimulator implements Runnable
	{
		private final AnalysisNode node;
		private final double[][] territory;

		public RandomSimulator(AnalysisNode root, double[][] territory)
		{
			this.node = root;
			this.territory = territory;
		}

		@Override
		public void run()
		{
			try {
				evaluateSequenceByUCT(node, territory);
			}
			catch (Throwable t) {
				logger.log(Level.SEVERE, "Exception caught", t);
				throw new RuntimeException(t);
			}
		}
	}

	final static Logger logger = Logger.getLogger(Evaluator.class.getName());
 
	
	public static EvaluatorParameters parameters;
	
	public static int chineseScore(AnalysisNode node, double[][] territory)
	{
		return node.goban.chineseScore(territory);
	}
	
	
	private ExecutorService executor;
	private List<EvaluatorListener> listeners = new ArrayList<EvaluatorListener>();
	private int simulation;
	private double[][] territory;

	Map<AnalysisNode, AnalysisNode> workingTree;

	public Evaluator()
	{
		parameters = new EvaluatorParameters();
		workingTree = new WeakHashMap<AnalysisNode, AnalysisNode>();
	}

	public void addEvaluatorListener(EvaluatorListener listener)
	{
		listeners.add(listener);
	}

	protected void createNode(AnalysisNode node)
	{
		if (workingTree.containsKey(node)) {
			// logger.info("Node " + node + " already present!");
			return;
		}
		
		synchronized (node) {
		node.children = new HashSet<AnalysisNode>();
		workingTree.put(node, node);

		for (Point p : Point.all(node.boardSize)) {
			AnalysisNode child = node.createChild(p);
			if (child.suitability > 0) {
				if (workingTree.containsKey(child.goban)) {
					child = workingTree.get(child.goban);
				}
				node.children.add(child);
			}
		}
		AnalysisNode child = node.createPassNode();
		child.value = 0.1;
		if (workingTree.containsKey(child.goban)) {
			child = workingTree.get(child.goban);
		}
		node.children.add(child);
		}
	}
	

	/**
	 * Dump the analysis tree to a logger.
	 * @param logger 
	 * @param root The root of the tree.
	 * @param depth The desired depth of the dump.
	 * @param factor Only siblings with value >= factor * max(values) will be included.
	 */
	protected void dumpTree(Logger logger, AnalysisNode root, int depth, double factor)
	{
		int d = 0;
		Comparator<AnalysisNode> valueComparator = new Comparator<AnalysisNode>() {
			@Override
			public int compare(AnalysisNode a, AnalysisNode b)
			{
				return -((Integer) a.getVisits()).compareTo(b.getVisits());
			}
		};
		SortedSet<AnalysisNode> currentLevel = new TreeSet<AnalysisNode>(valueComparator);
		SortedSet<AnalysisNode> nextLevel = new TreeSet<AnalysisNode>(valueComparator);

		currentLevel.add(root);
		while (d < depth) {
			int b = 0;
			double best = 0;
			for (AnalysisNode n : currentLevel) {
				if (n.value > best) best = n.value;
				if (n.value < factor*best)
					break;
				logger.info("level=" + d + ", n=" + (b++) + ": " + n);
				if (n.children != null)
					nextLevel.addAll(n.children);
			}
			d++;
			currentLevel = nextLevel;
			nextLevel = new TreeSet<AnalysisNode>(valueComparator);
		}
	}

	/** Evaluates the score of a Goban */

	
	/** Evaluates the score of a Goban */
	public double evaluate(AnalysisNode root)
	{
		//FIXME How can we avoid this?
		workingTree.clear();
		
		int boardSize = root.boardSize;
		createNode(root);
		if (parameters.checkTerritory)
			territory = new double[boardSize][boardSize];
		else
			territory = null;

		Queue<Future<?>> workQueue = new LinkedList<Future<?>>();

		for (simulation = 0; simulation < parameters.numSimulations; simulation++) {
			Runnable simulation = new RandomSimulator(root, territory);
			workQueue.add(getExecutor().submit(simulation));
			// evaluateSequenceByUCT(root, territory);
			// logger.info("simulation " + simulation + ": value=" + root.value
			// + ", tree size: " + workingTree.size());
		}

		Future<?> future;
		while (workQueue.size() > 0) {
			try {
				future = workQueue.peek();
				if (!future.isDone()) {
					logger.info("still " + workQueue.size()
							+ " simulations outstanding, value=" + root.getValue() + ", tree size=" + workingTree.size());
					fireStillWorking(root, workQueue.size(), parameters.numSimulations);
					Thread.sleep(500);
					continue;
				} else {
					workQueue.remove(future);
				}
			} catch (Exception ex) {
				Evaluator.logger.log(Level.WARNING, "evaluate: ", ex);
				throw new RuntimeException(ex);
			}
		}
		fireDone(root, parameters.numSimulations);

		StringBuffer sb = new StringBuffer();
		if (territory != null) {
			for (int i = 0; i < boardSize; i++) {
				sb.append("\n");
				for (int j = 0; j < boardSize; j++) {
					sb.append(String.format(" %3.1f", territory[i][j]/parameters.numSimulations));
				}
			}
		}
		
		dumpTree(logger, root, 4, 0.25);

		logger.info("best: " + root.getBestChild() + sb.toString());
		return root.getScore();
	}

	/** Evaluates the score of a Goban */
	public double evaluate(Goban goban, Goban.BoardType movingColor, double komi)
	{
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
		return evaluate(root);
	}

	public void evaluateSequenceByUCT(AnalysisNode root, double[][] territory)
	{
		AnalysisNode[] sequence = new AnalysisNode[parameters.maxMoves];
		sequence[0] = root;

		int i = 0;
		while (sequence[i].children != null) {
			sequence[i + 1] = sequence[i].selectRandomUCTMove();
			i++;
			// logger.info("sequence: i=" + i + ": " + sequence[i].move);
			if (i > 1 && sequence[i].move == null
					&& sequence[i - 1].move == null)
				break;
		}
		createNode(sequence[i]);

		double score;
		double value;
		if (i > 1 && sequence[i].move == null && 
			sequence[i - 1].move == null) {
			// logger.info("end node reached");
			score = sequence[i].evaluateByScoring(territory);
		} else {
			score = sequence[i].evaluateByMC(sequence, i, territory);
		}

		if (sequence[i].movingColor == BoardType.BLACK)
			score = -score;

		// All or nothing ...
		/*
			if (result.score < 0)
				value = 1;
			else
				value = 0;
		 */
		// Sigmoid exp(x)/(1+exp(x))
		double exp = Math.exp(parameters.steepness*score);
		value = exp / (1+exp);

		//logger.info("score=" + score + ", value=" + value);
		updateValues(sequence, i, value, score);

	}
	
	private void fireDone(AnalysisNode root, int numSimulations)
	{
		EvaluatorEvent event = new EvaluatorEvent(root, 0, numSimulations);
		for (EvaluatorListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	private void fireStillWorking(AnalysisNode root, int outstanding, int total)
	{
		EvaluatorEvent event = new EvaluatorEvent(root, outstanding, total);
		for (EvaluatorListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	public ExecutorService getExecutor() {
		if (executor == null)
			executor = Executors.newFixedThreadPool(parameters.numThreads);
		return executor;
	}

	public void setParameters(EvaluatorParameters parameters) 
	{
		Evaluator.parameters = parameters;
	}
	
	protected void updateValues(AnalysisNode[] sequence, int n, double value, double score)
	{
		synchronized (Evaluator.class) {
			for (int i = n; i >= 0; i--) {
				sequence[i].update(value, score);

				/*
				if (i==1) {
					logger.info("level 1 update: value=" + value + ", score=" + score + sequence[1]);
				}
				*/
				value = 1 - value;
				score = -score;
			}
		}
	}
}