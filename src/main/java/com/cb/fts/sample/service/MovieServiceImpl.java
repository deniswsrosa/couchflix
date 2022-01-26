package com.cb.fts.sample.service;

import com.cb.fts.sample.entities.vo.*;
import com.cb.fts.sample.repositories.MovieRepository;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.search.SearchOptions;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.queries.*;
import com.couchbase.client.java.search.result.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cb.fts.sample.service.EntityExtractor.EntityType.GENRES;
import static com.cb.fts.sample.service.EntityExtractor.EntityType.PERSON;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieQueryParser movieQueryParser;


    @Autowired
    private Cluster cluster;

    private static final String indexName = "movies_all_index";
    private static final String movies_shingle = "movies_shingle";

    private DisjunctionQuery getDisjunction(String words, String field) {
        MatchQuery query = SearchQuery.match(words).field(field);
        MatchQuery queryFuzzy = SearchQuery.match(words).field(field).fuzziness(1);

        return SearchQuery.disjuncts(query, queryFuzzy);
    }

    @Override
    public Result searchQuery(String phrase, String filters) {
        Map<String,List<String>> facets = getFilters(filters);
        return search1(phrase);
    }


    private Result search1(String word){
        QueryStringQuery simpleQuery = SearchQuery.queryString(word);
        SearchResult result = cluster.searchQuery(indexName, simpleQuery,
                SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }


    private Result search2(String word){
        DisjunctionQuery title = getDisjunction(word, "title");
        SearchResult result = cluster.searchQuery(indexName, title, SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }


    private Result search3(String word){
        DisjunctionQuery title = getDisjunction(word, "title");
        DisjunctionQuery overview = getDisjunction(word, "overview");
        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle");
        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);

        SearchResult result = cluster.searchQuery(indexName, fts, SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }


    private Result search4(String word){

        DisjunctionQuery title = getDisjunction(word, "title");
        DisjunctionQuery overview = getDisjunction(word, "overview");
        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle");

        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);

        SearchResult result = cluster.searchQuery(movies_shingle, fts, SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }


    private Result search5(String word){
        DisjunctionQuery title = getDisjunction(word, "title", 1.4);
        DisjunctionQuery overview = getDisjunction(word, "overview");
        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle", 1.1);

        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);
        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(fts, boostReleaseYearQuery());

        SearchResult result = cluster.searchQuery(movies_shingle, conjunctionQuery, SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }



    private Result search6(String word){

        DisjunctionQuery title = getDisjunction(word, "title", 1.4);
        //Overview is making movies too relevant
        DisjunctionQuery overview = getDisjunction(word, "overview", -4);
        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle", 1.1);

        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);

        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(fts,
                boostReleaseYearQuery(),
                boostWeightedRating(),
                boostPromoted(),
                boostPopularity(),
                boostRuntime());

        SearchResult result = cluster.searchQuery(movies_shingle, conjunctionQuery, SearchOptions.searchOptions().limit(30).highlight());
        return  getSearchResults(result);
    }


    private Result search7(String word, Map<String, List<String>> facets){
        DisjunctionQuery title = getDisjunction(word, "title", 1.4);
        DisjunctionQuery overview = getDisjunction(word, "overview");
        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle", 1.1);

        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);


        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(fts,
                boostReleaseYearQuery(),
                boostWeightedRating(),
                boostPromoted(),
                boostPopularity(),
                boostRuntime());

        if(!facets.isEmpty()) {
            conjunctionQuery= addFacetFilters( conjunctionQuery, facets);
        }

        HashMap<String, SearchFacet> facetsDef = new HashMap<>();
        facetsDef.put("genres", SearchFacet.term("genres.name", 10));

        SearchResult result = cluster.searchQuery(movies_shingle, conjunctionQuery,
                SearchOptions.searchOptions().limit(30).highlight()
                        .facets(facetsDef));
        return  getSearchResults(result);
    }

    private Result search8(String words, Map<String, List<String>> facets){
        EntityExtractor entityExtractor = movieQueryParser.parse(words);

        DisjunctionQuery ftsQuery = new DisjunctionQuery();
        if(entityExtractor.getWords().trim().length() >0) {
            ftsQuery.or(getDisjunction(entityExtractor.getWords(), "title", 1.4));
            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "originalTitle", 1.1));
            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "overview"));
        }

        DisjunctionQuery actors = getActorsDisjunctionAdjusted(words, entityExtractor);
        if(actors!= null) {
            ftsQuery.or(actors);
        }


        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(ftsQuery,
                boostReleaseYearQuery(),
                boostRuntime(),
                boostPromoted(),
                boostWeightedRating(),
                boostPopularity());

        if(entityExtractor.getEntities().containsKey(GENRES)) {
            addFilters(conjunctionQuery, "genres.name", entityExtractor.getEntities().get(GENRES));
        }

        if(!facets.isEmpty()) {
            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
        }


        System.out.println("===================================================");
        System.out.println(entityExtractor);
        System.out.println("===================================================");

        HashMap<String, SearchFacet> facetsDef = new HashMap<>();
        facetsDef.put("genres", SearchFacet.term("genres.name", 10));

        SearchResult result = cluster.searchQuery(movies_shingle, conjunctionQuery,
                SearchOptions.searchOptions().limit(30).highlight()
                        .facets(facetsDef)

        );
        return  getSearchResults(result);
    }
