package com.cb.fts.sample;

import com.cb.fts.sample.entities.vo.FTSResult;
import com.cb.fts.sample.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class MovieSearchIntegrationTest extends SampleApplicationTests {

    @Autowired
    private MovieService movieService;

//
//    @Test
//    public void testSearch(){
//
//        List<SearchResult> movies = movieService.searchQuery("star trek");
//        printResults(movies);
//    }
//
//    @Test
//    public void testSearchMispelling(){
//
//        List<SearchResult> movies = movieService.searchQuery("start trek");
//        printResults(movies);
//    }

//    @Test
//    public void testMatchAllFields(){
//
//        List<SearchResult> movies = movieService.searchMatch1Field("book");
//        printResults(movies);
//    }



//    /*********************************************************************/
//
//
//    @Test
//    public void testSearchOriginalTitle(){
//
//        List<SearchResult> movies = movieService.searchMatch1Field("Star Trek");
//        printResults(movies);
//    }
//
//
//    /*********************************************************************/
//
//
//    @Test
//    public void testSearchOriginalTitleAndOverview(){
//
//        List<SearchResult> movies = movieService.searchMatch2Field("star trek");
//        printResults(movies);
//    }
//
//
//    @Test
//    public void testSearchOriginalTitleAndOverviewBoost(){
//
//        List<SearchResult> movies = movieService.searchMatch2Field("star trek");
//        printResults(movies);
//    }



    public void printResults(List<FTSResult> movies){
//        int counter = 1;
//
//        for(SearchResult result: movies) {
//            System.out.println("");
//            System.out.println("");
//            System.out.println(counter+") "+result.getMovie().getTitle());
//            printRow(result.getRow());
//            System.out.println("");
//            counter++;
//        }
    }

//    public void printRow(SearchQueryRow row){
//        String padding = "     ";
//        String doublePadding = padding+padding;
//        System.out.println(padding+"score: "+row.score());
//
//        System.out.println(padding+"Hit Locations: ");
//
//
//
//        for (HitLocation hitLocation: row.locations().getAll()) {
//            System.out.println(doublePadding+"field:"+ hitLocation.field() );
//            System.out.println(doublePadding+"term:"+ hitLocation.term() );
//            System.out.println(doublePadding+"fragment:"+ row.fragments().get(hitLocation.field()));
//            System.out.println(doublePadding+"-----------------------------");
//        }
//    }
}
