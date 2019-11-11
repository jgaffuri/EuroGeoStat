/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.algo;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.graph.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.graph.structure.Node;

/**
 * @author julien Gaffuri
 *
 */
public class NodeDisplacement {

	public static void moveTo(Node n, double x, double y) {
		if(n.getC().distance(new Coordinate(x,y))==0) return;

		//move position, updating the spatial index
		n.getGraph().removeFromSpatialIndex(n);
		n.getC().x = x;
		n.getC().y = y;
		n.getGraph().insertInSpatialIndex(n);

		//update faces geometries
		for(Face f : n.getFaces()) f.updateGeometry();

		//update edges coords
		//for(Edge e:getOutEdges()) e.coords[0]=getC();
		//for(Edge e:getInEdges()) e.coords[e.coords.length-1]=getC();
	}

}