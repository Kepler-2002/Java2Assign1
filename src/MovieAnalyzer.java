import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class MovieAnalyzer {
    static List<Movie> M;
    static class Movie{
        String Title;
        int Released_Year;
        String Certificate;
        int Runtime;
        String Genre;
        double Rating;
        String Overview;
        int Score;
        String Director;
        String Star1;
        String Star2;
        String Star3;
        String Star4;
        int Votes;
        long Gross;

        public String getTitle() {
            return Title;
        }

        public int getRuntime() {
            return Runtime;
        }

        public void setRuntime(int runtime) {
            Runtime = runtime;
        }

        public double getRating() {
            return Rating;
        }

        public void setRating(double rating) {
            Rating = rating;
        }

        public String getOverview() {
            return Overview;
        }

        public int getOverviewLength(){
            return Overview.length();
        }
        public void setOverview(String overview) {
            Overview = overview;
        }

        public long getGross() {
            return Gross;
        }

        public void setGross(int gross) {
            Gross = gross;
        }

        public Movie(String title, String released_Year, String certificate, String runtime, String genre, String rating, String overview, String score, String director, String star1, String star2, String star3, String star4, String votes, String gross) {

            Title = title.replace("\"","");
            Released_Year = Integer.parseInt(released_Year);
            Certificate = certificate;
            Runtime = Integer.parseInt(runtime);
            Genre = genre.replace("\"", "");
            Rating = Double.parseDouble(rating);
            Overview = (overview.charAt(0) == '"' && overview.charAt(overview.length() -1) == '"') ?overview.substring(1,overview.length()-1):overview;
            if(!Objects.equals(score, ""))
                this.Score = Integer.parseInt(score);
            Director = director;
            Star1 = star1;
            Star2 = star2;
            Star3 = star3;
            Star4 = star4;
            Votes = Integer.parseInt(votes);
            if (gross !=null)
                Gross = Integer.parseInt(gross);
        }
        public List<List<String>> getStarList(){
            String[] Stars = {Star1, Star2, Star3, Star4};
            Arrays.sort(Stars,0,Stars.length);
            return Arrays.asList(Arrays.asList(Stars[0],Stars[1]),Arrays.asList(Stars[0],Stars[2]),Arrays.asList(Stars[0],Stars[3]),Arrays.asList(Stars[1],Stars[2]),Arrays.asList(Stars[1],Stars[3]),Arrays.asList(Stars[2],Stars[3]));
        }

        public List<String> getStars(){
            return Arrays.asList(Star1, Star2, Star3, Star4);
        }





    }
    public MovieAnalyzer(String dataset_path) throws IOException {


        M = Files.lines(Paths.get(dataset_path)).skip(1)
                    .map(l -> l.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"))
                        .map(a -> new Movie(a[1],a[2], a[3], a[4].substring(0,a[4].length()-4), a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a.length==16?a[15].replace(",","").substring(1,a[15].replace(",","").length()-1):null)).collect(Collectors.toList());


   }
    public Map<Integer, Integer> getMovieCountByYear(){
        Stream<Movie> movies = M.stream();
        Map<Integer, Integer> result = new TreeMap<>(new Comparator<Integer>()
        {
            public int compare(Integer key1,Integer key2)
            {
                return key2.compareTo(key1);
            }
        });
        movies.forEach(a -> result.put(a.Released_Year, result.get(a.Released_Year)==null?1: result.get(a.Released_Year)+1));

        return result;
    }
    public Map<String, Integer> getMovieCountByGenre(){
        Stream<Movie> movies = M.stream();
        Map<String, Integer> result = new TreeMap<>(new Comparator<String>()
        {
            public int compare(String  key1,String key2)
            {
                return key1.compareTo(key2);
            }
        });
        Map<String, Integer> finalResult = result;
        movies.forEach(m -> Arrays.stream(m.Genre.split(", ")).forEach(g -> finalResult.put(g, finalResult.get(g) == null?1: finalResult.get(g) + 1)));
        result = sortDescend(result);
        return result;
    }
    public Map<List<String>, Integer> getCoStarCount(){
        Stream<Movie> movies = M.stream();
        Map<List<String>, Integer> result = new HashMap<>();
        movies.forEach(m -> m.getStarList().forEach(s -> result.put(s, result.get(s)==null?1:result.get(s) + 1)));
        return result;
    }

    public List<String> getTopMovies(int top_k, String by){
        Stream<Movie> movies = M.stream();
        List<String> result = new ArrayList<>();
        if (by.equals("runtime")){
            List<Movie> ms =  movies.sorted(Comparator.comparing(Movie::getRuntime).reversed().thenComparing(Movie::getTitle)).collect(Collectors.toList());
            for (int i = 0; i < top_k; i++) {
                result.add(ms.get(i).Title);
            }
        }
        else if(by.equals("overview")){
            List<Movie> ms = movies.sorted(Comparator.comparing(Movie::getOverviewLength).reversed().thenComparing(Movie::getTitle)).collect(Collectors.toList());
            for (int i = 0; i < top_k; i++) {
                result.add(ms.get(i).Title);
            }
        }
        return result;
    }
    public List<String> getTopStars(int top_k, String by){
        Stream<Movie> movies = M.stream();
        List<String> result = new ArrayList<>();
        if (by.equals("rating")){
            Map<String,List<Double>> Actor_Rating = new TreeMap<>();
            Map<String, List<Double>> finalActor_Rating = Actor_Rating;
            movies.forEach(m -> m.getStars().forEach(s -> {
                finalActor_Rating.computeIfAbsent(s, k -> new ArrayList<Double>());
                finalActor_Rating.get(s).add(m.getRating());
            }));
            Map<String, Double> tmp = new TreeMap<>();
            Map<String, Double> finalTmp = tmp;
            Actor_Rating.forEach((key, value) -> finalTmp.put(key,value.stream().collect(Collectors.averagingDouble(x ->x))));
            tmp = sortDescend(tmp);
            tmp.entrySet().stream().limit(top_k).forEachOrdered(e -> result.add(e.getKey()));
        }
        else if (by.equals("gross")){
            Map<String,Long> Actor_Gross = new TreeMap<>();
            Map<String, Long>Actor_count = new HashMap<>();
            Map<String, Long> finalActor_Gross = Actor_Gross;
            movies.forEach(m -> m.getStars().forEach(s -> {
                if (m.getGross() != 0){
                    if (Actor_count.get(s) == null){
                        Actor_count.put(s,1L);
                        finalActor_Gross.put(s, m.getGross());
                    }else {
                        long r = finalActor_Gross.get(s)*Actor_count.get(s);
                        r += m.getGross();
                        r = r/(Actor_count.get(s)+1);
                        finalActor_Gross.put(s, r);
                        Actor_count.put(s,Actor_count.get(s)+1);
                    }
                }

            }));
            Actor_Gross = sortDescend(Actor_Gross);

            Actor_Gross.entrySet().stream().limit(top_k).forEachOrdered(e -> result.add(e.getKey()));
        }
        return result;
    }

    public static Map<String, Double> getTopStarss(int top_k){
        Stream<Movie> movies = M.stream();
        Map<String,List<Double>> Actor_Rating = new TreeMap<>();
        Map<String, List<Double>> finalActor_Rating = Actor_Rating;
        movies.forEach(m -> m.getStars().forEach(s -> {
            finalActor_Rating.computeIfAbsent(s, k -> new ArrayList<Double>());
            finalActor_Rating.get(s).add(m.getRating());
        }));
        Map<String, Double> tmp = new TreeMap<>();
        Map<String, Double> finalTmp = tmp;
        Actor_Rating.forEach((key, value) -> finalTmp.put(key,value.stream().collect(Collectors.averagingDouble(x ->x))));
        tmp = sortDescend(tmp);
        return tmp;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortDescend(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return -compare;
            }
        });

        Map<K, V> returnMap = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            returnMap.put(entry.getKey(), entry.getValue());
        }
        return returnMap;
    }
    public static <K, V extends Comparable<? super V>> Map<K, V> sortAscend(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return compare;
            }
        });

        Map<K, V> returnMap = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            returnMap.put(entry.getKey(), entry.getValue());
        }
        return returnMap;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime){
        Stream<Movie> movies = M.stream();
        List<String> result = new ArrayList<>();
        movies.forEach(m -> {
            String [] genres = m.Genre.split(", ");
            for (String g: genres
                 ) {
                if (g.equals(genre) && m.getRating() >= min_rating && m.Runtime <= max_runtime)
                    result.add(m.Title);
            }
        });
        Collections.sort(result);
        return result;
    }

    public static void main(String[] args) throws IOException {
        MovieAnalyzer a = new MovieAnalyzer("resources/imdb_top_500.csv");

        Map<String, Double> ans =  getTopStarss(80);
        ans.forEach((k,v) -> System.out.println(k + " " + v));
    }

}