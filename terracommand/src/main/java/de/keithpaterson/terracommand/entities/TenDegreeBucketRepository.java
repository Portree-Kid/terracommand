package de.keithpaterson.terracommand.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "tenDegreeBuckets")
public interface TenDegreeBucketRepository extends JpaRepository<TenDegreeBucket, Long> {
	TenDegreeBucket getByName(String name);

}
