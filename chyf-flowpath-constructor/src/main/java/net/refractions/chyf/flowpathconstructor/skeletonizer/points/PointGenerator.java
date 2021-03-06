/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.flowpathconstructor.skeletonizer.points;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.linemerge.LineMerger;

import net.refractions.chyf.datasource.DirectionType;
import net.refractions.chyf.datasource.FlowDirection;
import net.refractions.chyf.flowpathconstructor.ChyfProperties;
import net.refractions.chyf.flowpathconstructor.ChyfProperties.Property;
import net.refractions.chyf.flowpathconstructor.datasource.FlowpathGeoPackageDataSource.NodeType;

/**
 * Generates input/output points for skeletonizer
 * 
 * @author Emily
 *
 */
public class PointGenerator {
	
	private ChyfProperties props;
	
	private List<ConstructionPoint> points;
	private Polygon workingWaterbody = null;
	private Set<Polygon> workingPolygons = new HashSet<>();
	private Set<InsertPoint> insertCoordinates = new HashSet<>();
	private List<BoundaryEdge> boundaries;
	private Set<Coordinate> interiorexteriortouches = new HashSet<>(); //points where interior touches exterior
	
	public PointGenerator(List<BoundaryEdge> boundaries, ChyfProperties props) {
		points = new ArrayList<>();		
		this.boundaries = boundaries;
		this.props = props;
	}
	
	public List<ConstructionPoint> getPoints(){
		return this.points;
	}
	
