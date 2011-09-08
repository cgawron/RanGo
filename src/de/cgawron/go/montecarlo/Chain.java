package de.cgawron.go.montecarlo;

import java.util.TreeSet;

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * A connected chain of stones.
 *  
 * @author Christian Gawron
 *
 */
public class Chain extends TreeSet<Point> implements Comparable<Chain>
{
	private static final long serialVersionUID = 1L;
	// The chain id will also be used as a "visited" marker. They are negative to avoid collisions there.
	private static int lastId = -1;
	
	BoardType color;
	int numLiberties;

	public int id;

	public Chain(BoardType color) {
		super();
		this.id = lastId--;
		this.color = color;
	}

	public void addLiberty(Point p) 
	{
		numLiberties++;
	}

	@Override
	public int compareTo(Chain chain) 
	{
		if (this.id < chain.id) return -1;
		else if (this.id > chain.id) return 1;
		else return 0;
	}
}
