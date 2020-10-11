package com.xobotun.vtb;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static spark.Spark.port;
import static spark.Spark.post;

@Slf4j
class CarBot {
    static {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(PropertiesHelper.get(PropertiesHelper.PROXY_LOGIN), PropertiesHelper.get(PropertiesHelper.PROXY_PASSWORD)));
        clientBuilder.useSystemProperties();
        clientBuilder.setProxy(new HttpHost(PropertiesHelper.get(PropertiesHelper.PROXY_HOST), Integer.parseInt(PropertiesHelper.get(PropertiesHelper.PROXY_PORT))));
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
        clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
        Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.BASIC, new BasicSchemeFactory()).build();
        clientBuilder.setDefaultAuthSchemeRegistry(authProviders);
        Unirest.setHttpClient(clientBuilder.build());
    }

    public static void main(String[] args) {
        port(Integer.parseInt(PropertiesHelper.get(PropertiesHelper.HTTP_PORT)));
        post("/", CarBot::processCommand);
    }

    public static String processCommand(Request request, Response response) {
        log.info(request.body());

        JSONObject body = new JSONObject(request.body());

        String text = null;
        String photo = null;
        long chatId;

        //#region parsing message
        try {
            text = body.getJSONObject("message").getString("text");
        } catch (JSONException e) {
            // not a text message
        }

        try {
            val photos = body.getJSONObject("message").getJSONArray("photo");
            photo = photos == null ? null : photos.getJSONObject(0).getString("file_id");
        } catch (JSONException e) {
            // not a photo message
        }

        try {
            chatId = body.getJSONObject("message").getJSONObject("chat").getLong("id");

        } catch (JSONException e) {
            // not a text message, may be a changed text message
            try {
                text = body.getJSONObject("edited_message").getString("text");
            } catch (JSONException e2) {
                // not a text message. Still, no need for sending photo data
            }

            chatId = body.getJSONObject("edited_message").getJSONObject("chat").getLong("id");
        }
        //#endregion

        //#region handling parsed message
        if (photo == null && text == null) {
            sendMessage(chatId, badRequest());
            return "bad request";
        }

        if (photo != null) {
            log.info("photo id {}", photo);
            byte[] file = getTelegramFile(photo);
            return handleCarRecognition(chatId, file);
        }
        if (text.startsWith("http")) {
            byte[] file = getFile(text);
            return handleCarRecognition(chatId, file);
        }

        if (text.startsWith("/start")) {
            sendMessage(chatId, getGreetings());
            return "started";
        }
        if (text.startsWith("/help")) {
            sendMessage(chatId, getHelp());
            return "helped";
        }
        //#endregion

        sendMessage(chatId, badRequest());
        return "bad request";
    }

    private static String handleCarRecognition(long chatId, byte[] file) {
        if (file == null) { sendMessage(chatId, "Failed to get file from url"); }
        else {
            try {
                String resp = recognizeCar(file);
                val sorted = parseRecognition(resp);
                if (sorted.isEmpty()) {
                    sendMessage(chatId, "Я не смог распознать эту машину, увы. :(");
                    return "photo received";
                }

                Double best = sorted.lastKey();
                String price = HardcodedPriceService.get(sorted.get(best));
                if (best > 0.85) {
                    sendMessage(chatId, String.format("Это %s, я уверен на %d%%!\n" + price, sorted.get(best), (int)(best * 100)));
                } else {
                    sendMessage(chatId, String.format("Это может быть %s, но я в этом уверен лишь на %d%%. Вполне возможно, что меня этой модели не обучали и я её с чем-то спутал.", sorted.get(best), (int)(best * 100)));
                }
            } catch (VtbException e) {
                sendMessage(chatId, "Received unexpected message from VTB backend");
                sendMessage(chatId, e.getMessage());
            }
        }

        return "photo received";
    }

    private static void sendMessage(long chatId, String text) {
        String token = PropertiesHelper.get(PropertiesHelper.TELEGRAM_APIKEY);
        String urlForLogging = "null";
        try {
            val request = Unirest.get(String.format("https://api.telegram.org/bot%s/sendMessage", token)).queryString("chat_id", Long.toString(chatId)).queryString("text", text).queryString("parse_mode", "Markdown");

            urlForLogging = request.getUrl();
            val response = request.asString(); // send it
            if (response.getStatus() != 200) {
                log.warn(String.format("%d %s %s", response.getStatus(), response.getStatusText(), response.getBody()));
                log.warn(urlForLogging);
            }
        } catch (UnirestException e) {
            log.warn(String.format("Could not send request to %s", urlForLogging));
        }
    }

    @SneakyThrows
    private static byte[] getTelegramFile(String fileId) {
        String token = PropertiesHelper.get(PropertiesHelper.TELEGRAM_APIKEY);
        String urlForLogging = "null";
        try {
            val request = Unirest.get(String.format("https://api.telegram.org/bot%s/getFile", token)).queryString("file_id", fileId);

            urlForLogging = request.getUrl();
            val response = request.asString(); // send it
            if (response.getStatus() != 200) {
                log.warn(String.format("%d %s %s", response.getStatus(), response.getStatusText(), response.getBody()));
                log.warn(urlForLogging);
            }

            String filePath = new JSONObject(response.getBody()).getJSONObject("result").getString("file_path");
            log.info("file path for {} is {}", fileId, filePath);

            val request2 = Unirest.get(String.format("https://api.telegram.org/file/bot%s/", token) + filePath);

            urlForLogging = request2.getUrl();

            return getFile(urlForLogging);
        } catch (UnirestException e) {
            log.warn(String.format("Could not send request to %s", urlForLogging));
        }

        return null;
    }

    @SneakyThrows
    private static byte[] getFile(String url) {
        val request2 = Unirest.get(url);

        val response = request2.asBinary(); // send it

        try {
            return response.getBody().readAllBytes();
        } catch (IOException e) {
            log.warn("Failed to read image from stream, url {}", url, e);
            return null;
        }
    }

    private static String recognizeCar(byte[] image) throws RuntimeException {
        String token = PropertiesHelper.get(PropertiesHelper.VTB_APIKEY);
        String urlForLogging = "null";
        try {
            val request = Unirest.post("https://gw.hackathon.vtb.ru/vtb/hackathon/car-recognize")
                                 .header("X-IBM-Client-Id", token)
                                 .body("{\"content\":\"" + new String(Base64.getEncoder().encode(image)) + "\"}");

//            urlForLogging = request.getUrl();
            val response = request.asString(); // send it
            if (response.getStatus() != 200) {
                log.warn(String.format("%d %s %s", response.getStatus(), response.getStatusText(), response.getBody()));
                log.warn(urlForLogging);
                throw new VtbException(response.getBody());
            }

            String body = response.getBody();
            log.info("recognition: {}", body);
            return body;
        } catch (UnirestException e){
            log.warn(String.format("Could not send request to %s", urlForLogging));
        }

        return null;
    }

    private static SortedMap<Double, String> parseRecognition(String raw) {
        if (raw == null) return Collections.emptySortedMap();

        JSONObject body = new JSONObject(raw).getJSONObject("probabilities");

        SortedMap<Double, String> result = new TreeMap<>();
        for (String key : body.keySet()) {
            result.put(body.getDouble(key), key);
        }

        return result;
    }

    private static String getHelp() {
        return "Просто пришли мне картинку или ссылку на неё и я попытаюсь определить, что за машина на ней изображена.";
    }

    private static String badRequest() {
        return "Я тебя не понимаю. :(\n" + "Я пыталась вызвать у тебя /help, но ты, похоже, не бот...";
    }

    private static String getGreetings() {
        return "Я умею определять марку и модель машины. Можете прислать фотографию, или картинку из интернета и я подскажу, что это за авто.";
    }

}
//{
//  "update_id": 292501094,
//  "message": {
//    "message_id": 7,
//    "from": {
//      "id": 279964437,
//      "is_bot": false,
//      "first_name": "Paul Maminov",
//      "last_name": "@Xobotun",
//      "username": "Xobotun",
//      "language_code": "en-US"
//    },
//    "chat": {
//      "id": 279964437,
//      "first_name": "Paul Maminov",
//      "last_name": "@Xobotun",
//      "username": "Xobotun",
//      "type": "private"
//    },
//    "date": 1539500523,
//    "text": "/help",
//    "entities": [
//      {
//        "offset": 0,
//        "length": 5,
//        "type": "bot_command"
//      }
//    ]
//  }
//}
