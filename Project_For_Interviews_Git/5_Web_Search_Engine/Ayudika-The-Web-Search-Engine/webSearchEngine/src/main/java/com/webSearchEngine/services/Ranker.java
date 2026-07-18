package com.webSearchEngine.services;

import java.net.URL;
import java.util.*;

import static com.webSearchEngine.services.IndexerApplication.words;
import static com.webSearchEngine.services.StaticVariables.*;

public class Ranker {
    public static List<URL> urls= new ArrayList<>();
    public static void ranking(List<Pair> list) {
        System.out.println("Now Ranking web pages please wait!!");
        Set<String> urlSet = new HashSet<>();
        if(list == null) {
            list = new ArrayList<>();
            return;
        }

        for(Pair p : list) {
            urlSet.add(p.url);
        }
        for(Pair p : list) {
            if(urlSet.contains(p.url)) {
                finalList.add(p);
                urlSet.remove(p.url);
            }
        }
        if(finalList != null) sort(finalList);
        if(finalList == null) finalList = list;
    }
    public static void sort(List<Pair> finalList) {
        List<Pair> containsWord = new ArrayList<>();
        List<Pair> notContainsWord = new ArrayList<>();

        for(Pair p : finalList) {
            String title[] = p.title.toLowerCase().split("[^a-zA-Z]+");
            boolean isAdded = false;
            for(String word : words) {
                if(p.title.equals(word) && p.demoParagraph.length() > 3) {
                    containsWord.add(p);
                    isAdded = true;
                }
            }
            if(!isAdded  && p.demoParagraph.length() > 3) notContainsWord.add(p);
        }

        finalList.clear();// = new ArrayList<>();
        for(Pair p : containsWord) {
            if (finalList.size() < 40) finalList.add(p);
            else return;
        }
        for(Pair p : notContainsWord) {
            if (finalList.size() < 40) finalList.add(p);
            else return;
        }
    }
}