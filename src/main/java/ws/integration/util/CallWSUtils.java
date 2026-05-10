package ws.integration.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.integration.config.AppConfig;

public class CallWSUtils {

    private static final Logger logger = LoggerFactory.getLogger(CallWSUtils.class);

    public String callCus20(String requestBody) throws Exception {
        URL url = new URL(AppConfig.getInstance().getSearchCustomerUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        return send(conn, requestBody);
    }

    private String send(HttpURLConnection conn, String requestBody) throws Exception {
        OutputStream os = null;
        BufferedReader br = null;
        try {
            os = conn.getOutputStream();
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            os.flush();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("CUS-20 responded with HTTP {}", responseCode);
                throw new Exception("HTTP error " + responseCode + ": " + output);
            }

            logger.info("CUS-20 response: {}", output);
            return output.toString();
        } finally {
            if (os != null) os.close();
            if (br != null) br.close();
            conn.disconnect();
        }
    }
}
