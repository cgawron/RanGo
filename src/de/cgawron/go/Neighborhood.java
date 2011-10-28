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

package de.cgawron.go;

/**
 * Iterate over the neighbors of a given point.
 * @author: Christian Gawron
 */
import java.util.Iterator;

public final class Neighborhood implements Iterable<Point>
{
	public class NeighborhoodIterator implements Iterator<Point> {
		public int direction;
		public Point nextPoint;

		public NeighborhoodIterator() {
			direction = 0;
			calcNext();
		}

		@Override
		public boolean hasNext() {
			return nextPoint != null;
		}

		@Override
		public Point next() {
			Point p = nextPoint;
			calcNext();
			return p;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	
		private final void calcNext()
		{
			nextPoint = null;
			while (nextPoint == null && direction < 4) {
				switch (direction) {
				case 0:
					if (x + 1 < size)
						nextPoint = new Point((x + 1), y);
					break;
				case 1:
					if (y + 1 < size)
						nextPoint = new Point(x, (y + 1));
					break;
				case 2:
					if (x > 0)
						nextPoint = new Point((x - 1), y);
					break;
				case 3:
					if (y > 0)
						nextPoint = new Point(x, (y - 1));
					break;
				}
				direction++;
			}
		}
	}

	private int size;
	private int x;
	private int y;

	@Deprecated
	public Neighborhood(Goban goban, Point p)
	{
		this(goban, p.getX(), p.getY());
	}

	public Neighborhood(int boardSize, Point p)
	{
		this(boardSize, p.getX(), p.getY());
	}
	
	@Deprecated
	public Neighborhood(Goban goban, int x, int y)
	{
		this.x = x;
		this.y = y;
		size = goban.getBoardSize();
	}

	public Neighborhood(int size, int x, int y)
	{
		this.x = x;
		this.y = y;
		this.size = size;
	}
	@Override
	public Iterator<Point> iterator() {
		return new NeighborhoodIterator();
	}
}
