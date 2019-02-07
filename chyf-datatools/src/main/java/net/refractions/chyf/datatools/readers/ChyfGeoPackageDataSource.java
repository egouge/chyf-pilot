package net.refractions.chyf.datatools.readers;

import java.io.IOException;
import java.nio.file.Path;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a geopackage input dataset.  
 * 
 * @author Emily
 *
 */
public class ChyfGeoPackageDataSource implements ChyfDataSource{

	static final Logger logger = LoggerFactory.getLogger(ChyfDataSource.class.getCanonicalName());
	
	private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

	public static final String FLOWPATH_LAYER = "Flowpath";
	public static final String CATCHMENT_LAYER = "Catchment";
	public static final String WATERBODY_LAYER = "Waterbody";
	public static final String BOUNDARY_LAYER = "Working_limit";
	
	private Path geopackageFile ;
	
	private FeatureEntry waterbody;
	private FeatureEntry catchments;
	private FeatureEntry flowpaths;
	
	private GeoPackage reader;
	
	public ChyfGeoPackageDataSource(Path geopackageFile) throws Exception {
		this.geopackageFile = geopackageFile;
		read();
		
	}
	
	public Path getFile() {
		return this.geopackageFile;
	}
	private void read() throws Exception {
			
		reader = new GeoPackage(geopackageFile.toFile());
		
	    logger.info("Reading waterbodies");
	    waterbody = reader.feature(WATERBODY_LAYER);
		if (waterbody == null) throw new Exception("No waterbody layer found in geopackage file");
		
		logger.info("Reading catchments");
		catchments = reader.feature(CATCHMENT_LAYER);
		if (catchments == null) throw new Exception("No Catchment layer found in geopackage file");
		
		logger.info("Reading flowpaths");
		flowpaths = reader.feature(FLOWPATH_LAYER);
		if (flowpaths == null) throw new Exception("No Flowpath layer found in geopackage file");	
	}
	
	public SimpleFeatureReader getECatchments(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, catchments);
	}
	
	public SimpleFeatureReader getWaterbodies(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, waterbody);
	}
	
	public SimpleFeatureReader getFlowpaths(ReferencedEnvelope bounds) throws IOException{
		return query(bounds, flowpaths);
	}
	
	private SimpleFeatureReader query(ReferencedEnvelope bounds, FeatureEntry source) throws IOException {
		String geom = source.getGeometryColumn();
		Filter filter1 = ff.bbox(ff.property(geom), bounds);
		Filter filter2 = ff.intersects(ff.property(geom), ff.literal(JTS.toGeometry(bounds)));
        Filter filter = ff.and(filter1, filter2);
	        
        return reader.reader(source, filter, null);
		
	}

	
	public ReferencedEnvelope getCatchmentBounds() throws IOException{
		return catchments.getBounds();
	}

	@Override
	public void close() throws Exception {
		reader.close();
	}
	
}
