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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/** 
 * Evaluate a Node using Monte Carlo simulation
 * @author Christian Gawron
 */
public class Evaluator
{
	public static class AnalysisResult {
		public int score;
		public int depth;
		public double[][] territory;
		
		public AnalysisResult(double[][] territory) {
			this.territory = territory;
		}
	}

	public static class RandomSimulator implements Callable<AnalysisResult>
	{
		private final AnalysisNode node;
		private final double[][] territory;

		public RandomSimulator(AnalysisNode root, double[][] territory)
		{
			this.node = root;
			this.territory = territory;
		}
		
		@Override
		public final AnalysisResult call() throws Exception {
			return node.evaluateByMC(territory);
		}
	}

	static Logger logger = Logger.getLogger(Evaluator.class.getName());

	int numNodes = 0;
	Map<AnalysisGoban, AnalysisNode> workingTree;

	private int simulation;
	
	final static int NUM_SIMULATIONS = 1000;
	final static int MAX_MOVES = 200;

	public Evaluator()
	{
		workingTree = new HashMap<AnalysisGoban, AnalysisNode>();
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
	public static double evaluateOne(Goban goban, Goban.BoardType movingColor, double komi)
	{
		int boardSize = goban.getBoardSize();
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		double[][] territory = null; //new double[boardSize][boardSize];
		root.evaluateRandomly(executor, NUM_SIMULATIONS, territory);

		StringBuffer sb = new StringBuffer();
		if (territory != null) {
			for (int i=0; i<boardSize; i++) {
				sb.append("\n");
				for (int j=0; j<boardSize; j++) {
					sb.append(String.format("%4.1f ", territory[i][j] / NUM_SIMULATIONS));
				}
			}
		}
		
		logger.info(String.format("evaluate: wins=%.1f, score=%.1f +- %.1f, average depth=%.1f\n%s\n%s", 
				                  root.value, root.score, root.score2, root.depth, goban, sb.toString()));
		
		return root.score;
	}
	
	

	/** Evaluates the score of a Goban */
	public double evaluate(Goban goban, Goban.BoardType movingColor, double komi)
	{
		//ExecutorService executor = Executors.newFixedThreadPool(2);
		
		int boardSize = goban.getBoardSize();
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
		createNode(root);
        double[][] territory = null; //new double[boardSize][boardSize];
       
        for (simulation=0; simulation<NUM_SIMULATIONS; simulation++) {
        	evaluateSequenceByUCT(root, territory);
        	logger.info("simulation " + simulation + ": value=" + root.value + ", tree size: " + workingTree.size());
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
		
		logger.info(String.format("evaluate: value=%.1f, score=%.1f +- %.1f, average depth=%.1f\n%s\n%s", 
				                  root.value, root.score, root.score2, root.depth, goban, sb.toString()));
		
		return root.score;
	}
	
	public void evaluateSequenceByUCT(AnalysisNode root, double[][]territory)
	{
        AnalysisNode[] sequence = new AnalysisNode[MAX_MOVES];
        sequence[0] = root;
        
        int i = 0; 
        while (sequence[i].children != null)
        {
        	sequence[i+1] = sequence[i].selectRandomUCTMove();
        	i++;
        	//logger.info("sequence: i=" + i + ": " + sequence[i].move);
        }
        createNode(sequence[i]);

        sequence[i].evaluateByMC(territory);
        logger.info("simulation " + simulation + ": UCT sequence end: i=" + i + ": " + sequence[i]);
        updateValues(sequence, i, -sequence[i].value);
 	}

	protected void createNode(AnalysisNode node) 
	{
	    node.children = new HashSet<AnalysisNode>();
	    workingTree.put(node.goban, node);
 
        for (Point p : Point.all(node.boardSize))
        {
        	AnalysisNode child = node.createChild(p);
        	double value = child.calculateStaticSuitability();
        	if (value > 0) {
        		child.value = value;
        		node.children.add(child);
        	}
        }
	}

	protected void updateValues(AnalysisNode[] sequence, int n, double value) 
	{
		for (int i=n; i>=0; i--)
		{
			sequence[i].value += value;
			sequence[i].visits++;
			value = 1 - value;
		}
	}
}