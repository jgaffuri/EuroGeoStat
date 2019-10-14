package eu.europa.ec.eurostat.eurogeostat.algo.triangulation;

import org.locationtech.jts.geom.Coordinate;

public class TPointFactoryImpl implements TPointFactory {

	public TPoint create(Coordinate c) {
		return new TPointImpl(c);
	}

}
