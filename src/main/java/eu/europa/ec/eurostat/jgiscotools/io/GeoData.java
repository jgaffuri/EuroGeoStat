/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil.CRSType;

/**
 * Some generic function to load data from mainstream data formats: gpkg, shp, geojson.
 * 
 * @author julien Gaffuri
 *
 */
public class GeoData {
	private final static Logger LOGGER = LogManager.getLogger(GeoData.class);

	//TODO handle additional formats? WKT/WKB?

	private String filePath;
	private Filter filter;
	private File file = null;
	private String format;

	/**
	 * Build a GeoData from a file.
	 * 
	 * @param filePath
	 */
	public GeoData(String filePath) { this(filePath, null); }

	/**
	 * Build a GeoData from a file.
	 * 
	 * @param filePath
	 * @param filter
	 */
	public GeoData(String filePath, Filter filter) {
		this.filePath = filePath;
		this.filter = filter;
		this.file = new File(filePath);
		if(!this.file.exists()) {
			LOGGER.error("Data source: " + filePath + " not found.");
			return;
		}
		this.format = FilenameUtils.getExtension(filePath).toLowerCase();
		if(!"shp".equals(this.format) && !"geojson".equals(this.format) && !"gpkg".equals(this.format)) {
			LOGGER.error("Unsupported data format '" + this.format + "' for data source:" + filePath + " not found.");
			return;
		}
	}


	private SimpleFeatureType schema = null;

	/**
	 * @return The schema
	 */
	public SimpleFeatureType getSchema() {
		if(schema == null) 
			switch(format) {
			case "shp":
				try {
					this.schema = FileDataStoreFinder.getDataStore(this.file).getSchema();
				} catch (Exception e) { e.printStackTrace(); }
				break;
			case "geojson":
				try {
					InputStream input = new FileInputStream(new File(filePath));
					this.schema = new FeatureJSON().readFeatureCollectionSchema(input, true);
					input.close();
				} catch (Exception e) { e.printStackTrace(); }
				break;
			case "gpkg":
				try {
					HashMap<String, Object> params = new HashMap<>();
					params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
					params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
					DataStore store = DataStoreFinder.getDataStore(params);
					String[] names = store.getTypeNames();
					if(names.length >1 )
						LOGGER.warn("Several types found in GPKG " + filePath + ". Only " + names[0] + " will be considered.");
					String name = names[0];
					LOGGER.debug(name);
					this.schema = store.getSchema(name);
					store.dispose();
				} catch (IOException e) { e.printStackTrace(); }
				break;
			default:
				LOGGER.error("Could not retrieve schema from data source: " + filePath);
			}
		return schema;
	}

	private ArrayList<Feature> features = null;

