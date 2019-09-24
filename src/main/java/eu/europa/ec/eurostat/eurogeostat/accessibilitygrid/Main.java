/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Polygon;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;
import org.opencarto.util.JTSGeomUtil;
import org.opencarto.util.ProjectionUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author julien Gaffuri
 *
 */
public class Main {
	private static Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {

		//example
		//https://krankenhausatlas.statistikportal.de/

		logger.info("Start");

		//create xkm grid
		String outPath = "C:/Users/gaffuju/Desktop/out/";
		int size= 5000000;
		double res = 10000;
		int epsg = 3035;

		logger.info("Start GT");
		gridSHP_GT(outPath+"grid_GT.shp", size, res, epsg);
		logger.info("Start OC");
		gridSHP_OC(outPath+"grid_OC.shp", size, res, epsg);

		logger.info("End");
	}




	public static void gridSHP_OC(String outFile, int size, double res, int epsg) {
		logger.info("Create objects in memory");
		Collection<Feature> fs = new ArrayList<Feature>();
		for(double x=0; x<size; x+=res)
			for(double y=0; y<size; y+=res) {
				Feature f = new Feature();
				f.setDefaultGeometry( JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y ) );
				f.setID( "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y );
				f.setAttribute("cellId", f.getID());
				fs.add(f);
			}
		logger.info("Save " + fs.size() + " cells");
		SHPUtil.saveSHP(fs, outFile, ProjectionUtil.getCRS(epsg));
	}



	public static void gridSHP_GT(String outFile, int size, double res, int epsg) throws Exception {

		logger.info("Create objects in memory");

		SimpleFeatureType type = DataUtilities.createType("Grid","the_geom:Polygon:srid="+epsg+",cellId:String,");
		//logger.info("TYPE:" + type);

		ArrayList<SimpleFeature> fs = new ArrayList<>();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

		for(double x=0; x<size; x+=res)
			for(double y=0; y<size; y+=res) {

				Polygon poly = JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y );
				String id = "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y;

				featureBuilder.add(poly);
				featureBuilder.add(id);
				SimpleFeature feature = featureBuilder.buildFeature(id);
				fs.add(feature);
			}


		logger.info("Save " + fs.size() + " cells");

		File newFile = new File(outFile);
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(type);

		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
		SimpleFeatureType shpType = featureSource.getSchema();

		System.out.println(shpType);
		System.out.println(type);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			SimpleFeatureCollection collection = new ListFeatureCollection(type, fs);
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			logger.warn(typeName + " does not support read/write access");
		}
	}


}
