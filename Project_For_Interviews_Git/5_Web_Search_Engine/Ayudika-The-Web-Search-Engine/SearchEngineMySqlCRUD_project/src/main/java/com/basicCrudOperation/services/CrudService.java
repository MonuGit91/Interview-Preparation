package com.basicCrudOperation.services;

import com.basicCrudOperation.dao.models.Url;
import com.basicCrudOperation.dao.repositories.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.basicCrudOperation.services.fileInput.FileInput.fileInput;
import static com.basicCrudOperation.services.fileInput.FileInput.scan;

@Service
public class CrudService {
    @Autowired
    UrlRepository urlRepository;

    public List<Url> getUrls() {
        List<Url> urls =  urlRepository.findAll();
        System.out.println(urls.toString());
        return urls;
    }
    public List<Url> deleteUrls() {
        urlRepository.deleteAll();
        return null;
    }

    public void fill() {
        System.out.println("inside fill(): ");
        fileInput();
        while(scan.hasNextLine()) {
            String urlStr = scan.nextLine();

            try {
                Url url = new Url();
                System.out.println("url: " + urlStr);
                 url.setUrl(urlStr);
                urlRepository.save(url);
                System.out.println("url.toString(): " + url.toString());
            }
            catch (Exception e) {}
        }

    }

    public List<Url> setUrls() {
        System.out.println("inidesetUrl()");
        fill();
        return getUrls();
    }
}
