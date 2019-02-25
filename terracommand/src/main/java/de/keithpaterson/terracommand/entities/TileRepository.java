package de.keithpaterson.terracommand.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "tiles")
public interface TileRepository extends JpaRepository<Tile, Long> {

	public Tile findByTileIndex(int tileIndex);
}
