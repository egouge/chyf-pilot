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
package net.refractions.chyf.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.datasource.ChyfAngle;
import net.refractions.chyf.datasource.ChyfDataSource;
import net.refractions.chyf.datasource.ChyfDataSource.EfType;
import net.refractions.chyf.datasource.ChyfDataSource.RankType;

/**
 * Compute rank by looking at angles.
 * 
 * @author Emily
 *
 */
public class RankComputer {

	private ChyfAngle angleComputer;
	private ChyfDataSource source;
	
	private CoordinateReferenceSystem sourceCRS;
	
	public RankComputer(CoordinateReferenceSystem sourceCRS, ChyfDataSource source) {
		this.angleComputer = new ChyfAngle(sourceCRS);
		this.source = source;
		this.sourceCRS = sourceCRS;
	}
	
	public void computeRank(RGraph graph) throws Exception {
		for (RNode node : graph.getNodes()) {
			if (node.getOutDegree() > 1) {
				//compute primary/secondary
				computePrimarySecondary(node);
			}
		}
	}
	
	
	private void computePrimarySecondary(RNode node) throws Exception {
		HashMap<REdge, Double> angles = new HashMap<>();

		
		boolean inwb = true;
		for (REdge out : node.getOutEdges()) {
			if (out.getType() != EfType.SKELETON) inwb = false;
			
			double angle = 0;
			for (REdge in : node.getInEdges()) {
				double a = computeAngle(in.getToC(), node.getCoordinate(), out.getFromC());
				
				if (Math.PI - a < Math.PI - angle) {
					angle = (Math.PI-a);
				}
			}
			angles.put(out,  angle);
		}
		
		//if all angles are within 10degrees of each other then look for another way of computing rank
		Double minValue = angles.entrySet().stream().min(Map.Entry.comparingByValue()).get().getValue();
		Double maxValue = angles.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
		
		if ( inwb && (maxValue - minValue < 30*(Math.PI / 180.0))) {
			//compute channel width
			
			HashMap<REdge, Double> angles2 = new HashMap<>();

			//find waterbody that contains the node
			Polygon wb = null;
			FeatureId wbfid = null;
			Point nodep = ((new GeometryFactory())).createPoint(node.getCoordinate());
			
			try(SimpleFeatureReader reader = source.getWaterbodies(new ReferencedEnvelope(nodep.getEnvelopeInternal(), sourceCRS))){
				while(reader.hasNext()) {
					SimpleFeature sf = reader.next();
					Polygon p = ChyfDataSource.getPolygon(sf);
					if (p.intersects(nodep)) {
						wb = p;
						wbfid = sf.getIdentifier();
						break;
					}
				}
			}
			if (wb == null) {
				throw new Exception("waterbody could not be found for node: " + node.toString());
			}else {	
				List<LineString> wbparts = new ArrayList<>();
				wbparts.add(wb.getExteriorRing());
				for (int i = 0; i < wb.getNumInteriorRing();i ++) wbparts.add(wb.getInteriorRingN(i));
				
				List<LineString> tempparts = new ArrayList<>();
				
				//break waterbody parts where they intersect with another waterbody or the coastline
				try(SimpleFeatureReader reader = source.getWaterbodies(new ReferencedEnvelope(wb.getEnvelopeInternal(), sourceCRS))){
					while(reader.hasNext()) {
						SimpleFeature sf = reader.next();
						if (sf.getIdentifier().equals(wbfid)) continue;
						Polygon p = ChyfDataSource.getPolygon(sf);
						
						tempparts.clear();
						for (LineString l : wbparts) {
							if (l.getEnvelopeInternal().intersects(p.getEnvelopeInternal()) && l.intersects(p)) {
								Geometry g = l.difference(p);
								for (int i = 0; i < g.getNumGeometries(); i ++) {
									LineString ls = ((LineString)g.getGeometryN(i));
									if (!ls.isEmpty()) tempparts.add(ls);
								}	
							}else {
								tempparts.add(l);
							}
						}
						wbparts.clear();
						wbparts.addAll(tempparts);
						
					}
				}
				//do the some thing with the coastline
				try(SimpleFeatureReader reader = source.getCoastlines(new ReferencedEnvelope(wb.getEnvelopeInternal(), sourceCRS))){
					if (reader != null) {
						while(reader.hasNext()) {
							SimpleFeature sf = reader.next();
							LineString p = ChyfDataSource.getLineString(sf);
							
							tempparts.clear();
							for (LineString l : wbparts) {
								if (l.getEnvelopeInternal().intersects(p.getEnvelopeInternal())) {
									Geometry g = l.difference(p);
									for (int i = 0; i < g.getNumGeometries(); i ++) {
										LineString ls = ((LineString)g.getGeometryN(i));
										if (!ls.isEmpty()) tempparts.add(ls);
									}	
								}else {
									tempparts.add(l);
								}
							}
							wbparts.clear();
							wbparts.addAll(tempparts);
							
						}
					}
				}
				
			
			
				for (REdge out : node.getOutEdges()) {
				
					LineString ls = getOutGeometry(out);
					if (ls == null) continue; //line goes outside of waterbody
					
					//closest points on different rings
					double target = ls.getLength() / 2.0;
					LengthIndexedLine ll = new LengthIndexedLine(ls);
					Point center = wb.getFactory().createPoint( ll.extractPoint(target) );
					
					Coordinate p1 = null;
					LineString f1 = null;
					Double d1 = Double.MAX_VALUE;
						
					Coordinate p2 = null;
					LineString f2 = null;
					Double d2 = Double.MAX_VALUE;
					
					for (LineString lls : wbparts) {
						Double dt = lls.distance(center);
						if (dt < d1) {
							if (d1 < d2) {
								d2 = d1;
								p2 = p1;
								f2 = f1;
							}
							d1 = dt;
							p1 = DistanceOp.nearestPoints(lls,  center)[0];
							f1 = lls;
								
							
						}else if (dt < d2) {
							
							d2 = dt;
							p2 = DistanceOp.nearestPoints(lls,  center)[0];
							f2 = lls;
						}
					}
						
					if (f1 == null || f2 == null) {
						//single lake case
						angles2.put(out,angles.get(out));
						continue;
					}
					Coordinate[] closest =  DistanceOp.nearestPoints(f1,  ls);
					Coordinate[] closest2 =  DistanceOp.nearestPoints(f2,  ls);
					p1 = closest [0];
					p2 = closest2 [0];
						
					LineString item = wb.getFactory().createLineString(new Coordinate[] {p1,p2});
						
					Geometry g1 = item.difference(wb);
					if (g1.getLength() > item.getLength() * 0.1) {
						angles2.put(out,angles.get(out));
						continue;
					}
					
					System.out.println(item.toString());
						
					double width = -1*p1.distance(p2);
						
					angles2.put(out, width);
						
				}	
			}
			if (!angles2.isEmpty())  angles = angles2;
		}
		
		//find min angle and set others to secondary
		REdge e = angles.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
		
		for (REdge out : node.getOutEdges()) {
			if (out == e) continue;
			out.setRank(RankType.SECONDARY);
			RNode oout = out.getToNode();
			while(true) {
				if (oout.getOutDegree() == 1 && oout.getInDegree() == 1) {
					REdge e1 = oout.getOutEdges().get(0);
					e1.setRank(RankType.SECONDARY);
					oout = e1.getToNode();
				}else if (oout.getOutDegree() == 1) {
					int bcnt = 0;
					int ocnt = 0;
					for (REdge in : oout.getInEdges()) {
						if (in.getType() == EfType.BANK) {
							bcnt ++;
						}else {
							ocnt ++;
						}
					}
					if (bcnt >= 1 && ocnt == 1) {
						REdge e1 = oout.getOutEdges().get(0);
						e1.setRank(RankType.SECONDARY);
						oout = e1.getToNode();
					}else {
						break;
					}
				} else {
					break;
				}
				
			}
		}
		
	}
	
