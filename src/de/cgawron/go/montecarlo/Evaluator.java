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

import java.io.File;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.GameTree;

/** 
 * Evaluate a Node using Monte Carlo simulation
 * @author Christian Gawron
 */
public class Evaluator
{
	public interface EvaluatorListener extends EventListener
	{
		public void stateChanged(EvaluatorEvent event);
	}
	
	public class EvaluatorEvent extends EventObject 
	{
		public EvaluatorEvent() {
			super(Evaluator.this);
		}

		private static final long serialVersionUID = 1L;
		
	}
	
	public static class AnalysisResult {
		public int score;
		public int depth;
		public double[][] territory;
		
		public AnalysisResult(double[][] territory) {
			this.territory = territory;
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
			evaluateSequenceByUCT(node, territory);
		}

	}

	static Logger logger = Logger.getLogger(Evaluator.class.getName());

	int numNodes = 0;
	Map<AnalysisNode, AnalysisNode> workingTree;

	private int simulation;
	private ExecutorService executor;

	final static int NUM_SIMULATIONS = 1000;
	final static int MAX_MOVES = 200;

	public Evaluator()
	{
		executor = Executors.newFixedThreadPool(1);
		workingTree = new HashMap<AnalysisNode, AnalysisNode>();
	}
	
	public static int chineseScore(AnalysisNode node, Goban.BoardType movingColor, double[][] territory)
	{
		return chineseScore(node.goban, movingColor, territory);
	}
	
	/**
	 * Calculate the chinese score of a Goban position.
	 */
	public static int chineseScore(AnalysisGoban goban, Goban.BoardType movingColor, double[][] territory)
	{
		int score = goban.chineseScore(territory);
		
		if (movingColor == BoardType.WHITE) 
			return -score;
		else
			return score;
	}

	/** Evaluates the score of a Goban */
	public double evaluate(Goban goban, Goban.BoardType movingColor, double komi)
	{
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
		return evaluate(root);
	}
	
	/** Evaluates the score of a Goban */
	public double evaluate(AnalysisNode root)
	{
		int boardSize = root.goban.getBoardSize();
		createNode(root);
        double[][] territory = null; //new double[boardSize][boardSize];

  		Queue<Future<?>> workQueue = new LinkedList<Future<?>>();
		
        for (simulation=0; simulation<NUM_SIMULATIONS; simulation++) {
        	Runnable simulation = new RandomSimulator(root, territory);
        	workQueue.add(executor.submit(simulation));
        	//evaluateSequenceByUCT(root, territory);
        	//logger.info("simulation " + simulation + ": value=" + root.value + ", tree size: " + workingTree.size());
        }
		
		Future<?> future;
		while (workQueue.size() > 0)
		{
			try {
				future = workQueue.peek();
				if (!future.isDone()) {
					logger.info("still " + workQueue.size() + " simulations outstanding, value=" + root.value);
					Thread.sleep(500);
					continue;
				}
				else
				{
					workQueue.remove(future);
				}
			}
			catch (Exception ex) {
				Evaluator.logger.log(Level.WARNING, "evaluate: ", ex);
			}
		}

		StringBuffer sb = new StringBuffer();
		/*
		for (AnalysisGoban goban : root.children)
		{
			sb.append("\n");
			for (int j=0; j<boardSize; j++) {
				if (wins[i][j] > 0)
					sb.append(String.format(" %3.1f", wins[i][j]));
				else {
					switch (root.goban.getStone(i, j)) {
					case WHITE:
						sb.append("  O ");
						break;
					case BLACK:
						sb.append("  X ");
						break;
					case EMPTY:
						sb.append("  . ");
						break;
					}
				}
			}
		}
		*/
		
		dumpTree(logger, root, 3, 30);
		logger.info(String.format("evaluate: value=%.1f, score=%.1f +- %.1f, average depth=%.1f\n%s\n%s", 
				                  root.value, root.score, root.score2, root.depth, root.goban, sb.toString()));
		
		logger.info("best: " + root.getBestChild());
		return root.getScore();
	}
	
