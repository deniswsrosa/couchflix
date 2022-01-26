package com.cb.fts.sample;


import com.cb.fts.sample.entities.Actor;
import com.cb.fts.sample.entities.Movie;
import com.cb.fts.sample.repositories.MovieRepository;
import com.couchbase.client.java.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PosterPathRunner implements CommandLineRunner {

    @Autowired
    private Cluster cluster;

    @Autowired
    private MovieRepository movieRepository;

    @Override
    public void run(String... strings) throws Exception {


//        List<Movie> movies = movieRepository.findAllWithOldCover();
//
//        System.out.println("==============Retornou todooosssss");
//
//        int count = 0;
//        for(Movie movie: movies) {
//
//            count++;
//
//
//            if(movie.getCast() == null) {
//                movie.setCast(new ArrayList<>());
//            }
//
//            if(movie.getCast().size() >= 5) {
//                movie.setCastAdjusted(movie.getCast().subList(0, 5));
//            } else {
//
//                movie.setCastAdjusted(movie.getCast());
//                for(int i=0; i < 5; i++) {
//                    if(movie.getCastAdjusted().size() < 5) {
//                        movie.getCastAdjusted().add(getDefaultActor());
//                    }
//                }
//            }
//            movieRepository.save(movie);


//            movie.setPosterPath(movie.getPosterPath().replaceAll("\"", ""));
//            movie.setPathUpdated(true);
//                movieRepository.save(movie);
//
////            try (java.io.InputStream is = new java.net.URL("https://api.themoviedb.org/3/movie/"+movie.getId().replace("key::", "")+"?api_key=2342fca7e5f78044b2c8f4a60b0e3363").openStream()) {
////                String contents = new String(is.readAllBytes());
////                com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(contents);
////
////                movie.setPosterPath(element.getAsJsonObject().get("poster_path").toString());
////                movie.setPathUpdated(true);
////                movieRepository.save(movie);
////                count++;
////            } catch (Exception e) {
////                System.out.println("============skipped = "+movie.getId().replace("key::", ""));
////                movie.setPathUpdated(true);
////                movieRepository.save(movie);
////            }

//            if(count %10 == 0){
//                System.out.println(count);
//            }
//        }



    }

    private Actor getDefaultActor() {

        Actor actor = new Actor();
        actor.setName("Some actor");
        actor.setCharacter("Some charachter");
        return  actor;
    }
}