	private LineString getOutGeometry(REdge e) {
		List<LineString> mm = new ArrayList<>();
		mm.add(e.getLineString());
		
		while(true) {
			if (e.getToNode().getInDegree() == 1 && e.getToNode().getOutDegree() == 1) {
				if (e.getToNode().getInEdges().get(0).getType().equals(e.getToNode().getOutEdges().get(0).getType())) {
					break;
				}else {
					
					e = e.getToNode().getOutEdges().get(0);
					if (e.getType() != EfType.SKELETON) return null;
					mm.add(e.getLineString());
				}
			}else if (e.getToNode().getOutDegree() == 1) {
				//if one of those in edges is bank then continue else stop
				//we don't want to push through to a different waterbody
				int bcnt = 0;
				int ocnt = 0;
				for (REdge in : e.getToNode().getInEdges()) {
					if (in.getType() == EfType.BANK) {
						bcnt ++;
					}else {
						ocnt ++;
					}
				}
				if (bcnt >= 1 && ocnt == 1) {
					e = e.getToNode().getOutEdges().get(0);
					mm.add(e.getLineString());
				}else {
					break;
				} 
			}else {
				break;
			}
		}
		if (mm.size() == 1) return mm.get(0);
		
		List<Coordinate> cs = new ArrayList<>();
		cs.add(mm.get(0).getCoordinateN(0));
		for (LineString ls : mm) {
			for (int i = 1; i < ls.getCoordinates().length; i ++) {
				cs.add(ls.getCoordinateN(i));
			}
		}
		LineString lls = e.getLineString().getFactory().createLineString(cs.toArray(new Coordinate[cs.size()]));
//		System.out.println(lls.toText());
		return lls;
	}
	
