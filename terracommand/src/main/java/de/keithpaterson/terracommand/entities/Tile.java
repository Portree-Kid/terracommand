package de.keithpaterson.terracommand.entities;

import java.util.ArrayList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "tile", indexes = { @Index(name = "tileIndex", columnList = "tileIndex", unique = true) })
public class Tile  extends ResourceSupport {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	int tileIndex;

	// public Tile(TileIndex t) {
	// tileIndex = t.getIndex();
	// latitude = t.getLonLat().y;
	// longitude = t.getLonLat().x;
	// }

	@ManyToOne(cascade = CascadeType.ALL)
	Island island;

	/**y*/
	java.lang.Double latitude;

	/**x*/
	java.lang.Double longitude;

	private boolean terrain;

	public Tile() {
	}

	public Tile(int tileIndex) {
		this.tileIndex = tileIndex;
		updatePosition();
	}

	public Tile(java.awt.geom.Point2D.Double neighbour) {
		latitude = neighbour.y;
		longitude = neighbour.x;
		tileIndex = getTileIndex();
	}

//	public Tile(TileIndex t) {
//		tileIndex = t.getIndex();
//		latitude = t.getLonLat().y;
//		longitude = t.getLonLat().x;
//	}

	public void setTileIndex(int tileIndex) {
		this.tileIndex = tileIndex;
	}

	public java.lang.Double getLatitude() {
		return latitude;
	}

	public void setLatitude(java.lang.Double latitude) {
		this.latitude = latitude;
	}

	public java.lang.Double getLongitude() {
		return longitude;
	}

	public void setLongitude(java.lang.Double longitude) {
		this.longitude = longitude;
	}

	public Island getIsland() {
		return island;
	}

	public void setIsland(Island island) {
		this.island = island;
	}

	public double getTileWidth() {
		if (Math.abs(latitude) < 22)
			return 0.125;
		if (Math.abs(latitude) < 62)
			return 0.25;
		if (Math.abs(latitude) < 76)
			return 0.5;
		if (Math.abs(latitude) < 83)
			return 1;
		if (Math.abs(latitude) < 86)
			return 2;
		if (Math.abs(latitude) < 88)
			return 4;
		if (Math.abs(latitude) < 89)
			return 8;
		if (Math.abs(latitude) <= 90)
			return 360;
		return 0.125;
	}

	/**
	 * Returns a TileIndex for the given lat/lon
	 * 
	 * @see <a href=
	 *      "http://wiki.flightgear.org/Tile_Index_Scheme">http://wiki.flightgear.org/Tile_Index_Scheme</a>
	 * @param p (x lon, y lat)
	 * @return
	 */

	public int getTileIndex() {
		if (longitude > 180 || longitude < -180)
			throw new IllegalArgumentException( "" + longitude + " out of bounds ");
		if (latitude > 90 || latitude < -90)
			throw new IllegalArgumentException( "" + latitude + " out of bounds ");
		double baseY = Math.floor(latitude);
		int y = (int) ((latitude - baseY) * 8);
		double tileWidth = getTileWidth();
		double base_x = Math.floor(Math.floor(longitude / tileWidth) * tileWidth);
		if (base_x < -180)
			base_x = -180;
		double x = Math.floor((longitude - base_x) / tileWidth);
		int index = ((int) Math.floor(longitude) + 180) << 14;
		index += ((int) Math.floor(latitude) + 90) << 6;
		index += (y << 3);
		index += x;
		return index;
	}

	public void updatePosition() {
		int unpacking = tileIndex;
		longitude = (double) ((unpacking >>> 14) - 180);
		unpacking = unpacking - (unpacking & 0xffc000);
		latitude = (double) (unpacking >>> 6) - 90;
		unpacking = unpacking - (unpacking & 0xffff40);

		int yIndex = (unpacking >>> 3) & 0x07;
		latitude += yIndex * 0.125;
		unpacking = unpacking - (unpacking & 0xfffff8);
		int x = unpacking & 0x07;
		unpacking -= x;
		double width = getTileWidth();
		longitude += x * width;
		if (longitude > 180 || longitude < -180)
			throw new IllegalArgumentException( "" + longitude + " out of bounds ");
		if (latitude > 90 || latitude < -90)
			throw new IllegalArgumentException( "" + latitude + " out of bounds ");
	}

	public ArrayList<Integer> getNeighbours() {
		ArrayList<Integer> neighbours = new ArrayList<>();
		if (longitude > 180 || longitude < -180)
			throw new IllegalArgumentException( "" + longitude + " out of bounds ");
		if (latitude > 90 || latitude < -90)
			throw new IllegalArgumentException( "" + latitude + " out of bounds ");
		// Left/Right
		if (latitude < 90 && latitude > -90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(longitude, latitude);
			double width = new Tile(neighbour).getTileWidth();
			neighbour.x -= width;
			if (neighbour.x < -180)
				neighbour.x += 360;
			neighbours.add(new Tile(neighbour).getTileIndex());
			neighbour.x += width;
			neighbour.x += width;
			if (neighbour.x > 180)
				neighbour.x -= 360;
			neighbours.add(new Tile(neighbour).getTileIndex());
		}
		if (latitude < 90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(longitude, latitude);
			neighbour.y += 0.125;
			int tIndex = new Tile(neighbour).getTileIndex();
			double width = new Tile(neighbour).getTileWidth();
			neighbours.add(tIndex);
			if (width < getTileWidth()) {
				neighbour.x += width;
				tIndex = new Tile(neighbour).getTileIndex();
				neighbours.add(tIndex);
			}

		}
		if (latitude > -90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(longitude, latitude);
			neighbour.y -= 0.125;
			int tIndex = new Tile(neighbour).getTileIndex();
			double width = getTileWidth();
			neighbours.add(tIndex);
			if (width < getTileWidth()) {
				neighbour.x += width;
				tIndex = new Tile(neighbour).getTileIndex();
				neighbours.add(tIndex);
			}
		}
		return neighbours;
	}

	public boolean isTerrain() {
		return terrain;
	}

	public void setTerrain(boolean terrain) {
		this.terrain = terrain;
	}
}
