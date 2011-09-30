package br.ufpb.lavid.cinecamera;

import java.util.LinkedList;
import java.util.Queue;

import android.graphics.Path;
import android.graphics.Point;

public class PixelBorder extends LinkedList<Point> {
	
	private static final long serialVersionUID = -1758135141695435052L;

	private static double vectorCos(Point p0, Point p1) {
		if (p1.y < p0.y) {
			Point tmp = p0;
			p0 = p1;
			p1 = tmp;
		}
		Point p2 = new Point(p0.x, 1 + p0.y);
		double dx1 = p1.x - p0.x;
	    double dy1 = p1.y - p0.y;
	    double dx2 = p2.x - p0.x;
	    double dy2 = p2.y - p0.y;
	    return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
	}
	
	private static final int MIN_POINTS = 5;
	public Path asPolygonalPath () {
		Path result = new Path();
		
		Queue<Point> recentPointsBuffer = new LinkedList<Point>();
		
		Point p0 = get(0);
		result.moveTo(p0.x, p0.y);
		int pointCount = 0;
		
		Point lastPoint = p0;
		double currentLineAngleCos = 0d;
		
		for (Point p : subList(1, size())) {
			++pointCount;
			if (pointCount <= MIN_POINTS) {
				if (pointCount >= MIN_POINTS/2) {
					recentPointsBuffer.add(p);
				}
				if (pointCount == MIN_POINTS) {
					currentLineAngleCos = vectorCos(lastPoint, p);
				}
			} else if (Math.abs(currentLineAngleCos - vectorCos(lastPoint, p)) > 0.05) {
				Point middlePoint = recentPointsBuffer.element();
				result.lineTo(middlePoint.x, middlePoint.y);
				lastPoint = p;
				pointCount = 0;
				recentPointsBuffer.clear();
			} else {
				recentPointsBuffer.poll();
				recentPointsBuffer.add(p);
			}
		}
		
		result.lineTo(p0.x, p0.y);
		
		return result;
	}
	
}
