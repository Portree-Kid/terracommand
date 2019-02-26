package de.keithpaterson.terracommand.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "oneDegreeBuckets")
public interface OneDegreeBucketRepository extends JpaRepository<OneDegreeBucket, Long> {

	OneDegreeBucket getByName(String name);
}