	public Collection<Polygon> getUpdatedPolygons(){
		ArrayList<Polygon> items = new ArrayList<>();
		if (PolygonInfo.isModified(workingWaterbody)) items.add(workingWaterbody);
		for (Polygon p : workingPolygons) if (PolygonInfo.isModified(p)) items.add(p);
		return items;
	}
	
	
	public void processPolygon(Polygon waterbodya, List<Polygon> touches, List<LineString> flowpaths) throws Exception {
		List<ConstructionPoint> wbpoints = new ArrayList<>();
	
		workingWaterbody = waterbodya;
		workingPolygons.clear();
		workingPolygons.addAll(touches);
		insertCoordinates.clear();
		interiorexteriortouches.clear();
		
		//set of coordinates where the interior
		//ring touches the exterior ring, do not use these as bank coordinates
		for (Coordinate c : workingWaterbody.getExteriorRing().getCoordinates()) {
			for (int i = 0; i < workingWaterbody.getNumInteriorRing(); i ++) {
				for (Coordinate cs : workingWaterbody.getInteriorRingN(i).getCoordinates()) {
					if (c.equals2D(cs)) {
						interiorexteriortouches.add(c);
					}
				}
			}
		}
				
		//boundary points
		//make points out of all boundary points
		for (BoundaryEdge be: boundaries) {
			if (be.getLineString() == null) {
				if (be.getInOut().getEnvelopeInternal().intersects(waterbodya.getEnvelopeInternal()) &&
						be.getInOut().intersects(waterbodya)) {
					ConstructionPoint cp = new ConstructionPoint(be.getInOut().getCoordinate(), NodeType.FLOWPATH, be.getDirection(), (PolygonInfo) workingWaterbody.getUserData());
					addPoint(wbpoints, cp);
				}
			}
		}
		
		if (flowpaths.isEmpty() && touches.isEmpty()) {
			for (ConstructionPoint cp : processIsolated(workingWaterbody) ) {
				addPoint(wbpoints,cp);
			}
			
		}else {
			//intersect the flowpaths with the exterior of the waterbody
			Set<Coordinate> inpoints = new HashSet<>();
			Set<Coordinate> outpoints = new HashSet<>();
			Set<Coordinate> unknownpoints = new HashSet<>();
			
			for (LineString ls : flowpaths) {
				if (ls.getUserData() == null || ((DirectionType)ls.getUserData()) == DirectionType.KNOWN ) {
					outpoints.add(ls.getCoordinateN(0));
					inpoints.add(ls.getCoordinateN(ls.getNumPoints() - 1));
				}else {
					unknownpoints.add(ls.getCoordinateN(0));
					unknownpoints.add(ls.getCoordinateN(ls.getNumPoints() - 1));
				}
			}
		
			//create in/out points for linestring intersections
			processLineString(workingWaterbody.getExteriorRing(), inpoints, outpoints, unknownpoints, wbpoints);
			for (int i = 0; i < workingWaterbody.getNumInteriorRing(); i ++) {
				processLineString(workingWaterbody.getInteriorRingN(i), inpoints, outpoints, unknownpoints, wbpoints);	
			}
			
			//processing waterbody intersections
			for (Polygon p : touches) {
				Collection<Geometry> intersections = getIntersection(p, workingWaterbody);
				if (intersections == null) continue;
				for (Geometry g : intersections) {
					if (g instanceof Point) {
						addPoint(wbpoints, new ConstructionPoint(((Point)g).getCoordinate(), NodeType.WATER, FlowDirection.UNKNOWN, (PolygonInfo) workingWaterbody.getUserData()));
					}else {
						List<Coordinate> items = new ArrayList<>();
						for (Coordinate n : ((LineString)g).getCoordinates()) items.add(n);
						addPoint(wbpoints, new ConstructionPoint(findMidpoint(items, true), NodeType.WATER, FlowDirection.UNKNOWN, (PolygonInfo) workingWaterbody.getUserData()));
					}
				}
			}
			
			for (BoundaryEdge b : boundaries) {
				if (b.getLineString() == null || b.getInOut() == null) continue;
				
				if (b.getLineString().getEnvelopeInternal().intersects(workingWaterbody.getEnvelopeInternal()) &&
						b.getLineString().intersects(workingWaterbody) && b.getInOut().intersects(workingWaterbody)) {
					//use the b.getinout point
					addPoint(wbpoints, new ConstructionPoint(b.getInOut().getCoordinate(), NodeType.WATER, b.getDirection(), (PolygonInfo) workingWaterbody.getUserData()));
					
				}
			}
		}
		
		//if there are only in nodes then we need to add an out node
		int incount = 0;
		int outcount = 0;
		int unknowncount = 0;
		for (ConstructionPoint p : wbpoints) {
			if (p.getDirection() == FlowDirection.INPUT) incount ++;
			if (p.getDirection() == FlowDirection.OUTPUT) outcount ++;
			if (p.getDirection() == FlowDirection.UNKNOWN) unknowncount ++;
		}
		if (outcount == 0 && incount > 0 && unknowncount == 0) {
			//list all the points from the  
			Coordinate c = findLongestMidPoint(workingWaterbody.getExteriorRing(), touches, wbpoints.stream().map(a->a.getCoordinate()).collect(Collectors.toSet()));
			addPoint(wbpoints, new ConstructionPoint(c, NodeType.TERMINAL, FlowDirection.OUTPUT, (PolygonInfo) workingWaterbody.getUserData()));
		}
		//if there are only outnodes then we need to add an in node
		if (incount == 0 && outcount > 0 && unknowncount == 0) {
			Coordinate c = findLongestMidPoint(workingWaterbody.getExteriorRing(), touches, wbpoints.stream().map(a->a.getCoordinate()).collect(Collectors.toSet()));
			addPoint(wbpoints, new ConstructionPoint(c, NodeType.HEADWATER, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
		}
		if (unknowncount == 1 && incount == 0 && outcount == 0) {
			Coordinate c = findLongestMidPoint(workingWaterbody.getExteriorRing(), touches, wbpoints.stream().map(a->a.getCoordinate()).collect(Collectors.toSet()));
			//TODO: incorrect classification; thought could be either headwater or terminal, but it is a degree1 node
			addPoint(wbpoints, new ConstructionPoint(c, NodeType.HEADWATER, FlowDirection.UNKNOWN, (PolygonInfo) workingWaterbody.getUserData()));
		}
		
		//add bank points, ensuring they are not added
		//at the waterbody intersections
		Set<ConstructionPoint> banks = addBanks(workingWaterbody, wbpoints, workingPolygons);
		for (ConstructionPoint cp : banks) {
			addPoint(wbpoints, cp);
		}
		
		//add all to point list
		points.addAll(wbpoints);
		
		//add verticies to all polygons as necessary
		addVerticies();
	}
	
	/*
	 * Adds the construction point to the set of points for the polygon,
	 * ensuring it doesn't already exists at that point.  If one
	 * already exists attempt to keep the correct one or else fail
	 * if two different types (id bank and flowpath);
	 * @param point
	 */
	private void addPoint(List<ConstructionPoint> addto, ConstructionPoint point) throws Exception{
		ConstructionPoint found = null;
		for (ConstructionPoint cp : addto) {
			if (cp.getCoordinate().equals2D(point.getCoordinate())) {
				found = cp;
				break;
			}
		}
		if (found == null) {
			addto.add(point);
			return;
		}
		
		//if they are of similar types
		if (found.getType() == point.getType()) {
			if (found.getDirection() == point.getDirection()) return;
			if (found.getDirection() == FlowDirection.UNKNOWN) {
				points.remove(found);
				addto.add(point);
				return;
			}
			if ( (found.getDirection() == FlowDirection.INPUT && point.getDirection() == FlowDirection.OUTPUT) ||
					 (found.getDirection() == FlowDirection.OUTPUT && point.getDirection() == FlowDirection.INPUT) ) {
				throw new Exception("Cannot create both an inflow and outflow construction point at the same coordinate: " + point.getCoordinate());
			}
		}else {
			if (found.getType() == NodeType.BANK || point.getType() == NodeType.BANK) {
				//throw and exception cannot create bank and flowpath node at the same coordinate
				throw new Exception("Cannot create bank and flow construction point at the same coordinate: " + point.getCoordinate().toString());
			}
			if (found.getDirection() == point.getDirection()) return;
			if (found.getDirection() == FlowDirection.UNKNOWN) {
				points.remove(found);
				addto.add(point);
				return;
			}
			if ( (found.getDirection() == FlowDirection.INPUT && point.getDirection() == FlowDirection.OUTPUT) ||
					 (found.getDirection() == FlowDirection.OUTPUT && point.getDirection() == FlowDirection.INPUT) ) {
				throw new Exception("Cannot create both an inflow and outflow construction point at the same coordinate: " + point.getCoordinate());
			}
		}
	}
	
	/**
	 * Intersects the two polygons and returns the linestring or points
	 * where they overlap. 
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 * @throws Exception 
	 */
	private Collection<Geometry> getIntersection(Polygon p1, Polygon p2) throws Exception {
		Geometry g = p1.intersection(p2);
		if (g == null) return null;
		if (g.getNumGeometries() == 0) return null;
		
		ArrayList<Geometry> items = new ArrayList<>();
		LineMerger m = new LineMerger();
		for (int i = 0; i < g.getNumGeometries(); i ++) {
			Geometry t = g.getGeometryN(i);
			if (t.isEmpty()) continue;
			if (t instanceof Point) {
				items.add(t);
			}else if ( t instanceof LineString) {
				m.add(t);
			}else {
				throw new Exception("Invalid intersection of polygons.  Intersection does not form point or linestring (check that the polygons are noded correctly).  Centroids of offending polygons: " + p1.getCentroid().toText() + " " + p2.getCentroid().toText());
			}
		}
		items.addAll(m.getMergedLineStrings());
		
		//for the case where an interior ring touches the boundary
		//at a single point
		int i = 0;
		List<LineString> interiors = new ArrayList<>();
		for (int j = 0; j < p1.getNumInteriorRing(); j ++) interiors.add(p1.getInteriorRingN(j));
		for (int j = 0; j < p2.getNumInteriorRing(); j ++) interiors.add(p2.getInteriorRingN(j));
		while (i < items.size()) {
			Geometry t = items.get(i);
			i++;
			
			if (t instanceof Point) continue;
			LineString ls = (LineString)t;
			
			for (LineString ls2 : interiors) {
				//does a coordinate on this interior ring match a coordinate on the linestring??
				Coordinate[] cc = ls2.getCoordinates();
				
				for (int k = 1; k < ls.getCoordinates().length-1; k ++) {
					boolean found = false;
					for (int l = 0; l < cc.length; l ++) {
						if (ls.getCoordinates()[k].equals2D(cc[l])) {
							found = true;
							break;
						}
					}
					if (found) {
						//split linestring; reset counter and continue
						LineString l1 = (new  GeometryFactory()).createLineString(Arrays.copyOfRange(ls.getCoordinates(), 0, k+1));
						LineString l2 = (new  GeometryFactory()).createLineString(Arrays.copyOfRange(ls.getCoordinates(), k, ls.getCoordinates().length));
						items.remove(ls);
						items.add(l1);
						items.add(l2);
						i = 0;
						continue;
					}
				}
			}
			
		}
		return items;
	}

	/**
	 * Finds the midpoint of the longest segment broken at the points
	 * 
	 * Used for adding terminal or headwater points if required
	 * 
	 * @param ls
	 * @param points
	 * @throws Exception 
	 */
	private Coordinate findLongestMidPoint(LineString ls, List<Polygon> touches, Set<Coordinate> points) throws Exception {
		
		
		//first remove any shared edges from this linestring
		Geometry temp = ls;
		for (Polygon p : touches) {
			if (!p.getExteriorRing().getEnvelopeInternal().intersects(temp.getEnvelopeInternal())) continue;
			temp = temp.difference(p.getExteriorRing());
		}
		for (BoundaryEdge g : boundaries) {
			if (g.getLineString() == null) continue;
			if (!g.getLineString().getEnvelopeInternal().intersects(temp.getEnvelopeInternal())) continue;
			temp = temp.difference(g.getLineString());
		}
		
		if (temp instanceof MultiLineString) {
			LineMerger lm = new LineMerger();
			lm.add(temp);
			Collection<?> items = lm.getMergedLineStrings();
			if (items.size() > 1) throw new RuntimeException("cannot find longest midpoint -> single linestring not generated for finding midpoint");
			temp = (Geometry) items.iterator().next();
		}
		//if its a linestring
		if (!(temp instanceof LineString)) {
			throw new RuntimeException("cannot find longest midpoing -> intersection does not form a linestring");
		}
		
		ls = (LineString)temp;

		HashMap<Double, Coordinate> options = new HashMap<>();
		Coordinate[] cs = ls.getCoordinates();
		int start = 0;
		for(int i = 0; i < cs.length; i ++) {
			if (points.contains(cs[i])) {
				start = i;
				break;
			}
		}
		
		int cnt = (start+1) % (cs.length - 1);
		double distance = 0;
		List<Coordinate> coords = new ArrayList<>();
		coords.add(ls.getCoordinateN(start));
		while(cnt != start) {
			Coordinate n = ls.getCoordinateN(cnt);
			coords.add(n);
			distance += n.distance(coords.get(coords.size() - 1));
			
			if (points.contains(n)) {
				//find the midpoint between first and cnt and create node
				options.put(distance, findMidpoint(coords, false));
				coords = new ArrayList<>();
				coords.add(n);
				distance = 0;
			}
			cnt = (cnt+1) % (ls.getCoordinates().length - 1);
		}
		//find midpoint between first and cnt and create node
		options.put(distance, findMidpoint(coords, false));
	
		return options.get(options.keySet().stream().reduce(Math::max).get());
	}
	
	
	private void processLineString(LineString ls, Set<Coordinate> inpoints, Set<Coordinate> outpoints, Set<Coordinate> unknown,List<ConstructionPoint> points ) throws Exception {
		for (int i = 0; i < ls.getCoordinates().length-1; i ++) {
			Coordinate c = ls.getCoordinateN(i);
			if (outpoints.contains(c)) {
				addPoint(points, new ConstructionPoint(c, NodeType.FLOWPATH, FlowDirection.OUTPUT, (PolygonInfo) workingWaterbody.getUserData()));
			}else if (inpoints.contains(c)) {
				addPoint(points, new ConstructionPoint(c, NodeType.FLOWPATH, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
			}else if (unknown.contains(c)) {
				addPoint(points, new ConstructionPoint(c, NodeType.FLOWPATH, FlowDirection.UNKNOWN, (PolygonInfo) workingWaterbody.getUserData()));
			}
		}
	}
	
	/**
	 * Add Bank point
	 * @param waterbody the polygon to add banks to
	 * @param flowpoints in/out points
	 * @param donotuse set of points to not use for bank flows
	 * @return
	 * @throws Exception 
	 */
	private Set<ConstructionPoint> addBanks(Polygon waterbody, List<ConstructionPoint> flowpoints,  Set<Polygon> otherpolys) throws Exception{
		if (flowpoints.isEmpty()) return Collections.emptySet();

		
		
		Set<ConstructionPoint> banks = new HashSet<>();
		
		Set<Coordinate> flows = new HashSet<>();
		boolean hasheadwater = false;
		for(ConstructionPoint p : flowpoints) {
			flows.add(p.getCoordinate());
			if (p.getType() == NodeType.HEADWATER) hasheadwater = true;
			if (p.getType() == NodeType.TERMINAL) hasheadwater = true;
		}
				
		List<LineString> toprocess = new ArrayList<>();
		toprocess.add(waterbody.getExteriorRing());
		waterbody.getExteriorRing().setUserData(hasheadwater);
		
		for (int i = 0; i < waterbody.getNumInteriorRing(); i ++) {
			toprocess.add(waterbody.getInteriorRingN(i));
			waterbody.getInteriorRingN(i).setUserData(false);
		}
		
		for (LineString ls : toprocess) {
			Geometry temp = ls;
			for (Polygon p : otherpolys) {
				if (!p.getExteriorRing().getEnvelopeInternal().intersects(temp.getEnvelopeInternal())) continue;
				temp = temp.difference(p.getExteriorRing());
			}
			for (BoundaryEdge g : boundaries) {
				if (g.getLineString() == null) continue;
				if (!g.getLineString().getEnvelopeInternal().intersects(temp.getEnvelopeInternal())) continue;
				temp = temp.difference(g.getLineString());
			}
			List<LineString> items = new ArrayList<>();
			if (temp instanceof LineString) {
				items.add((LineString)temp);
			}else if (temp instanceof MultiLineString) {
				for (int i = 0; i < temp.getNumGeometries(); i ++) {
					items.add((LineString)temp.getGeometryN(i));
				}
			}else {
				throw new RuntimeException("Invalid geometry type");
			}

			LineMerger merger = new LineMerger();
			merger.add(items);
			Collection<LineString> items2 = merger.getMergedLineStrings();
			
			for (LineString i : items2) {
				generateBanks(i, flows, (boolean)ls.getUserData(), banks);
			}
		}

		return banks;
	}
	
	/**
	 * Generate bank flowpaths
	 * 
	 * @param the linestring representing the bank edit
	 * @param flows set of coordinates where flows touch point
	 * @param hasheadwater
	 * @param interiorexteriortouch coordinates where interior rings touch exterior ring 
	 * @param banks bank constructions point list to update
	 * 
	 */
	private void generateBanks(LineString ls, Set<Coordinate> flows, boolean hasheadwater,  Set<ConstructionPoint> banks) throws Exception{
		Coordinate[] cs = ls.getCoordinates();
		
		if (cs[0].equals2D(cs[cs.length - 1])) {  //linear ring
			int start = 0;
			//ring start at first in/out point
			for(int i = 0; i < cs.length; i ++) {
				if (flows.contains(cs[i])) {
					start = i;
					break;
				}
			}
			int cnt = (start+1) % (cs.length - 1);
			
			List<Coordinate> coords = new ArrayList<>();
			coords.add(cs[start]);
			
			while(cnt != start) {
				Coordinate n = cs[cnt];
				coords.add(n);
				if (flows.contains(n)) {
					//find the midpoint between first and cnt and create node
					Coordinate bankPnt = findMidpoint(coords, false);
					bankPnt = findCoordinate(coords, bankPnt);
					banks.add(new ConstructionPoint(bankPnt,NodeType.BANK, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
					coords = new ArrayList<>();
					coords.add(n);
				}
				cnt = (cnt+1) % (cs.length - 1);
			}
			//find midpoint between first and cnt and create node
			coords.add(cs[start]);
			if (coords.size() > 1 && !hasheadwater) banks.add(new ConstructionPoint(findMidpoint(coords, false),NodeType.BANK, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
		}else {
			List<Coordinate> coords = new ArrayList<>();
			for (int i = 0; i < cs.length; i ++) {
				coords.add(cs[i]);
				if (flows.contains(cs[i]) && i != 0) {
					Coordinate bankPnt = findMidpoint(coords, false);
					bankPnt = findCoordinate(coords, bankPnt);
					banks.add(new ConstructionPoint(bankPnt,NodeType.BANK, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
					coords.clear();
					coords.add(cs[i]);	
					
				}
			}
			if (!hasheadwater && coords.size() > 1) banks.add(new ConstructionPoint(findMidpoint(coords, false),NodeType.BANK, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
		}
	}
	
	/*
	 * if given bankpnt is in the set of int/ext then 
	 * use a point halfway along linesegment formed
	 * by previous coordinate
	 */
	private Coordinate findCoordinate(List<Coordinate> coords, Coordinate bankPnt) {
		if (!interiorexteriortouches.contains(bankPnt)) return bankPnt;
	
		int index = coords.indexOf(bankPnt);
		int previndex = index-1;
		
		LineSegment seg = new LineSegment(coords.get(previndex), coords.get(index));
		Coordinate c = seg.pointAlong(0.5);
		insertCoordinates.add(new InsertPoint(coords.get(previndex), coords.get(index), c));
		return c;
	}
	
	
	
	/**
	 * Find the mid-point of the set of coordinates. If one of the coordinates
	 * is already flagged as an in/out point it will return this coordinate. 
	 * Otherwise this will return an existing coordinate if the mid-point is "NEAR" the
	 * middle of the coordinates, otherwise it will return a new coordinate
	 * 
	 * @param interpolate if false will always return an existing coordinate (unless there are
	 * only two coordinate in which case it will add a new one).  if true then if
	 * the existing coordinate is more then a given distance from the centerpoint
	 * it will add a new coordinate at the centerpoint
	 */
	private Coordinate findMidpoint(List<Coordinate> items, boolean interpolate) throws Exception{		
		if (items.size() == 0) {
			throw new Exception("Cannot find the midpoint when zero coordinates computed");
		}
		if (items.size() == 1) {
			throw new Exception("Cannot find the midpoint of a single coordinate. Likely data error near: (" + items.get(0).x + " " + items.get(0).y + ")");
		}
		
		//if there is already a point on this line, reuse the exact same point
		//for banks we don't want to use existing points
		for (int i = 1; i < items.size() - 2; i ++) {
			for (ConstructionPoint p : points) {
				if (p.getCoordinate().equals2D(items.get(i)))  return items.get(i);
			}
		}
		
		double vertexDistance = props.getProperty(Property.PNT_VERTEX_DISTANCE);

		InsertPoint ipnt = findMidPoint(items,  vertexDistance, interpolate);
		if (ipnt == null) return null;
		if (ipnt.before != null) { 
			insertCoordinates.add(ipnt);
		}
		return ipnt.toinsert;

	}
	
	/**
	 * Finds the midpoint of a line represented by a list
	 * of coordinates.  If the midpoint is within vertexDistance
	 * of an existing vertex the existing vertex will be used. If
	 * interpolate is true then a new vertex will be added at midpoint
	 * otherwise an existing vertex will always be used (except in cases
	 * where there are only two points).
	 * 
	 * @param items
	 * @param vertexDistance
	 * @param interpolate
	 */
	public static InsertPoint findMidPoint(List<Coordinate> items, double vertexDistance, boolean interpolate) {
		
		if (items.size() == 2) {
			//we are going to have to add another vertex to waterbody
			Coordinate c = LineSegment.midPoint(items.get(0),  items.get(1));
			return new InsertPoint(items.get(0), items.get(1), c);
		}
		
		//if mid coordinate is not near the center in terms of distance
		//then add a vertex note: vertex will need to be added to all waterbodies here
		double length = 0;
		for (int i = 1; i < items.size(); i ++) {
			length += items.get(i).distance(items.get(i-1));
		}
		double target = length / 2.0;
		
		double distance = 0;
		for (int i = 1; i < items.size(); i ++) {
			double lastdistance = distance;
			distance += items.get(i-1).distance(items.get(i));
			
			if (distance > target) {
				//stop here
				if (target - lastdistance < distance - target) {
					if (interpolate && (target-lastdistance) > vertexDistance) {
						//compute a coordinate nearest to the center
						LineSegment seg = new LineSegment(items.get(i-1), items.get(i));
						Coordinate c = seg.pointAlong((target-lastdistance) / seg.getLength());
						return new InsertPoint(items.get(i-1), items.get(i), c);
					}else {
						return new InsertPoint(null, null, new Coordinate(items.get(i-1)));
					}
				}else {
					if (interpolate &&  distance - target > vertexDistance) {
						//add new
						LineSegment seg = new LineSegment(items.get(i-1), items.get(i));
						Coordinate c = seg.pointAlong( 1 - ((distance-target) / seg.getLength()));
						return new InsertPoint(items.get(i-1), items.get(i), c);
					}else {
						return new InsertPoint(null, null, new Coordinate(items.get(i)));
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Process an isolated waterbody.  Adds in/out points
	 * at approximately opposite ends of the polygon
	 * 
	 * @param waterbody
	 * @return
	 * @throws Exception 
	 */
	private List<ConstructionPoint> processIsolated(Polygon waterbody) throws Exception {
		
		Coordinate[] cs = waterbody.getExteriorRing().getCoordinates();
		
		double target = waterbody.getExteriorRing().getLength() / 2.0;
		
		Coordinate start = cs[0];
		if (interiorexteriortouches.contains(start)) {
			if (interiorexteriortouches.contains(cs[1])) {
				LineSegment seg = new LineSegment(cs[0], cs[1]);
				start = seg.pointAlong(0.5);
				insertCoordinates.add(new InsertPoint(cs[0], cs[1], start));
			}else {
				start = cs[1];
			}			
		}
		
		double distance = 0;
		int index = -1;
		for (int i = 1; i < cs.length; i ++) {
			double lastdistance = distance;
			distance += cs[i-1].distance(cs[i]);
			if (distance > target) {
				//stop here
				if (target - lastdistance < distance - target) {
					index = i-1;
				}else {
					index = i;
				}
				break;		
			}
		}
		Coordinate end = cs[index];
		if (interiorexteriortouches.contains(end)) {
			if (interiorexteriortouches.contains(cs[index-1])) {
				LineSegment seg = new LineSegment(cs[index-1], cs[index]);
				end = seg.pointAlong(0.5);
				insertCoordinates.add(new InsertPoint(cs[index-1], cs[index], start));
			}else {
				end = cs[index];
			}			
		}
		
		if (end.equals2D(start)) throw new Exception("Cannot generated constructions points for isolated waterbody: " + workingWaterbody.getCentroid().toText());
		
		List<ConstructionPoint> points = new ArrayList<>();
		points.add(new ConstructionPoint(start, NodeType.HEADWATER, FlowDirection.INPUT, (PolygonInfo) workingWaterbody.getUserData()));
		points.add(new ConstructionPoint(end, NodeType.TERMINAL, FlowDirection.OUTPUT, (PolygonInfo) workingWaterbody.getUserData()));
		return points;
	}
	
	/**
	 * If the coordinate intersects one of the working polygons but there
	 * is no vertex at this point then add a vertex 
	 * @param c
	 */
	private void addVerticies() {
		workingWaterbody = addVertex(workingWaterbody, insertCoordinates);
		
		HashSet<Polygon> newwb = new HashSet<>();
		for (Polygon p : workingPolygons) {
			newwb.add(addVertex(p, insertCoordinates));
		}
		this.workingPolygons = newwb;
		insertCoordinates.clear();
	}
	
	public static Polygon addVertex(Polygon p, Collection<InsertPoint> insertPoints) {
		boolean modified = false;
		 LineString[] in = new LineString[p.getNumInteriorRing()+1];
		 in[0] = p.getExteriorRing();
		 for (int i = 0; i < p.getNumInteriorRing(); i ++) {
			 in[i+1] = p.getInteriorRingN(i);
		 }
		
		 LineString[] out = new LineString[in.length];
		 
		 for (int i = 0; i < in.length; i ++) {
	 		 out[i] = in[i];
	 		 
	 		Coordinate[] cs = in[i].getCoordinates();
	 		List<Coordinate> newCoordinates = new ArrayList<>();
	 		newCoordinates.add(cs[0]);
			 for (int k = 1; k < cs.length; k ++) {
				 
				 for (InsertPoint pnts : insertPoints) {
					 Coordinate c0 = pnts.before;
					 Coordinate c1 = pnts.after;
	 				 if (cs[k-1].equals2D(c0) && cs[k].equals2D(c1)) {
	 					 //insert pnts[2]
	 					newCoordinates.add(pnts.toinsert);
	 					modified = true;
	 				 }
	 				if (cs[k-1].equals2D(c1) && cs[k].equals2D(c0)) {
	 					newCoordinates.add(pnts.toinsert);
	 					modified = true;
	 				}
	 			 }
				newCoordinates.add(cs[k]);
	 		 }
	 		 if (cs.length != newCoordinates.size()) {
	 			 out[i] = p.getFactory().createLineString(newCoordinates.toArray(new Coordinate[newCoordinates.size()]));
	 		 }
		 }
		 if (!modified) return p;
		 
		 GeometryFactory gf = p.getFactory();
		 LinearRing[] rings = new LinearRing[out.length];
		 for (int i = 0; i < out.length; i ++) {
			 rings[i] = gf.createLinearRing(out[i].getCoordinates());
		 }
		 Polygon newp = p.getFactory().createPolygon(rings[0], Arrays.copyOfRange(rings, 1, rings.length));
		 newp.setUserData(p.getUserData());
		 PolygonInfo.setModified(newp, true);
		 return newp;
	}
	
	/**
	 * class for tracking points to add to waterbodies 
	 * @author Emily
	 *
	 */
	public static class InsertPoint {
		Coordinate before;
		Coordinate after;
		Coordinate toinsert;
		
		public InsertPoint(Coordinate before, Coordinate after, Coordinate toinsert) {
			this.before = before;
			this.after = after;
			this.toinsert = toinsert;
			
			try { toinsert.setZ(Double.NaN); }catch (Throwable t) {}
			try { toinsert.setM(Double.NaN); }catch (Throwable t) {}			
		}
	}
}
