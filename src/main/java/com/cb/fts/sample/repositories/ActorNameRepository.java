package com.cb.fts.sample.repositories;


import com.cb.fts.sample.entities.ActorName;
import com.cb.fts.sample.entities.Movie;
import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@N1qlPrimaryIndexed
@Repository
public interface ActorNameRepository extends PagingAndSortingRepository<ActorName, String> {


}
