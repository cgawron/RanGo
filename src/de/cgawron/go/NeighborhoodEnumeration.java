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
 * Enumerate the neighbors of a given point.
 * @author: Christian Gawron
 */
import de.cgawron.go.Point;
import java.util.Enumeration;

/**
 * 
 * @author cgawron
 * @deprecated
 */
public class NeighborhoodEnumeration implements Enumeration<Point>
{
	protected int direction;
	protected int size;
	protected Point point;
	private Point nextPoint;

	public NeighborhoodEnumeration(Goban goban, Point p)
	{
		super();
		point = p;
		size = (int) goban.getBoardSize();
		direction = 0;
		calcNext();
	}

	private void calcNext()
	{
		nextPoint = null;
		while (nextPoint == null && direction < 4) {
			switch (direction) {
			case 0:
				if (point.getX() + 1 < size)
					nextPoint = new Point((short) (point.getX() + 1), point.getY());
				break;
			case 1:
				if (point.getY() + 1 < size)
					nextPoint = new Point(point.getX(),	(short) (point.getY() + 1));
				break;
			case 2:
				if (point.getX() > 0)
					nextPoint = new Point((short) (point.getX() - 1), point.getY());
				break;
			case 3:
				if (point.getY() > 0)
					nextPoint = new Point(point.getX(),	(short) (point.getY() - 1));
				break;
			}
			direction++;
		}
	}

	/** hasMoreElements method comment. */
	public boolean hasMoreElements()
	{
		return nextPoint != null;
	}

	/** nextElement method comment. */
	public Point nextElement()
	{
		Point p = nextPoint;
		calcNext();
		return p;
	}
}
