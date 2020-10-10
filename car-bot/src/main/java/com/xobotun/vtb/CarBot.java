package com.xobotun.vtb;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
            text = body.getJSONObject("edited_message").getString("text");
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
            return "photo received";
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

    private static String getHelp() {
        return "`/help` — я всегда подскажу, как ко мне обратиться. :3\n" + "`/calc num1 num2` — то, для чего я и была создана – считать твои действия до няфферки. Просто укажи мне два числа – и я подскажу как пройти от одного к другому. :3\n" + "`/calc num1 num2 mult` — если на этой неделе Нянтик ввели множитель АП, напиши его в конце, я не очень слежу за новостями.\n" + "`/list` — если тебе интересно, какие действия я умею учитывать при подсчёте АП до следующей няфферки.";
    }

    private static String badRequest() {
        return "Я тебя не понимаю. :(\n" + "Я пыталась вызвать у тебя /help, но ты, похоже, не бот...";
    }

    private static String getGreetings() {
        return "Ой, привет! Я – НяфферкоБот, я умею считать АП до следующей няфферки и люблю это делать. Просто скажи мне твоё текущее АП и я скажу тебе, что тебе надо сделать до следующей няфферки. :3\n" + "А ещё я умею делать всякое другое:\n\n" + getHelp();
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
