package eu.europa.ec.eurostat.eurogeostat.algo.deformation.constraint;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GSimpleConstraint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.submicro.GSegment;

public class SegmentMaximalLength extends GSimpleConstraint {
	private static Logger logger = Logger.getLogger(SegmentMaximalLength.class.getName());

	private GSegment s;
	private double goalLength;

	public SegmentMaximalLength(GSegment s, double imp){
		this(s, imp, s.getIniLength());
	}

	public SegmentMaximalLength(GSegment s, double imp, double goalLength){
		super(s,imp);
		this.s = s;
		this.goalLength = goalLength;
	}

	@Override
	public Coordinate getDisplacement(GPoint p, double alpha) {
		double d,a,dx,dy;
		d = s.getPt1().getDistance(s.getPt2());
		if (d<this.goalLength) return new Coordinate(0,0);

		a = alpha*(this.goalLength-d)/d*0.5;
		dx = a*(s.getPt2().getX()-s.getPt1().getX());
		dy = a*(s.getPt2().getY()-s.getPt1().getY());
		if      (p==s.getPt1()) return new Coordinate(-dx, -dy);
		else if (p==s.getPt2()) return new Coordinate( dx,  dy);
		else {
			logger.severe("Error");
			return null;
		}
	}
}
