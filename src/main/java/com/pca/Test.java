package com.pca;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

public class Test {
    public static void main(String[] args){
        SimilarityStrategy strategy = new JaroWinklerStrategy();
        StringSimilarityService service = new StringSimilarityServiceImpl(
                strategy);
        System.out.println(service.score("TISSIR ALLAH", "TISSIRALLAH"));
    }
}
