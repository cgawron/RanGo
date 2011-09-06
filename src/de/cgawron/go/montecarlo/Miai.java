package de.cgawron.go.montecarlo;

import de.cgawron.go.Point;

/**
 * This class represents the concept of miai moves in the game of Go: If one player plays one of the miai moves, the 
 * opponent should usually answer with a certain other move and vice versa.
 * 
 *  Neglecting to answer (to play tenuki) usually loses points, represented by the value of the miai.
 *  
 * @author Christian Gawron
 *
 */
public class Miai
{
	public Point p1;
	public Point p2;
	public double value;

	public Miai(Point p1, Point p2, double value)
	{
		this.p1 = p1;
		this.p2 = p2;
		this.value = value;
	}
	
	public Point other(Point move) {
		if (move.equals(p1)) return p2;
		else if (move.equals(p2)) return p1;
		else return null;
	}

	@Override
	public String toString() {
		return "Miai [p1=" + p1 + ", p2=" + p2 + ", value=" + value + "]";
	}
	
}
