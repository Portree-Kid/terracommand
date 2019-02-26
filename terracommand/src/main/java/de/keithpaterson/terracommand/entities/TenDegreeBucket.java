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
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonView;

import de.keithpaterson.terracommand.Views;

@Entity
@Table(name = "tendegreebucket", indexes = { @Index(name = "id", columnList = "id", unique = true), @Index(name = "name", columnList = "name", unique = true) })
public class TenDegreeBucket {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	long id;
	@Pattern(message = "Wrong Bucketname", regexp = "(e|w)[0-9]{3}(n|s)[0-9]{2}")	
	private String name;
	
	@JsonView(Views.Deep.class)
	@OneToMany(targetEntity = Tile.class, mappedBy = "tenDegreeBucket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	List<Tile> tiles = new ArrayList<>();

	public TenDegreeBucket() {
	}
	
	public TenDegreeBucket(String calcName) {
		setName(calcName);
	}

	public static String calcName(Double longitude, Double latitude) {
	    char ew = (longitude < 0)?'w':'e';
	    char ns = (latitude < 0)?'s':'n';
	    if( longitude < 0 && longitude > -1)
	    	longitude -= 1;
	    if( latitude < 0 && latitude > -1)
	    	latitude -= 1;

	    String ret = String.format("%c%03d%c%02d", ew, (int)(Math.abs(Math.floor(longitude/ 10)*10)), ns, (int)(Math.abs(Math.floor(latitude/10)*10)));
		return ret;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
