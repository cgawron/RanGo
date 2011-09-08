/*
 * Copyright (C) 2010 Christian Gawron
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GobanMap<T> extends AbstractMap<Point, T> 
{
	Object[][] store;
	int boardSize;
	int size;
	
	EntrySet entrySet = new EntrySet();
	
	private class EntryIterator implements Iterator<Map.Entry<Point, T>>
	{
		int i=0;
		int j=0;
		
		@Override
		public boolean hasNext() {
			if (i >= boardSize) {
				i=0; 
				j++;
				if (j>= boardSize)
					return false;
			}
			while (store[i][j] == null) {
				i++;
				if (i >= boardSize) {
					i=0; 
					j++;
					if (j>= boardSize)
						return false;
				}
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map.Entry<Point, T> next() {
			return new AbstractMap.SimpleImmutableEntry<Point, T>(new Point(i, j), (T) store[i++][j]);
		}

		@Override
		public void remove() 
		{
			throw new UnsupportedOperationException();
		}
		
	}
	
	private class EntrySet extends AbstractSet<Map.Entry<Point, T>> 
	{
		@Override
		public Iterator<java.util.Map.Entry<Point, T>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return size;
		}
	}
	
	public GobanMap(int boardSize)
	{
		this.boardSize = boardSize;
		store = new Object[boardSize][boardSize];
	}
	
	@Override
	public boolean containsKey(Object key)
	{
		Point point = (Point) key;
		return store[point.getX()][point.getY()] != null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(Object key)
	{
		Point point = (Point) key;
		return (T) store[point.getX()][point.getY()];
	}
	
	@Override
	public T put(Point p, T value) 
	{
		if (value != null) size++;
		T oldValue = get(p);
		if (oldValue != null) size--;
		store[p.getX()][p.getY()] = value;
		return oldValue;
	}
	
	@Override
	public Set<Map.Entry<Point, T>> entrySet() 
	{
		return entrySet;
	}

}