	/**
	 * @return The feature
	 */
	public ArrayList<Feature> getFeatures() {
		if(features == null) 
			switch(format) {
			case "shp":
				try {
					FileDataStore store = FileDataStoreFinder.getDataStore(this.file);
					SimpleFeatureCollection features = filter==null? store.getFeatureSource().getFeatures() : store.getFeatureSource().getFeatures(filter);
					store.dispose();
					this.features = SimpleFeatureUtil.get(features, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case "geojson":
				try {
					InputStream input = new FileInputStream(new File(filePath));
					SimpleFeatureCollection fc = (SimpleFeatureCollection) new FeatureJSON().readFeatureCollection(input);
					if(this.filter == null)
						this.features = SimpleFeatureUtil.get(fc, "id");
					else {
						this.features = new ArrayList<Feature>();
						for(Feature f : SimpleFeatureUtil.get(fc, "id"))
							if(this.filter.evaluate(f)) this.features.add(f);
					}
					//remove 'geometry' attribute
					for(Feature f : this.features) {
						Object o = f.getAttributes().remove("geometry");
						if(o == null) LOGGER.warn("Could not remove geometry attribute when loading GeoJSON data.");
					}

					input.close();
				} catch (Exception e) { e.printStackTrace(); }
				break;
			case "gpkg":
				try {
					HashMap<String, Object> params = new HashMap<>();
					params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
					params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
					DataStore store = DataStoreFinder.getDataStore(params);
					String[] names = store.getTypeNames();
					if(names.length >1 )
						LOGGER.warn("Several types found in GPKG " + filePath + ". Only " + names[0] + " will be considered.");
					String name = names[0];
					LOGGER.debug(name);
					SimpleFeatureCollection sfc = filter==null? store.getFeatureSource(name).getFeatures() : store.getFeatureSource(name).getFeatures(filter);
					this.schema = store.getSchema(name);
					this.features = SimpleFeatureUtil.get(sfc, null);
					store.dispose();
				} catch (Exception e) { e.printStackTrace(); }
				break;
			default:
				LOGGER.error("Could not retrieve features from data source: " + filePath);
			}
		return features;
	}


	/**
	 * @return The coordinate reference system
	 */
	public CoordinateReferenceSystem getCRS() {
		return getSchema().getCoordinateReferenceSystem();
	}

	/**
	 * @return The coordinate reference system type
	 */
	public CRSType getCRSType() {
		return ProjectionUtil.getCRSType(getCRS());
	}









	/**
	 * Get features
	 * 
	 * @param filePath
	 * @return
	 */
	public static ArrayList<Feature> getFeatures(String filePath)  {
		return new GeoData(filePath).getFeatures();
	}

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @param filter 
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath, Filter filter)  {
		return new GeoData(filePath, filter).getFeatures();
	}

	/**
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureType getSchema(String filePath) {
		return new GeoData(filePath).getSchema();
	}

	/**
	 * @param filePath
	 * @return
	 */
	public static CoordinateReferenceSystem getCRS(String filePath) {
		return getSchema(filePath).getCoordinateReferenceSystem();
	}



	/**
	 * @param fs
	 * @param filePath
	 * @param crs
	 */
	public static void save(Collection<Feature> fs, String filePath, CoordinateReferenceSystem crs) {
		List<String> atts = null;
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(fs.iterator().next(), crs, atts);
		SimpleFeatureCollection sfc = SimpleFeatureUtil.get(fs, ft);
		if(sfc.size() == 0){
			//file.createNewFile();
			LOGGER.warn("Could not save file "+filePath+" - collection of features is empty");
			return;
		}

		//create output file
		File file = FileUtil.getFile(filePath, true, true);

		String format = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(format) {
		case "shp":
			try {
				//create feature store
				HashMap<String, Serializable> params = new HashMap<String, Serializable>();
				params.put("url", file.toURI().toURL());
				params.put("create spatial index", Boolean.TRUE);
				ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

				ds.createSchema(sfc.getSchema());
				SimpleFeatureStore fst = (SimpleFeatureStore)ds.getFeatureSource(ds.getTypeNames()[0]);

				//creation transaction
				Transaction tr = new DefaultTransaction("create");
				fst.setTransaction(tr);
				try {
					fst.addFeatures(sfc);
					tr.commit();
				} catch (Exception e) {
					e.printStackTrace();
					tr.rollback();
				} finally {
					tr.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case "geojson":
			GeoJSONUtil.save(fs, filePath, crs);
			break;
		case "gpkg":
			try {
				//create feature store
				HashMap<String, Serializable> params = new HashMap<String, Serializable>();
				params.put("url", file.toURI().toURL());
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, filePath);
				params.put("create spatial index", Boolean.TRUE);
				DataStore ds = DataStoreFinder.getDataStore(params);

				ds.createSchema(sfc.getSchema());
				SimpleFeatureStore fst = (SimpleFeatureStore)ds.getFeatureSource(ds.getTypeNames()[0]);

				//creation transaction
				Transaction tr = new DefaultTransaction("create");
				fst.setTransaction(tr);
				try {
					fst.addFeatures(sfc);
					tr.commit();
				} catch (Exception e) {
					e.printStackTrace();
					tr.rollback();
				} finally {
					tr.close();
					ds.dispose();
				}
			} catch (IOException e) { e.printStackTrace(); }
			break;
		default:
			LOGGER.error("Unsuported output format: " + format);
		}
	}

	public static <T extends Geometry> void saveGeoms(Collection<T> geoms, String outFile, CoordinateReferenceSystem crs) {
		save(SimpleFeatureUtil.getFeaturesFromGeometries(geoms), outFile, crs);
	}

}
