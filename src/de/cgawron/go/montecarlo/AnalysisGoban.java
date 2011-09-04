package de.cgawron.go.montecarlo;

import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.NeighborhoodEnumeration;
import de.cgawron.go.Point;
import de.cgawron.go.SimpleGoban;

public class AnalysisGoban extends SimpleGoban 
{
	public static Logger logger = Logger.getLogger(AnalysisGoban.class.getName());

	public AnalysisGoban() {
		super();
	}

	public AnalysisGoban(Goban m) {
		super(m);
	}

	public AnalysisGoban(int size) {
		super(size);
	}

	public AnalysisGoban clone() 
	{
		AnalysisGoban goban = new AnalysisGoban();
		goban.copy(this);
		return goban;
	}
	
	public Vector<Point> getRemoved() 
	{
		return removed;
	}
	
	public boolean isCapture(Point p, BoardType movingColor) 
	{
		
		//int captured = 0;
		if (getStone(p) != BoardType.EMPTY) return false;
	
		BoardType enemy = movingColor.opposite();
		setStone(p, movingColor);
		NeighborhoodEnumeration ne = new NeighborhoodEnumeration(this, p);
		while (ne.hasMoreElements()) {
			Point q = (Point) ne.nextElement();
			visited++;
			if (getStone(q) == enemy)
				if (countLiberties(q) == 0) {
					setStone(p, BoardType.EMPTY);
					return true;
				}
		}
		setStone(p, BoardType.EMPTY);
		return false;	
	}

	public boolean isOneSpaceEye(Point p) 
	{
		NeighborhoodEnumeration ne = new NeighborhoodEnumeration(this, p);
		p = (Point) ne.nextElement();
		BoardType color = getStone(p);
		if (color == BoardType.EMPTY) return false;
		while (ne.hasMoreElements()) {
			p = (Point) ne.nextElement();
			if (getStone(p) != color) 
				return false;	
		}
		return true;
	}

	public boolean isIllegalKoCapture(Set<Goban> history, Point p, BoardType movingColor) {
		SimpleGoban goban = (SimpleGoban) clone();
		return history.contains(goban.move(p, movingColor));
	}

	public boolean isValidMove(Point p, BoardType movingColor) {
		if (getStone(p) != BoardType.EMPTY)
			return false;
		else { 
			if (isCapture(p, movingColor))
				return true;
			
			setStone(p, movingColor);
			int liberties = countLiberties(p);
			setStone(p, BoardType.EMPTY);
			
			return liberties > 0;
		}
	}

	public int getAtariCount(BoardType movingColor) 
	{
		int count = 0;
		//visited++;
		for (int i=0; i<size; i++) {
			for (int j=0; j<size; j++) {
				if (boardRep[i][j] == movingColor && tmpBoard[i][j] != visited) {
					count += countLiberties(i, j, false);
				}
			}
		}
		return count;
	}

}