//
//
//    private Result search9(String word, Map<String, List<String>> facets){
//        String indexName = "movies_shingle";
//        DisjunctionQuery title = getDisjunction(word, "title", 1.4);
//        DisjunctionQuery overview = getDisjunction(word, "overview");
//        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle", 1.1);
//
//        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle);
//
//        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(fts,
//                boostReleaseYearQuery(),
//                boostPopularity(),
//                boostPromoted(),
//                boostRuntime(),
//                boostWeightedRating()
//        );
//
//        if(!facets.isEmpty()) {
//            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
//        }
//
//        SearchQuery query = new SearchQuery(indexName, conjunctionQuery).highlight().limit(30);
//        query.addFacet("genres", SearchFacet.term("genres.name", 10));
//        SearchQueryResult result = movieRepository.getCouchbaseOperations()
//                .getCouchbaseBucket()
//                .query(query);
//        return  getSearchResults(result);
//    }
//
//
//    private Result search10(String word, Map<String, List<String>> facets){
//        String indexName = "movies_shingle";
//        DisjunctionQuery title = getDisjunction(word, "title", 1.4);
//        DisjunctionQuery overview = getDisjunction(word, "overview");
//        DisjunctionQuery originalTitle = getDisjunction(word, "originalTitle", 1.1);
//
//        DisjunctionQuery fts = SearchQuery.disjuncts(title, overview, originalTitle,
//                getActorsDisjunction(word)
//                );
//
//        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(fts,
//                boostReleaseYearQuery(),
//                boostPopularity(),
//                boostPromoted(),
//                boostRuntime(),
//                boostWeightedRating()
//        );
//
//        if(!facets.isEmpty()) {
//            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
//        }
//
//        SearchQuery query = new SearchQuery(indexName, conjunctionQuery).highlight().limit(30);
//        query.addFacet("genres", SearchFacet.term("genres.name", 10));
//        SearchQueryResult result = movieRepository.getCouchbaseOperations()
//                .getCouchbaseBucket()
//                .query(query);
//        return  getSearchResults(result);
//    }
//
//
//
//    private Result search11(String words, Map<String, List<String>> facets){
//        String indexName = "movies_shingle";
//
//        EntityExtractor entityExtractor = movieQueryParser.parse(words);
//
//        DisjunctionQuery ftsQuery = new DisjunctionQuery();
//        if(entityExtractor.getWords().trim().length() >0) {
//            ftsQuery.or(getDisjunction(entityExtractor.getWords(), "title", 1.4));
//            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "originalTitle", 1.15));
//            ftsQuery.or(getDisjunction(entityExtractor.getWords(), "collection.name", 1.1));
//            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "overview"));
//        }
//
//        DisjunctionQuery actors = getActorsDisjunction(words, entityExtractor);
//        if(actors!= null) {
//            ftsQuery.or(actors);
//        }
//
//
//        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(ftsQuery,
//                boostReleaseYearQuery(),
//                boostRuntime(),
//                boostPromoted(),
//                boostWeightedRating(),
//                boostPopularity());
//
//        if(entityExtractor.getEntities().containsKey(GENRES)) {
//            addFilters(conjunctionQuery, "genres.name", entityExtractor.getEntities().get(GENRES));
//        }
//
//        if(!facets.isEmpty()) {
//            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
//        }
//
//
//        System.out.println("===================================================");
//        System.out.println(entityExtractor);
//        System.out.println("===================================================");
//
//        SearchQuery searchQuery =  new SearchQuery(indexName, conjunctionQuery).highlight().limit(20);
//        searchQuery.addFacet("genres",  SearchFacet.term("genres.name", 10));
//        SearchQueryResult result = movieRepository.getCouchbaseOperations().getCouchbaseBucket().query(searchQuery);
//        return getSearchResults(result);
//    }
//
//
//
//    private Result search12(String words, Map<String, List<String>> facets){
//        String indexName = "movies_shingle";
//
//        EntityExtractor entityExtractor = movieQueryParser.parse(words);
//
//        DisjunctionQuery ftsQuery = new DisjunctionQuery();
//        if(entityExtractor.getWords().trim().length() >0) {
//            ftsQuery.or(getDisjunction(entityExtractor.getWords(), "title", 1.4));
//            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "originalTitle", 1.15));
//            ftsQuery.or(getDisjunction(entityExtractor.getWords(), "collection.name", 1.1));
//            ftsQuery.or( getDisjunction(entityExtractor.getWords(), "overview"));
//        }
//
//        DisjunctionQuery actors = getActorsDisjunctionAdjusted(words, entityExtractor);
//        if(actors!= null) {
//            ftsQuery.or(actors);
//        }
//
//
//        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(ftsQuery,
//                boostReleaseYearQuery(),
//                boostRuntime(),
//                boostPromoted(),
//                boostWeightedRating(),
//                boostPopularity());
//
//        if(entityExtractor.getEntities().containsKey(GENRES)) {
//            addFilters(conjunctionQuery, "genres.name", entityExtractor.getEntities().get(GENRES));
//        }
//
//        if(!facets.isEmpty()) {
//            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
//        }
//
//
//        System.out.println("===================================================");
//        System.out.println(entityExtractor);
//        System.out.println("===================================================");
//
//        SearchQuery searchQuery =  new SearchQuery(indexName, conjunctionQuery).highlight().limit(20);
//        searchQuery.addFacet("genres",  SearchFacet.term("genres.name", 10));
//        SearchQueryResult result = movieRepository.getCouchbaseOperations().getCouchbaseBucket().query(searchQuery);
//        return getSearchResults(result);
//    }










    private DisjunctionQuery boostPromoted() {
        BooleanFieldQuery promotedQuery = SearchQuery.booleanField(true).field("promoted").boost(1.5);
        BooleanFieldQuery notPromotedQuery = SearchQuery.booleanField(false).field("promoted").boost(1);
        return SearchQuery.disjuncts(promotedQuery, notPromotedQuery);
    }







    private ConjunctionQuery addFacetFilters(ConjunctionQuery conjunctionQuery , Map<String, List<String>> facets) {

        for(Map.Entry<String, List<String>> entry: facets.entrySet()) {
            if("genres".equals(entry.getKey())) {
                addFilters(conjunctionQuery, "genres.name", entry.getValue());
            } else if("collection".equals(entry.getKey())) {
                addFilters(conjunctionQuery, "collection.name", entry.getValue());
            } else if("year".equals(entry.getKey())) {
                addFilters(conjunctionQuery, "release_year", entry.getValue());
            }
        }
        return conjunctionQuery;
    }

    private void addFilters(ConjunctionQuery conjunctionQuery, String fieldName, List<String> values) {
        if(values.size() == 1) {
            conjunctionQuery.and(SearchQuery.term(values.get(0)).field(fieldName));
        } if(values.size() > 1) {
            DisjunctionQuery disjunctionQuery = SearchQuery.disjuncts();
            for(String genre : values) {
                disjunctionQuery.or(SearchQuery.term(genre).field(fieldName));
            }
            conjunctionQuery.and(disjunctionQuery);
        }
    }


    private DisjunctionQuery getActorsDisjunction(String words) {
            DisjunctionQuery castQuery = getDisjunction(words, "cast.name", 1.15);
            MatchQuery character = SearchQuery.match(words).field("cast.character");
            return SearchQuery.disjuncts(castQuery, character);

    }

    private DisjunctionQuery getCrewDisjunction(String words) {
            DisjunctionQuery nameQuery = getDisjunction(words, "crew.name", 1.15);
            MatchQuery jobQuery = SearchQuery.match(words).boost(1.1).field("crew.job");
        return SearchQuery.disjuncts(nameQuery, jobQuery);

    }


    private DisjunctionQuery getActorsDisjunction(String words, EntityExtractor entityExtractor) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getDisjunction(names.get(0), "cast.name", 1.5 );
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getDisjunction(name, "cast.name", 1.5 ));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            DisjunctionQuery castQuery = getDisjunction(words, "cast.name", 1.15);
            MatchQuery character = SearchQuery.match(words).field("cast.character");
            return SearchQuery.disjuncts(castQuery, character);
        }
    }

    private DisjunctionQuery getActorsDisjunctionAdjusted(String words, EntityExtractor entityExtractor) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getDisjunction(names.get(0), "castAdjusted.name", 1.5 );
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getDisjunction(name, "castAdjusted.name", 1.5 ));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            DisjunctionQuery castQuery = getDisjunction(words, "castAdjusted.name", 1.15);
            MatchQuery character = SearchQuery.match(words).field("castAdjusted.character");
            return SearchQuery.disjuncts(castQuery, character);
        }
    }

    private DisjunctionQuery getCrewDisjunction(String words, EntityExtractor entityExtractor) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getDisjunction(names.get(0), "crew.name", 1.4);
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getDisjunction(name, "crew.name", 1.4));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            DisjunctionQuery nameQuery = getDisjunction(words, "crew.name", 1.15);
            MatchQuery jobQuery = SearchQuery.match(words).boost(1.1).field("crew.job");
            return SearchQuery.disjuncts(nameQuery, jobQuery);
        }
    }

    private DisjunctionQuery boostPopularity() {
        NumericRangeQuery rangeQuery = SearchQuery.numericRange().field("popularity").boost(1.25);
        rangeQuery.max(1000);
        rangeQuery.min(40);

        NumericRangeQuery rangeQuery2 = SearchQuery.numericRange().field("popularity").boost(1.20);
        rangeQuery2.max(39.9999);
        rangeQuery2.min(30);

        NumericRangeQuery rangeQuery3 = SearchQuery.numericRange().field("popularity").boost(1.10);
        rangeQuery3.max(29.9999);
        rangeQuery3.min(10);

        NumericRangeQuery rangeQuery4 = SearchQuery.numericRange().field("popularity").boost(0.90);
        rangeQuery4.max(9.9999);
        rangeQuery4.min(4);

        NumericRangeQuery rangeQuery5 = SearchQuery.numericRange().field("popularity").boost(0.80);
        rangeQuery5.max(3.9999);
        rangeQuery5.min(0);

        DisjunctionQuery yearDisjunction = SearchQuery.disjuncts(rangeQuery, rangeQuery2, rangeQuery3, rangeQuery4, rangeQuery5 );
        return yearDisjunction;
    }

    private DisjunctionQuery boostReleaseYearQuery() {

        LocalDateTime now = LocalDateTime.now();
        NumericRangeQuery rangeQuery = SearchQuery.numericRange().field("release_year").boost(1.35);
        rangeQuery.max(now.getYear());
        rangeQuery.min(now.getYear()-4);

        NumericRangeQuery penalizationQuery = SearchQuery.numericRange().field("release_year").boost(1.15);
        penalizationQuery.max(now.getYear()-5);
        penalizationQuery.min(now.getYear()-10);

        NumericRangeQuery penalization1Query = SearchQuery.numericRange().field("release_year").boost(1);
        penalization1Query.max(now.getYear()-9);
        penalization1Query.min(now.getYear()-15);

        NumericRangeQuery penalization2Query = SearchQuery.numericRange().field("release_year").boost(0.92);
        penalization2Query.max(now.getYear()-16);
        penalization2Query.min(now.getYear()-25);

        NumericRangeQuery penalization3Query = SearchQuery.numericRange().field("release_year").boost(0.85);
        penalization3Query.max(now.getYear()-25);
        penalization3Query.min(0);

        DisjunctionQuery yearDisjunction = SearchQuery.disjuncts(rangeQuery, penalizationQuery, penalization1Query, penalization2Query, penalization3Query );

        return yearDisjunction;
    }

    private DisjunctionQuery boostRuntime() {

        NumericRangeQuery runtime1 = SearchQuery.numericRange().field("runtime").boost(1.25);
        runtime1.max(5000);
        runtime1.min(360);

        NumericRangeQuery runtime2 = SearchQuery.numericRange().field("runtime").boost(1.17);
        runtime2.max(359);
        runtime2.min(100);

        NumericRangeQuery runtime3 = SearchQuery.numericRange().field("runtime").boost(0.90);
        runtime3.max(99);
        runtime3.min(40);

        NumericRangeQuery runtime4 = SearchQuery.numericRange().field("runtime").boost(0.75);
        runtime4.max(39);
        runtime4.min(0);

        DisjunctionQuery runtimeDisjunction = SearchQuery.disjuncts(runtime1, runtime2, runtime3, runtime4 );

        return runtimeDisjunction;
    }

    private DisjunctionQuery boostWeightedRating() {

        NumericRangeQuery weightedRating1 = SearchQuery.numericRange().field("weightedRating").boost(1.4);
        weightedRating1.max(10);
        weightedRating1.min(7);

        NumericRangeQuery weightedRating2 = SearchQuery.numericRange().field("weightedRating").boost(1.1);
        weightedRating2.max(6.9999);
        weightedRating2.min(5);

        NumericRangeQuery weightedRating3 = SearchQuery.numericRange().field("weightedRating").boost(1);
        weightedRating3.max(4.999);
        weightedRating3.min(3);

        NumericRangeQuery weightedRating4 = SearchQuery.numericRange().field("weightedRating").boost(0.75);
        weightedRating4.max(2.999);
        weightedRating4.min(0);


        DisjunctionQuery runtimeDisjunction = SearchQuery.disjuncts(weightedRating1, weightedRating2, weightedRating3, weightedRating4 );

        return runtimeDisjunction;
    }





    private DisjunctionQuery getDisjunction(String words, String field, double boost) {
        MatchQuery query = SearchQuery.match(words).boost(boost).field(field);
        MatchQuery queryFuzzy = SearchQuery.match(words).boost(boost).field(field)
                .fuzziness(1);

        return SearchQuery.disjuncts(query, queryFuzzy);
    }


    private Result getSearchResults(SearchResult result){

        Result rt = new Result();
        List<FTSResult> movies = new ArrayList<>();
        if (result != null ) {
            Iterator<SearchRow> resultIterator = result.rows().iterator();
            int counter = 1;
            while (resultIterator.hasNext()) {
                SearchRow row = resultIterator.next();
                movies.add( new FTSResult(movieRepository.findById(row.id()).get(), new QueryStats(counter, row)));
                counter++;
            }
        }

        rt.setResults(movies);

        if( result.facets()!= null & result.facets().size() >0) {
            List<Facet> facets = new ArrayList<>();
            for( Map.Entry<String, SearchFacetResult> entry: result.facets().entrySet()) {

                List<SearchTermRange> termRanges = ((TermSearchFacetResult) entry.getValue()).terms();
                if(termRanges == null){
                    continue;
                }
                List<FacetItem> items =  termRanges.stream().map(e->new FacetItem(e)).collect(Collectors.toList());
                facets.add(new Facet(entry.getKey(), items));
            }

            rt.setFacets(facets);
        }
        return rt;
    }

    private Map<String,List<String>> getFilters(String filters){
        if (filters == null || filters.trim().isEmpty()) {
            return new HashMap<>();
        }
        String[] values = filters.split("::");

        Map<String,List<String>> facets = new HashMap<>();
        for(int i=0;i<values.length; i++) {

            String[] test = values[i].split("=");
            if(test.length > 1) {
                facets.put(test[0], Arrays.asList(test[1].split(",")));
            }
        }
        return facets;
    }
}
