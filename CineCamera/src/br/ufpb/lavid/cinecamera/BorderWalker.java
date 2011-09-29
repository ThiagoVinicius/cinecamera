package br.ufpb.lavid.cinecamera;

import java.util.Arrays;

import android.graphics.Point;

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
		
		public Direction nextCounterClockwise() {
			return Direction.values()[(ordinal()+1)%Direction.values().length];
		}
		
		public Direction opposite() {
			return Direction.values()[(ordinal()+4)%Direction.values().length];
		}
	}
	
	private final byte zbuffer[];
	private final int width;
	private final int height;
	
	public BorderWalker(int width, int height) {
		this.width = width;
		this.height = height;
		this.zbuffer = new byte[width*height];
	}
	
	public void reset() {
		Arrays.fill(zbuffer, (byte)0);
	}
	
	public PixelBorder walk(final byte img[], final Point p0) {
		return walk(img, p0, (byte)127);
	}
	
	public PixelBorder walk(final byte img[], final Point p0, final byte threshold) {
		PixelBorder result = new PixelBorder();
		result.add(new Point(p0));
		
		Direction currentDirection = Direction.WEST;
		Point currentPoint = p0;
		do {
			for (int i = 0; i < Direction.values().length; ++i) {
			    Point nextPoint = currentDirection.next(currentPoint);
			    if (img[nextPoint.y*width + nextPoint.x] < threshold) {
			        result.add(nextPoint);
			        currentPoint = nextPoint;
			        currentDirection = currentDirection.opposite();
			        break;
			    }
			    currentDirection.nextCounterClockwise();
			}
		} while (currentPoint.equals(p0) == false);
		
		return result;
	}
	
}
