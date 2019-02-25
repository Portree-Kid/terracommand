package de.keithpaterson.terracommand;

import java.util.HashMap;

import de.keithpaterson.terracommand.entities.Tile;

public class IndexMap extends HashMap<Integer, Tile> {

	@Override
	public Tile get(Object key) {
		// TODO Auto-generated method stub
		return super.get(key);
	}

	@Override
	public Tile getOrDefault(Object key, Tile defaultValue) {
		// TODO Auto-generated method stub
		return super.getOrDefault(key, defaultValue);
	}
}
