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

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import net.refractions.chyf.datasource.FlowDirection;

/**
 * Represents an edge or point along the boundary of the aoi
 * that is shared with a waterbody feature.
 * 
 * @author Emily
 *
 */
public class BoundaryEdge {

	private FlowDirection direction;
	private LineString ls;
	private Point inout;
	
	/**
	 * 
	 * @param direction the direction
	 * @param ls the edge that intersects or null if its just a point
	 * @param inout the "point on boundary" of intersection or null if non found
	 */
	public BoundaryEdge(FlowDirection direction, LineString ls, Point inout) {
		this.ls = ls;
		this.direction = direction;
		this.inout = inout;
	}
	
	public FlowDirection getDirection() {
		return this.direction;
	}
	
	public LineString getLineString() {
		return this.ls;
	}
	
	public Point getInOut() {
		return this.inout;
	}
}