	private double computeAngle(Coordinate c0, Coordinate c1, Coordinate c2) throws Exception{
		double a = angleComputer.angle(c0, c1, c2);
		if (a < 0) a += 2*Math.PI;
		if (a > Math.PI) a = (2*Math.PI) - a;
		return a;
	}
	
	
	public static void main(String[] args) {


		LineSegment seg = new LineSegment(0.0, 0.0, 1.0, 1.0);
		
		Coordinate c1 = seg.pointAlongOffset(0.5, 15);
		Coordinate c2 = seg.pointAlongOffset(0.5, -15);
		System.out.println("LINESTRING(" + seg.p0.x + " " +seg.p0.y + "," + seg.p1.x + " " +seg.p1.y + ")");
		System.out.println("POINT(" + c1.x + " " + c1.y + ")");
		System.out.println("POINT(" + c2.x + " " + c2.y + ")");
		
		LineSegment seg1 = new LineSegment(seg.pointAlong(0.5), c1);
		LineSegment seg2 = new LineSegment(seg.pointAlong(0.5), c2);
		
		for (LineSegment ls : new LineSegment[] {seg, seg1, seg2}) {
			System.out.println("LINESTRING(" + ls.p0.x + " " +ls.p0.y + "," + ls.p1.x + " " +ls.p1.y + ")");
		}
	}
}


//
//perpendicular linestring
//					Coordinate p0 = null;
//					Coordinate p1 = null;
//					
//					double d = 0;
//					double target = ls.getLength() / 2.0;
//					for (int i = 0; i < ls.getCoordinates().length-1; i++) {
//						double dis = ls.getCoordinateN(i+1).distance(ls.getCoordinateN(i));
//						
//						if ( d < target && dis + d > target ) {
//							p0 = ls.getCoordinateN(i);
//							p1 = ls.getCoordinateN(i+1);
//							break;
//						}
//						d += dis;
//					}
//					if (p0 == null || p1 == null) throw new IllegalStateException();
//					
//					LineSegment seg = new LineSegment(p0, p1);
//					
//					double mwidth = Math.sqrt(wb.getEnvelopeInternal().getWidth() * wb.getEnvelopeInternal().getWidth()  + wb.getEnvelopeInternal().getHeight() * wb.getEnvelopeInternal().getHeight() );
//					Coordinate c0 = seg.pointAlong(0.5);
//					Coordinate c1 = seg.pointAlongOffset(0.5, mwidth);
//					Coordinate c2 = seg.pointAlongOffset(0.5, -mwidth);
//
//					LineString seg1 = wb.getFactory().createLineString(new Coordinate[] {c0, c1});
//					LineString seg2 = wb.getFactory().createLineString(new Coordinate[] {c0, c2});
//					
//					List<LineString> items = new ArrayList<>();
//					items.add(wb.getExteriorRing());
//					for (int i = 0; i < wb.getNumInteriorRing();i ++) items.add(wb.getInteriorRingN(i));
//					
//					Double d2 = Double.MAX_VALUE;
//					Coordinate cout = null;
//					for (LineString ring : items) {
//						Geometry g = ring.intersection(seg1);
//						if (g.isEmpty()) continue;
//						for (int i = 0; i < g.getNumGeometries(); i ++) {
//							if (g.getGeometryN(i) instanceof Point) {
//								Point p = (Point) g.getGeometryN(i);
//								if (p.getCoordinate().distance(c0) < d2) {
//									d2 = p.getCoordinate().distance(c0);
//									cout = p.getCoordinate();
//								}
//							}
//						}
//						
//					}
//					d2 = Double.MAX_VALUE;
//					Coordinate cin = null;
//					for (LineString ring : items) {
//						Geometry g = ring.intersection(seg2);
//						if (g.isEmpty()) continue;
//						for (int i = 0; i < g.getNumGeometries(); i ++) {
//							if (g.getGeometryN(i) instanceof Point) {
//								Point p = (Point) g.getGeometryN(i);
//								if (p.getCoordinate().distance(c0) < d2) {
//									d2 = p.getCoordinate().distance(c0);
//									cin = p.getCoordinate();
//								}
//							}
//						}
//						
//					}
//					
//					System.out.println("LINESTRING(" + cin.x + " " + cin.y + "," + c0.x + " " + c0.y + ")");
//					System.out.println("LINESTRING(" + cout.x + " " + cout.y + "," + c0.x + " " + c0.y + ")");
//					
//					if (cin == null || cout == null) throw new IllegalStateException();
//					double width = cin.distance(c0) + cout.distance(c0);
//					angles.put(out, width);
