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

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.GobanMap;
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
		private final AnalysisNode root;
		private final double[][] territory;

		public RandomSimulator(AnalysisNode root, double[][] territory)
		{
			this.root = root;
			this.territory = territory;
		}
		
		@Override
		public final AnalysisResult call() throws Exception {
			return root.evaluateRandomSequence(territory);
		}
	}

	static class AnalysisNode implements Comparable<AnalysisNode>
	{
		int boardSize;
		AnalysisGoban goban;
		double komi;
		Point move;
		int moveNo;
		BoardType movingColor;
		AnalysisNode parent;
		double suitability;
		Vector<Integer> hashCodes;
		Map<Point, Miai> miaiMap;

		double wins;
		double score;
		double score2;
		double depth;

		int blackAtari = -1;
		int whiteAtari = -1;
		
		public AnalysisNode(AnalysisNode analysisNode) 
		{
			this.parent = analysisNode;
			this.hashCodes = analysisNode.hashCodes;
			this.boardSize = analysisNode.boardSize;
			this.goban = analysisNode.goban.clone();
			this.moveNo = analysisNode.moveNo;
			this.komi = analysisNode.komi;
			this.miaiMap = analysisNode.miaiMap;
		}
		
		public AnalysisNode(Goban goban, BoardType movingColor)
		{
			this(goban, movingColor, 6.5);
		}
		
		public AnalysisNode(Goban goban, BoardType movingColor, double komi) 
		{
			this.hashCodes = new Vector<Integer>(100, 100);
			this.hashCodes.setSize(1);
			this.hashCodes.set(0, goban.hashCode());
			this.goban = new AnalysisGoban(goban);
			this.boardSize = goban.getBoardSize();
			this.komi = komi;
			this.miaiMap = new GobanMap<Miai>(this.boardSize);
			this.movingColor = movingColor;
			this.moveNo = 0;
			
			initializeMiai();
		}
		
		/** Evaluates the score of a Goban */
		public void evaluateRandomly(ExecutorService executor, double[][] territory)
		{
			AnalysisNode root = this;
			AnalysisResult result;
			int boardSize = goban.getBoardSize();
			
			Queue<Future<AnalysisResult>> workQueue = new LinkedList<Future<AnalysisResult>>();
			for (int i=0; i<NUM_SIMULATIONS; i++) {
				RandomSimulator simulator = new RandomSimulator(root, territory);
				workQueue.add(executor.submit(simulator));
			}
			
			Future<AnalysisResult> future;
			while (workQueue.size() > 0)
			{
				try {
					future = workQueue.peek();
					if (!future.isDone()) {
						Thread.sleep(500);
						continue;
					}
					else
					{
						result = future.get();
						workQueue.remove(future);
						if (result == null) continue;
						if (movingColor == BoardType.BLACK)
							result.score -= komi;
						else
							result.score += komi;
						
						if (result.score < 0)
							wins += 1;
						score -= result.score;
						score2 += result.score*result.score;
						depth += result.depth;
					}
				}
				catch (Exception ex) {
					logger.log(Level.WARNING, "evaluate: ", ex);
				}
			}
			
			score2 = Math.sqrt((score2 - score*score/NUM_SIMULATIONS) / (NUM_SIMULATIONS - 1));
			score /= NUM_SIMULATIONS;
			depth /= NUM_SIMULATIONS;
			wins /= NUM_SIMULATIONS;
			
			logger.info("evaluateRandomly: " + move + " " + wins + " " + score);
		}

			
		public AnalysisResult evaluateRandomSequence(double[][] territory)
		{
			AnalysisResult result = new AnalysisResult(territory);
			AnalysisNode currentNode = this; 
			
			while (true) {
				currentNode = selectRandomMove(currentNode);
				currentNode.parent.parent = null;
				result.depth++;
				// logger.info("evaluate: " + currentNode);

				if (currentNode.isPass() && currentNode.parent.isPass()) {
					break;
				}
				
				if (currentNode.moveNo > MAX_MOVES) {
					logger.severe("exiting after " + MAX_MOVES + " moves: " + this);
					return null;
				}
			} 
			result.score = chineseScore(currentNode, this.movingColor, result.territory);
			// logger.info("score: " + score + "\n" + currentNode);
			return result;
		}

		private void initializeMiai() 
		{
			updateMiai();
		}

		private void updateMiai() 
		{
			// TODO Look for miai pairs and add them.	
		}

		public void addMiai(Miai miai)
		{
			miaiMap.put(miai.p1, miai);
			miaiMap.put(miai.p2, miai);
		}
		
		private AnalysisNode createChild() 
		{
			AnalysisNode child = new AnalysisNode(this);
			child.movingColor = movingColor.opposite();
			child.moveNo = moveNo + 1;
			this.parent = null;
			return child;
		}
		
		public AnalysisNode createChild(Point p) 
		{
			AnalysisNode child = createChild();
			child.move = p;
			child.goban.move(p, movingColor);
			if (child.moveNo >= child.hashCodes.size()) child.hashCodes.setSize(child.moveNo + 1);
			child.hashCodes.set(child.moveNo, child.goban.hashCode());

			updateMiai();
			child.suitability = child.calculateSuitability();
			//logger.info("createChild: \n" + child);
			return child;
		}
		
		/**
		 * Calculate the suitability of a move. 
		 * The suitability will be used as a probability when choosing a move.
		 * @return the suitability of this @code{AnalysisNode}
		 */
		double calculateSuitability() 
		{
			double suitability = 1;
			BoardType color = goban.getStone(move);
			
			// Only play on empty fields
			if (parent.goban.getStone(move) != BoardType.EMPTY) return 0;
			
			// Don't play Suicide
			if (goban.getStone(move) == BoardType.EMPTY) return 0;
			

			// Obey Ko rule 
			int hashCode = goban.hashCode();
			for (int i=moveNo-1; i>0; i--) {
				if (hashCodes.get(i) == hashCode) return 0;
			}
			
			Vector<Point> removed = goban.getRemoved();
			int saved = getSavedStones();

			// don't fill an eye
			if (parent.goban.isOneSpaceEye(move) && removed.size() == 0 && saved <= 0) return 0;
			
			// Capture is good
			if (removed.size() > 0) {
				//logger.info("Capturing: " + move + " captures " + removed.size());
				suitability += 2 * removed.size();
			}
						
			// Saving stones is good
			if (saved > 0) {
				//logger.info("Saving: " + move + " saves " + saved);
				suitability += 2*saved;
			}

			Miai miai = null;
			if (parent.move != null) miai = miaiMap.get(parent.move);
			if (miai != null && miai.other(parent.move).equals(move)) {
				//logger.info("miai found!!" + miai);
				suitability += miai.value;
			}
						
			// Self-Atari is discouraged
			if (saved < 0) 
				suitability *= -1.0 / saved;
			
			/*
			if (goban.libertyMap.containsKey(move)) {
				for (Chain chain : goban.libertyMap.get(move)) {
					if (chain.color == color.opposite())
						suitability += chain.size() / chain.numLiberties;
				}
			}
			*/
			
			return suitability;
		}

		private int getSavedStones() 
		{
			int parentAtari = parent.getAtariCount(parent.movingColor);		
			int myAtari = getAtariCount(parent.movingColor);
			
			// logger.info("getSavedStones: " + move + ": " + parentAtari + " - " + myAtari);
			return parentAtari - myAtari;
		}

		int getAtariCount(BoardType movingColor) 
		{
			switch (movingColor) {
			case BLACK:
				if (blackAtari < 0) {
					blackAtari = goban.getAtariCount(movingColor);
					//logger.info("getAtariCount(BLACK): " + this);
				}
				return blackAtari;

			case WHITE:
				if (whiteAtari < 0) {
					whiteAtari = goban.getAtariCount(movingColor);
					//logger.info("getAtariCount(WHITE): " + this);
				}
				return whiteAtari;
				
			case EMPTY:
			default:	
				return 0;	
			}
		}

		public AnalysisNode createPassNode() 
		{
			//logger.info("creating pass move");
			AnalysisNode child = createChild();
			child.move = null;
			return child;
		}
		
		public boolean isPass() 
		{
			return move == null;
		}

		@Override
		public int compareTo(AnalysisNode node) {
			return move.compareTo(node.move);
		}

		@Override
		public String toString() {
			return "AnalysisNode [move=" + move
					+ ", moveNo=" + moveNo + ", wins=" + wins + ", movingColor=" + movingColor
					+ ", blackAtari=" + blackAtari + ", whiteAatari=" + whiteAtari
					+ ", suitability=" + suitability + "\n" + goban + "]";
		}
	}

	private static Logger logger = Logger.getLogger(Evaluator.class.getName());

	int numNodes = 0;

	final static int NUM_SIMULATIONS = 50;
	final static int MAX_MOVES = 200;


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
		root.evaluateRandomly(executor, territory);

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
				                  root.wins, root.score, root.score2, root.depth, goban, sb.toString()));
		
		return root.score;
	}
	
	
	/** 
	 * Try to make a (sensible) random move. 
	 * A move is considered sensible if it is <ul>
	 * <li> legal,
	 * <li> does not fill an own eye.
	 * </ul>
	 */
	public static AnalysisNode selectRandomMove(AnalysisNode parent)
	{
		double totalSuitability = 0;
		Map<AnalysisNode, Double> map = new TreeMap<AnalysisNode, Double>();
		int size = parent.boardSize;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (parent.goban.getStone(i, j) != BoardType.EMPTY) continue;
				Point p = new Point(i, j);
				AnalysisNode node = parent.createChild(p);
				// numNodes++;
				if (node.suitability > 0) {
					totalSuitability += node.suitability;
					map.put(node, node.suitability);
				}
			}
		}
		
		double random = Math.random();
		Set<Map.Entry<AnalysisNode, Double>> entries = map.entrySet();
		//logger.info("selectRandomMove: " + parent.movingColor + " " + totalSuitability + " " + random);
		for (Map.Entry<AnalysisNode, Double> entry : entries) {
			random -= entry.getValue() / totalSuitability;
			if (random < 0) {
				AnalysisNode node = entry.getKey();
				//if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
				//node.hashCodes.set(node.moveNo, node.goban.hashCode());
				return node;
			}
		}
		AnalysisNode node = parent.createPassNode();
		//if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
		//node.hashCodes.set(node.moveNo, node.goban.hashCode());
		return node;
	}

	/** Evaluates the score of a Goban */
	public static double evaluate(Goban goban, Goban.BoardType movingColor, double komi)
	{
		ExecutorService executor = Executors.newFixedThreadPool(4);
		
		int boardSize = goban.getBoardSize();
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
        double[][] territory = null; //new double[boardSize][boardSize];
        
        double[][] wins = new double[boardSize][boardSize]; 
        for (Point p : Point.all(boardSize)) {
        	AnalysisNode node = root.createChild(p);
        	if (node.suitability > 0) {
        		node.evaluateRandomly(executor, null);
        		wins[p.getX()][p.getY()] = node.wins;
        		logger.info("evaluate: " + node);
        	}
        }
        	
	
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<boardSize; i++) {
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
		
		logger.info(String.format("evaluate: wins=%.1f, score=%.1f +- %.1f, average depth=%.1f\n%s\n%s", 
				                  root.wins, root.score, root.score2, root.depth, goban, sb.toString()));
		
		return root.score;
	}
}