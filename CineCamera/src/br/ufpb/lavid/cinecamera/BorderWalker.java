package br.ufpb.lavid.cinecamera;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Point;
import android.util.Log;

public class BorderWalker {
	
	public enum Direction {
		NORTH, NORTH_EAST, EAST, SOUTH_EAST, 
		SOUTH, SOUTH_WEST, WEST, NORTH_WEST;
		
		public Point next(final Point from) {
			switch (this) {
				case NORTH: return new Point(from.x, from.y-1);
				case NORTH_EAST: return new Point(from.x+1, from.y-1);
				case EAST: return new Point(from.x+1, from.y);
				case SOUTH_EAST: return new Point(from.x+1, from.y+1);
				case SOUTH: return new Point(from.x, from.y+1);
				case SOUTH_WEST: return new Point(from.x-1, from.y+1);
				case WEST: return new Point(from.x-1, from.y);
				case NORTH_WEST: return new Point(from.x-1, from.y-1);
				default: return from;
			}
		}
		
		public Direction nextClockwise() {
			return Direction.values()[(ordinal()+1)%Direction.values().length];
		}
		
		public Direction opposite() {
			return Direction.values()[(ordinal()+4)%Direction.values().length];
		}
	}
	
	private final byte zbuffer[];
	public final int width;
	public final int height;
	
	public BorderWalker(int width, int height) {
		this.width = width;
		this.height = height;
		this.zbuffer = new byte[width*height];
	}
	
	public void reset() {
		Arrays.fill(zbuffer, (byte)0);
	}
	
	public PixelBorder walk(final byte img[], final Point p0) {
		return walk(img, p0, 127);
	}
	
	public PixelBorder walk(final byte img[], final Point p0, final int threshold) {
		PixelBorder result = new PixelBorder();
		result.add(new Point(p0));
		
		//Log.d("BorderWalker", "p0 -> "+p0);
		
		Direction currentDirection = Direction.WEST;
		Point currentPoint = p0;
		int i;
		int loop = 0;
		do {
			for (i = 0; i < Direction.values().length; ++i) {
			    currentDirection = currentDirection.nextClockwise();
			    Point nextPoint = currentDirection.next(currentPoint);
			    
			    if (nextPoint.x >= 0 && nextPoint.x < width && 
			    	nextPoint.y >= 0 && nextPoint.y < height &&
			    	(img[nextPoint.y*width + nextPoint.x]&0xff) > threshold) {
			    	
			        result.add(nextPoint);
			        currentPoint = nextPoint;
			        currentDirection = currentDirection.opposite();
			        zbuffer[nextPoint.y*width + nextPoint.x] = 1;
			        //Log.d("BorderWalker", "Found point -> "+currentPoint + ", p0 = "+p0);
			        break;
			    }
			    
			}
			++loop;
			if (loop % 1000 == 0) {
				Log.d("BorderWalker", "Looping for "+loop+ " times");
			}
		} while (currentPoint.equals(p0) == false);
		
		return result;
	}
	
	public List<PixelBorder> findAllBorders(final byte img[]) {
		List<PixelBorder> result = new LinkedList<PixelBorder>();
		for (int i = 0; i < height; ++i) {
			int last = 0;
			for (int j = 0; j < width; ++j) {
				int idx = width*i + j;
				if (zbuffer[idx] == 0 && (img[idx] & 0xff) >= 128 && last < 128) {
					result.add(walk(img, new Point(j, i)));
				}
				last = img[idx] & 0xff;
			}
		}
		
		return result;
	}
	
}