	protected void dumpTree(Logger logger, AnalysisNode root, int depth, int breadth) 
	{
		int d=0;
		Comparator<AnalysisNode> valueComparator = new Comparator<AnalysisNode>() {
			@Override
			public int compare(AnalysisNode a, AnalysisNode b) {
				return -Double.compare(a.value, b.value);
			}			
		};
		SortedSet<AnalysisNode> currentLevel = new TreeSet<AnalysisNode>(valueComparator);
		SortedSet<AnalysisNode> nextLevel = new TreeSet<AnalysisNode>(valueComparator);
		
		currentLevel.add(root);
		while (d < depth) {
			int b = 0;
			for (AnalysisNode n : currentLevel) {
				logger.info("level=" + d + ", n=" + b + ": " + n);
				if (n.children != null)
					nextLevel.addAll(n.children);
				if (++b >= breadth) break;
			}
			d++;
			currentLevel = nextLevel;
			nextLevel = new TreeSet<AnalysisNode>(valueComparator);
		}
	}

	public void evaluateSequenceByUCT(AnalysisNode root, double[][]territory)
	{
        AnalysisNode[] sequence = new AnalysisNode[MAX_MOVES];
        sequence[0] = root;
        
        int i = 0; 
        while (sequence[i].children != null)
        {
        	sequence[i+1] = sequence[i].selectRandomUCTMove(sequence, i);
        	i++;
        	//logger.info("sequence: i=" + i + ": " + sequence[i].move);
        	if (i>1 && sequence[i].move == null && sequence[i-1].move == null) 
        		break;
        }
         
        synchronized(this) {
            createNode(sequence[i]);
        	if (i>1 && sequence[i].move == null && sequence[i-1].move == null) {
        		sequence[i].evaluateByScoring(sequence[i], territory);
        		sequence[i].visits++;
        		updateValues(sequence, i, 1-sequence[i].value, -sequence[i].score);
        	}
        	else {
        		sequence[i].evaluateByMC(sequence, i, territory);
        		updateValues(sequence, i, sequence[i].value, sequence[i].score);
        	}
        }
        //logger.info("simulation " + root.visits + ": UCT sequence end: i=" + i + ": " + sequence[i]);
        //logger.info("simulation " + simulation + ": root: " + sequence[0]);
        
 
  	}

	protected void createNode(AnalysisNode node) 
	{
		synchronized(this) {
			if (workingTree.containsKey(node)) return;
			node.children = new HashSet<AnalysisNode>();
			workingTree.put(node, node);

			for (Point p : Point.all(node.boardSize))
			{
				AnalysisNode child = node.createChild(p);
				double value = child.calculateStaticSuitability();
				if (value > 0) {
					if (workingTree.containsKey(child.goban)) {
						child = workingTree.get(child.goban);
					}
					node.children.add(child);
					child.value = value;
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

	protected void updateValues(AnalysisNode[] sequence, int n, double value, double score) 
	{		
		synchronized (this) {
			for (int i=n-1; i>=0; i--)
			{
				sequence[i].value = (sequence[i].visits*sequence[i].value + value) / (sequence[i].visits+1);
				sequence[i].score += score;
				sequence[i].score2 += score*score;
				sequence[i].visits++;
				value = 1 - value;
				score = -score;
			}
		}
	}
	
	public static void main(String[] args) throws Exception
	{
    	File inputFile = new File("test/sgf", "lifeAndDeath1.sgf");
    	GameTree gameTree = new GameTree(inputFile);
    	Goban goban = gameTree.getLeafs().get(0).getGoban();
		Evaluator evaluator = new Evaluator();
		double score = evaluator.evaluate(goban, BoardType.BLACK, 15);
		logger.info("score=" + score);
	}
}