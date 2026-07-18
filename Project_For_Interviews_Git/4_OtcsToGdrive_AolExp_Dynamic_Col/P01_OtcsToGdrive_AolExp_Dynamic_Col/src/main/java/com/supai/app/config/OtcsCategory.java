package com.supai.app.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Configuration
@ConfigurationProperties(prefix = "otcs.category")
@Slf4j
public class OtcsCategory {
	private String attr;
	private String queryId;
	private String categoryId;
	private Map<String, Object> feildNames = new LinkedHashMap<>();
	private Map<String, String> userFeildNames = new LinkedHashMap<>();
	// private Map<String, String> feildNames;
	
	public List<String> getAllLeafKeys() {
	    List<String> leafKeys = new ArrayList<>();

	    // Handle nested map (feildNames)
	    for (Map.Entry<String, Object> entry : feildNames.entrySet()) {
	        extractLeafKeys(entry.getKey(), entry.getValue(), leafKeys);
	    }

	    // Handle flat map (userFeildNames)
	   // leafKeys.addAll(userFeildNames.keySet());

	    return leafKeys;
	}

	private void extractLeafKeys(String currentPath, Object value, List<String> leafKeys) {
	    if (value instanceof Map<?, ?> map) {
	        for (Map.Entry<?, ?> entry : map.entrySet()) {
	            String key = entry.getKey().toString();
	            extractLeafKeys(currentPath + "." + key, entry.getValue(), leafKeys);
	        }
	    } else {
//	    	log.info(currentPath); //it can be like ContractTransactionDetails.CounterParty
	    	String []catSplit = currentPath.split("[.]");
	        leafKeys.add(catSplit[catSplit.length-1]);
	    }
	}

}
