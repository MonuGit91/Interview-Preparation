package com.supai.app.otcsapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.supai.app.config.OtcsApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteVersion {
    private final OkHttpClient okHttpClient;
    private final OtcsApi otcsApi;

    public Response deleteVersion(String otdsTicket, String nodeId, String versionNo) throws IOException {
        String url = otcsApi.base.getCommon()+  "/api/v1/nodes/" + nodeId + "/versions/"
                + versionNo;
        log.info("Start : {}", url);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("otdsTicket", otdsTicket)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    public void deleteVersionNo(String otdsTicket, String nodeId, String versionNo) {
        try (Response response = deleteVersion(otdsTicket, nodeId, versionNo)) {
            log.info("Delete Version API Status Code: {}", response.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
