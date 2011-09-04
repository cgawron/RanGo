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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
	private static Logger logger = Logger.getLogger(Evaluator.class.getName());

	final static int NUM_SIMULATIONS = 10;

	/** Evaluates the score of a Goban */
	public static double evaluate(Goban goban, Goban.BoardType movingColor)
	{
		double score = 0;
		for (int i=0; i<NUM_SIMULATIONS; i++) {
			score += evaluateRandomSequence(goban, movingColor);
		}
		
		return score / NUM_SIMULATIONS;
	}

	public static double evaluateRandomSequence(Goban goban, Goban.BoardType movingColor)
	{
		boolean lastSidePassed = false;
		Goban.BoardType currentColor = movingColor;
		
		while (true) {
			Point p = selectRandomMove(goban, currentColor);
			logger.info("evaluate: " + p);

			if (p == null) {
				if (lastSidePassed)
					break;
				else
					lastSidePassed = true;
			}
			else {
				goban.move(p, currentColor);
				lastSidePassed = false;
			}
			currentColor = currentColor.opposite();
		} 
		return chineseScore(goban, movingColor);
	}
	
	/** 
	 * Try to make a (sensible) random move. 
	 * A move is considered sensible if it is <ul>
	 * <li> legal,
	 * <li> does not fill an own eye.
	 * </ul>
	 */
	public static Point selectRandomMove(Goban goban, Goban.BoardType movingColor)
	{
		// FIXME
		double totalSuitability = 0;
		Map<Point, Double> map = new TreeMap<Point, Double>();
		int size = goban.getBoardSize();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Point p = new Point(i, j);
				double suitability = getSuitability(goban, p, movingColor);
				if (suitability > 0) {
					totalSuitability += suitability;
					map.put(p, suitability);
				}
			}
		}
		
		double random = Math.random();
		Set<Map.Entry<Point, Double> > entries = map.entrySet();
		logger.info("selectRandomMove: " + movingColor + " " + totalSuitability + " " + random);
		for (Map.Entry<Point, Double> entry : entries) {
			random -= entry.getValue() / totalSuitability;
			if (random < 0) return entry.getKey();
		}
		return null;
	}
	
	private static double getSuitability(Goban goban, Point p, BoardType movingColor) 
	{
		SimpleGoban sg = (SimpleGoban) goban;
		
		if (goban.getStone(p) != BoardType.EMPTY)
			return 0;
		else { 
			if (sg.isCapture(p, movingColor))
 				return 2;
			
			sg.setStone(p, movingColor);
			int liberties = sg.countLiberties(p);
			sg.setStone(p, BoardType.EMPTY);
			
			if (liberties == 0) return 0;
			
			if (sg.isEye(p, movingColor)) 
				return 0;
			else
				return 1;
		}
	}

	/**
	 * Calculate the chinese score of a Goban position.
	 */
	public static int chineseScore(Goban goban, Goban.BoardType movingColor)
	{
		int score = goban.chineseScore();
		
		if (movingColor == BoardType.WHITE) 
			return -score;
		else
			return score;
	}
}