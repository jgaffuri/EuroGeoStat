/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.graph;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.algo.base.AffineTransformUtil;
import eu.europa.ec.eurostat.jgiscotools.datamodel.graph.Edge;
import eu.europa.ec.eurostat.jgiscotools.datamodel.graph.Face;
import eu.europa.ec.eurostat.jgiscotools.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class FaceScaling {
	private final static Logger LOGGER = Logger.getLogger(FaceScaling.class.getName());

	//scale a face
	public static void scale(Face f, double factor) {
		if(factor == 1) return;

		if(f.getGeom() == null) {
			LOGGER.error("Null geometry found for face "+f.getId());
			return;
		}

		//get center
		Coordinate center = f.getGeom().getCentroid().getCoordinate();

		//remove all edges from spatial index
		boolean b;
		for(Edge e : f.getEdges()){
			b = f.getGraph().removeFromSpatialIndex(e);
			if(!b) LOGGER.error("Could not remove edge from spatial index when scaling face");
		}

		//scale edges' internal coordinates
		for(Edge e : f.getEdges()){
			for(Coordinate c : e.getCoords()){
				if(c==e.getN1().getC()) continue;
				if(c==e.getN2().getC()) continue;
				AffineTransformUtil.applyScaling(c,center,factor);
			}
		}

		//scale nodes coordinates
		for(Node n : f.getNodes())
			AffineTransformUtil.applyScaling(n.getC(),center,factor);

		//add edges to spatial index with new geometry
		for(Edge e : f.getEdges())
			f.getGraph().insertInSpatialIndex(e);

		//force geometry update
		f.updateGeometry();
		for(Face f_ : f.getTouchingFaces())
			f_.updateGeometry();
	}

}