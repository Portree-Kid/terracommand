package de.keithpaterson.terracommand.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "tiles")
public interface TileRepository extends JpaRepository<Tile, Long> {

	public Tile findByTileIndex(int tileIndex);
	
	@Query("SELECT t FROM Tile t WHERE t.bottomLeftLatitude <= :latitude AND t.topRightLatitude > :latitude "
			+ " AND t.bottomLeftLongitude <= :longitude AND t.topRightLongitude > :longitude")
	public Tile findByLocation(@Param("latitude") Double latitude, @Param("longitude") Double longitude);
}
