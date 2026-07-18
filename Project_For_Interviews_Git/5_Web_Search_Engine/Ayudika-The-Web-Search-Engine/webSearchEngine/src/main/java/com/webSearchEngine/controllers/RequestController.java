package com.webSearchEngine.controllers;

import com.webSearchEngine.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.webSearchEngine.services.IndexerApplication.indexed;
import static com.webSearchEngine.services.IndexerApplication.words;
import static com.webSearchEngine.services.Others.*;
import static com.webSearchEngine.services.StaticVariables.*;

//@RestController
@RequestMapping("/api/v1/ayudika")
@Controller
public class RequestController {
    @Autowired
    ObjectCreator objectCreator;

    @GetMapping("/{value}")
    public String getUrls(@PathVariable("value") String quarry, Model model) {
        System.out.println("user is searching: " + quarry);
        String userQuarry = quarry;
        words = Others.filterQuarry(quarry);
        objectCreator.createrObject(quarry);
        Ranker.ranking(indexed);

        ResponseEntity<List<Pair>> responseEntity = new ResponseEntity<>(finalList, HttpStatus.OK);
        List<Pair> resultList = responseEntity.getBody();
        fillWebPage(model, userQuarry, resultList);
        System.out.println(resultList.size() + " Links found and it is sent to Front-End");
        Clear.cleanMemory();
        return "webpage";
    }
}
