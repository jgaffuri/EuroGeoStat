/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.base.distance;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CentroidDistance implements Distance<Feature> {

	public double get(Feature f1, Feature f2) {
		Geometry g1 = f1.getGeometry();
		Geometry g2 = f2.getGeometry();
		return g1.getCentroid().distance(g2.getCentroid());
	}

}
