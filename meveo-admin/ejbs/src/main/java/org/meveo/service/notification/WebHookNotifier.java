package org.meveo.service.notification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.IEntity;
import org.meveo.model.notification.NotificationHistoryStatusEnum;
import org.meveo.model.notification.WebHook;
import org.meveo.model.notification.WebHookMethodEnum;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.util.MeveoJpaForJobs;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
import org.slf4j.Logger;

@Stateless
public class WebHookNotifier {

    @Inject
    @MeveoJpaForJobs
    private EntityManager em;

    @Inject
    Logger log;

    @Inject
    NotificationHistoryService notificationHistoryService;

    @Inject
    ScriptInstanceService scriptInstanceService;

    private String evaluate(String expression, IEntity e, Map<String, Object> context) throws BusinessException {
        HashMap<Object, Object> userMap = new HashMap<Object, Object>();
        userMap.put("event", e);
        userMap.put("context", context);
        return (String) ValueExpressionWrapper.evaluateExpression(expression, userMap, String.class);
    }

    private Map<String, String> evaluateMap(Map<String, String> map, IEntity e, Map<String, Object> context) throws BusinessException {
        Map<String, String> result = new HashMap<String, String>();
        HashMap<Object, Object> userMap = new HashMap<Object, Object>();
        userMap.put("event", e);
        userMap.put("context", context);

        for (String key : map.keySet()) {
            result.put(key, (String) ValueExpressionWrapper.evaluateExpression(map.get(key), userMap, String.class));
        }

        return result;
    }

    @Asynchronous
    public void sendRequest(WebHook webHook, IEntity e, Map<String, Object> context) {
        log.debug("webhook sendRequest");
        String result = "";

        try {
            String url = webHook.getHost().startsWith("http") ? webHook.getHost() : "http://" + webHook.getHost();
            if (webHook.getPort() != null) {
                url += ":" + webHook.getPort();
            }

            if (!StringUtils.isBlank(webHook.getPage())) {
                url += (url.endsWith("/") ? "" : "/") + evaluate(webHook.getPage(), e, context);
            }
            Map<String, String> params = evaluateMap(webHook.getWebhookParams(), e, context);

            String paramQuery = "";
            String sep = "";
            for (String paramKey : params.keySet()) {
                paramQuery += sep + URLEncoder.encode(paramKey, "UTF-8") + "=" + URLEncoder.encode(params.get(paramKey), "UTF-8");
                sep = "&";
            }
            String bodyEL_evaluated = null;
            log.debug("paramQuery={}", paramQuery);
            if (WebHookMethodEnum.HTTP_GET == webHook.getHttpMethod()) {
                url += "?" + paramQuery;
            } else if (WebHookMethodEnum.HTTP_POST == webHook.getHttpMethod()) {
                bodyEL_evaluated = evaluate(webHook.getBodyEL(), e, context);
                log.debug("Evaluated BodyEL={}", bodyEL_evaluated);
                if (StringUtils.isBlank(bodyEL_evaluated)) {
                    paramQuery += "&" + bodyEL_evaluated;
                }
            }
            log.debug("webhook url: {}", url);
            URL obj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();

            Map<String, String> headers = evaluateMap(webHook.getHeaders(), e, context);
            if (!StringUtils.isBlank(webHook.getUsername()) && !headers.containsKey("Authorization")) {
                byte[] bytes = Base64.encodeBase64((webHook.getUsername() + ":" + webHook.getPassword()).getBytes());
                headers.put("Authorization", "Basic " + new String(bytes));
            }

            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }

            if (WebHookMethodEnum.HTTP_GET == webHook.getHttpMethod()) {
                conn.setRequestMethod("GET");
            } else if (WebHookMethodEnum.HTTP_POST == webHook.getHttpMethod()) {
                conn.setRequestMethod("POST");
            } else if (WebHookMethodEnum.HTTP_PUT == webHook.getHttpMethod()) {
                conn.setRequestMethod("PUT");
            } else if (WebHookMethodEnum.HTTP_DELETE == webHook.getHttpMethod()) {
                conn.setRequestMethod("DELETE");
            }
            conn.setUseCaches(false);

            if (WebHookMethodEnum.HTTP_GET != webHook.getHttpMethod()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(paramQuery);
                writer.flush();
                writer.close();
                os.close();
            }
            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            result = response.toString();
            if (responseCode != 200) {
                try {
                    log.debug("webhook httpStatus error : " + responseCode + " response=" + result);
                    notificationHistoryService.create(webHook, e, "http error status=" + responseCode + " response=" + result, NotificationHistoryStatusEnum.FAILED);
                } catch (BusinessException e2) {
                    log.error("Failed to create webhook ", e);
                }
            } else {
                if (webHook.getScriptInstance() != null) {
                    HashMap<Object, Object> userMap = new HashMap<Object, Object>();
                    userMap.put("event", e);
                    userMap.put("response", result);

                    try {
                        Map<String, Object> paramsEvaluated = new HashMap<String, Object>();

                        for (@SuppressWarnings("rawtypes")
                        Map.Entry entry : webHook.getParams().entrySet()) {
                            paramsEvaluated.put((String) entry.getKey(), ValueExpressionWrapper.evaluateExpression((String) entry.getValue(), userMap, String.class));
                        }
                        paramsEvaluated.put("response", result);
                        scriptInstanceService.execute(webHook.getScriptInstance().getCode(), paramsEvaluated, webHook.getScriptInstance().getAuditable().getCreator());

                    } catch (Exception ee) {
                        log.error("Failed to execute a script {}", webHook.getScriptInstance().getCode(), ee);
                    }
                }
                notificationHistoryService.create(webHook, e, result, NotificationHistoryStatusEnum.SENT);
                log.debug("webhook answer : " + result);
            }
        } catch (BusinessException e1) {
            try {
                log.debug("webhook business error : ", e1);
                notificationHistoryService.create(webHook, e, e1.getMessage(), NotificationHistoryStatusEnum.FAILED);
            } catch (BusinessException e2) {
                log.error("Failed to create webhook business ", e2);

            }
        } catch (IOException e1) {
            try {
                log.debug("webhook io error : ", e1);
                notificationHistoryService.create(webHook, e, e1.getMessage(), NotificationHistoryStatusEnum.TO_RETRY);
            } catch (BusinessException e2) {
                log.error("Failed to create webhook io ", e2);
            }
        }
    }

    public static void main(String[] args) {
        String test = "{  \"sid\": \"CLb2f57233976448368708c754b3c1efb7\",  \"date_created\": \"Sat, 21 Feb 2015 18:37:49 +0000\","
                + "  \"date_updated\": \"Sat, 21 Feb 2015 18:37:49 +0000\",  \"account_sid\": \"ACae6e420f425248d6a26948c17a9e2acf\","
                + "  \"api_version\": \"2012-04-24\",  \"friendly_name\": \"RC_A1\",  \"login\": \"RC_A1\","
                + "  \"password\": \"toto\",  \"status\": \"1\",  \"voice_method\": \"POST\",  \"voice_fallback_method\": \"POST\","
                + "  \"uri\": \"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Clients/CLb2f57233976448368708c754b3c1efb7.json\"}";
        try {
            JSONObject json = new JSONObject(test);
            System.out.println(json.getString("sid"));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
