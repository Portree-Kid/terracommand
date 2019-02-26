package de.keithpaterson.terracommand.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonView;

import de.keithpaterson.terracommand.Views;

@Entity
@Table(name = "island", indexes = { @Index(name = "id", columnList = "id", unique = true) })
public class Island extends ResourceSupport{
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;

	@JsonView(Views.Deep.class)
	@OneToMany(targetEntity = Tile.class, mappedBy = "island", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	List<Tile> tiles = new ArrayList<>();

	public Island() {
	}

	public List<Tile> getTiles() {
		return tiles;
	}

	public void setTiles(ArrayList<Tile> tiles) {
		this.tiles = tiles;
	}
	
	public void add(Tile tileIndex) {
		tileIndex.setIsland(this);
		tiles.add(tileIndex);
	}

	public boolean hasTiles() {
		return tiles.size()>0;
	}

	public Long getIslandId() {
		return id;
	}
	
	

}
