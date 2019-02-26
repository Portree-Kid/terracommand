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
	java.lang.Double bottomLeftLatitude;

	/**x*/
	java.lang.Double bottomLeftLongitude;

	private boolean terrain;

	private double topRightLatitude;

	private double topRightLongitude;
	
	@ManyToOne(cascade = CascadeType.ALL)
	private TenDegreeBucket tenDegreeBucket;

	@ManyToOne(cascade = CascadeType.ALL)
	private OneDegreeBucket oneDegreeBucket;
	

	public Tile() {
	}

	public Tile(int tileIndex) {
		this.tileIndex = tileIndex;
		updatePosition();
	}

	public Tile(java.awt.geom.Point2D.Double neighbour) {
		bottomLeftLatitude = neighbour.y;
		bottomLeftLongitude = neighbour.x;
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
		return bottomLeftLatitude;
	}

	public void setLatitude(java.lang.Double latitude) {
		this.bottomLeftLatitude = latitude;
	}

	public java.lang.Double getLongitude() {
		return bottomLeftLongitude;
	}

	public void setLongitude(java.lang.Double longitude) {
		this.bottomLeftLongitude = longitude;
	}

	public Island getIsland() {
		return island;
	}

	public void setIsland(Island island) {
		this.island = island;
	}

	public double getTileWidth() {
		if (Math.abs(bottomLeftLatitude) < 22)
			return 0.125;
		if (Math.abs(bottomLeftLatitude) < 62)
			return 0.25;
		if (Math.abs(bottomLeftLatitude) < 76)
			return 0.5;
		if (Math.abs(bottomLeftLatitude) < 83)
			return 1;
		if (Math.abs(bottomLeftLatitude) < 86)
			return 2;
		if (Math.abs(bottomLeftLatitude) < 88)
			return 4;
		if (Math.abs(bottomLeftLatitude) < 89)
			return 8;
		if (Math.abs(bottomLeftLatitude) <= 90)
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
		if (bottomLeftLongitude > 180 || bottomLeftLongitude < -180)
			throw new IllegalArgumentException( "" + bottomLeftLongitude + " out of bounds ");
		if (bottomLeftLatitude > 90 || bottomLeftLatitude < -90)
			throw new IllegalArgumentException( "" + bottomLeftLatitude + " out of bounds ");
		double baseY = Math.floor(bottomLeftLatitude);
		int y = (int) ((bottomLeftLatitude - baseY) * 8);
		double tileWidth = getTileWidth();
		double base_x = Math.floor(Math.floor(bottomLeftLongitude / tileWidth) * tileWidth);
		if (base_x < -180)
			base_x = -180;
		double x = Math.floor((bottomLeftLongitude - base_x) / tileWidth);
		int index = ((int) Math.floor(bottomLeftLongitude) + 180) << 14;
		index += ((int) Math.floor(bottomLeftLatitude) + 90) << 6;
		index += (y << 3);
		index += x;
		return index;
	}

	public void updatePosition() {
		int unpacking = tileIndex;
		bottomLeftLongitude = (double) ((unpacking >>> 14) - 180);
		unpacking = unpacking - (unpacking & 0xffc000);
		bottomLeftLatitude = (double) (unpacking >>> 6) - 90;
		unpacking = unpacking - (unpacking & 0xffff40);

		int yIndex = (unpacking >>> 3) & 0x07;
		bottomLeftLatitude += yIndex * 0.125;
		unpacking = unpacking - (unpacking & 0xfffff8);
		int x = unpacking & 0x07;
		unpacking -= x;
		double width = getTileWidth();
		bottomLeftLongitude += x * width;
		if (bottomLeftLongitude > 180 || bottomLeftLongitude < -180)
			throw new IllegalArgumentException( "" + bottomLeftLongitude + " out of bounds ");
		if (bottomLeftLatitude > 90 || bottomLeftLatitude < -90)
			throw new IllegalArgumentException( "" + bottomLeftLatitude + " out of bounds ");
		setTopRightLongitude(bottomLeftLongitude + width);
		setTopRightLatitude(bottomLeftLatitude + 0.125);
	}

	public ArrayList<Integer> getNeighbours() {
		ArrayList<Integer> neighbours = new ArrayList<>();
		if (bottomLeftLongitude > 180 || bottomLeftLongitude < -180)
			throw new IllegalArgumentException( "" + bottomLeftLongitude + " out of bounds ");
		if (bottomLeftLatitude > 90 || bottomLeftLatitude < -90)
			throw new IllegalArgumentException( "" + bottomLeftLatitude + " out of bounds ");
		// Left/Right
		if (bottomLeftLatitude < 90 && bottomLeftLatitude > -90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(bottomLeftLongitude, bottomLeftLatitude);
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
		if (bottomLeftLatitude < 90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(bottomLeftLongitude, bottomLeftLatitude);
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
		if (bottomLeftLatitude > -90) {
			java.awt.geom.Point2D.Double neighbour = new java.awt.geom.Point2D.Double(bottomLeftLongitude, bottomLeftLatitude);
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

	public double getTopRightLatitude() {
		return topRightLatitude;
	}

	public void setTopRightLatitude(double topRightLatitude) {
		this.topRightLatitude = topRightLatitude;
	}

	public double getTopRightLongitude() {
		return topRightLongitude;
	}

	public void setTopRightLongitude(double topRightLongitude) {
		this.topRightLongitude = topRightLongitude;
	}

	public TenDegreeBucket getTenDegreeBucket() {
		return tenDegreeBucket;
	}

	public void setTenDegreeBucket(TenDegreeBucket tenDegreeBucket) {
		this.tenDegreeBucket = tenDegreeBucket;
	}

	public OneDegreeBucket getOneDegreeBucket() {
		return oneDegreeBucket;
	}

	public void setOneDegreeBucket(OneDegreeBucket oneDegreeBucket) {
		this.oneDegreeBucket = oneDegreeBucket;
	}
}
