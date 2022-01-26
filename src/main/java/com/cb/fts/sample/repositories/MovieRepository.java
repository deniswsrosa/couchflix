package com.cb.fts.sample.repositories;

import com.cb.fts.sample.entities.Movie;
import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@N1qlPrimaryIndexed
@Repository
public interface MovieRepository extends PagingAndSortingRepository<Movie, String> {

    @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and castAdjusted is missing limit 10000")
    List<Movie> findAllWithOldCover();
}
