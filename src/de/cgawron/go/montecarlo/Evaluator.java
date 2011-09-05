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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.SimpleGoban;

/** 
 * Evaluate a Node using Monte Carlo simulation
 * @author Christian Gawron
 */
public class Evaluator
{
	private static class AnalysisNode implements Comparable<AnalysisNode>
	{
		int boardSize;
		AnalysisGoban goban;
		Point move;
		int moveNo;
		BoardType movingColor;
		AnalysisNode parent;
		double suitability;
		Vector<Integer> hashCodes;
		Map<Point, Miai> miaiMap;

		int blackAtari = -1;
		int whiteAtari = -1;
		
		public AnalysisNode(AnalysisNode analysisNode) 
		{
			this.parent = analysisNode;
			this.hashCodes = analysisNode.hashCodes;
			this.boardSize = analysisNode.boardSize;
			this.goban = analysisNode.goban.clone();
			this.moveNo = analysisNode.moveNo;
			this.miaiMap = analysisNode.miaiMap;
		}
		
		public AnalysisNode(Goban goban, BoardType movingColor) 
		{
			this.hashCodes = new Vector<Integer>(100, 100);
			this.hashCodes.setSize(1);
			this.hashCodes.set(0, goban.hashCode());
			this.miaiMap = new TreeMap<Point, Miai>();
			this.goban = new AnalysisGoban(goban);
			this.boardSize = goban.getBoardSize();
			this.movingColor = movingColor;
			this.moveNo = 0;
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
			child.moveNo++;
			return child;
		}
		
		public AnalysisNode createChild(Point p) 
		{
			AnalysisNode child = createChild();
			child.move = p;
			child.goban.move(p, movingColor);
			child.suitability = child.calculateSuitability();
			//logger.info("createChild: " + child);
			return child;
		}
		
		private double calculateSuitability() 
		{
			double suitability = 1;
			
			// Don't play Suicide
			if (goban.getStone(move) == BoardType.EMPTY) return 0;
			

			// Obey Ko rule 
			int hashCode = goban.hashCode();
			for (int i=moveNo-1; i>0; i--) {
				if (hashCodes.get(i) == hashCode) return 0;
			}
			
			// Capture is good
			Vector<Point> removed = goban.getRemoved();
			if (removed.size() > 0) {
				//logger.info("Capturing: " + move + " captures " + removed.size());
				suitability += 2 * removed.size();
			}
						
			// Saving stones is good
			int saved = getSavedStones();
			if (saved > 0) {
				//logger.info("Saving: " + move + " saves " + saved);
				suitability += 2*saved;
			}

			Miai miai = null;
			if (parent.move != null) miai = miaiMap.get(parent.move);
			if (miai != null && miai.other(parent.move).equals(move)) {
				logger.info("miai found!!" + miai);
				suitability += miai.value;
			}
			
			// don't fill an eye
			if (parent.goban.isOneSpaceEye(move) && saved <= 0) return 0;
			
			// Self-Atari is discouraged
			if (saved < 0) 
				suitability *= -1.0 / saved;
			
			return suitability;
		}

		private int getSavedStones() 
		{
			int parentAtari = parent.getAtariCount(parent.movingColor);		
			int myAtari = getAtariCount(parent.movingColor);
			
			//logger.info("getSavedStones: " + parentAtari + " - " + myAtari);
			return parentAtari - myAtari;
		}

		private int getAtariCount(BoardType movingColor) 
		{
			switch (movingColor) {
			case BLACK:
				if (blackAtari < 0) blackAtari = goban.getAtariCount(movingColor);
				return blackAtari;

			case WHITE:
				if (whiteAtari < 0) whiteAtari = goban.getAtariCount(movingColor);
				return whiteAtari;
				
			case EMPTY:
			default:	
				return 0;	
			}
		}

		public AnalysisNode createPassNode() 
		{
			logger.info("creating pass move");
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
					+ ", moveNo=" + moveNo + ", movingColor=" + movingColor
					+ ", suitability=" + suitability + "\n" + goban + "]";
		}
	}

	private static Logger logger = Logger.getLogger(Evaluator.class.getName());

	int numNodes = 0;

	final static int NUM_SIMULATIONS = 500;

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
	public static double evaluate(Goban goban, Goban.BoardType movingColor)
	{
		int boardSize = goban.getBoardSize();
		double score = 0;
		double score2 = 0;
		double territory[][] = new double[boardSize][boardSize];
		for (int i=0; i<NUM_SIMULATIONS; i++) {
			double value = evaluateRandomSequence(goban, movingColor, territory); 
			score += value;
			score2 += value*value;
		}

		StringBuffer sb = new StringBuffer();
		for (int i=0; i<boardSize; i++) {
			sb.append("\n");
			for (int j=0; j<boardSize; j++) {
				sb.append(String.format("%4.1f ", territory[i][j] / NUM_SIMULATIONS));
			}
		}
		
		score2 = Math.sqrt((score2 - score*score/NUM_SIMULATIONS) / (NUM_SIMULATIONS - 1));
		score /= NUM_SIMULATIONS;
		
		logger.info("evaluate: score=" + score + " +- " + score2 + "\n" + goban + "\n" + sb.toString());
		
		return score;
	}
	
	public static double evaluateRandomSequence(Goban goban, Goban.BoardType movingColor, double[][] territory)
	{
		AnalysisNode root = new AnalysisNode(goban, movingColor);
		// FIXME: Hack to test Miai
		root.addMiai(new Miai(new Point(7, 6), new Point(7, 7), 30));
		AnalysisNode currentNode = root; 
		
		while (true) {
			currentNode = selectRandomMove(currentNode);
			// logger.info("evaluate: " + currentNode);

			if (currentNode.isPass() && currentNode.parent.isPass()) {
				break;
			}
		} 
		int score = chineseScore(currentNode, movingColor, territory);
		// logger.info("score: " + score + "\n" + currentNode);
		return score;
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
		Set<Map.Entry<AnalysisNode, Double> > entries = map.entrySet();
		//logger.info("selectRandomMove: " + parent.movingColor + " " + totalSuitability + " " + random);
		for (Map.Entry<AnalysisNode, Double> entry : entries) {
			random -= entry.getValue() / totalSuitability;
			if (random < 0) {
				AnalysisNode node = entry.getKey();
				if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
				node.hashCodes.set(node.moveNo, node.goban.hashCode());
				return node;
			}
		}
		AnalysisNode node = parent.createPassNode();
		if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
		node.hashCodes.set(node.moveNo, node.goban.hashCode());
		return node;
	}
}