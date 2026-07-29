package com.limelight;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class SrvResolver {
    private static final String SERVICE_PREFIX = "_limelightax._tcp.";
    private static final String DNS_ENDPOINT =
            "https://cloudflare-dns.com/dns-query";

    public static class ResultCode{
        private int code;
        private String result;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }


    // SRV 查询入口
    public static ResultCode resolveSRVRecord(String domain) {
        String normalizedDomain = domain == null ? "" : domain.trim();
        String srvQuery = SERVICE_PREFIX + normalizedDomain;
        ResultCode resultCode=new ResultCode();
        HttpsURLConnection connection = null;
        try {
            String encodedQuery = URLEncoder.encode(srvQuery, "UTF-8");
            URL url = new URL(DNS_ENDPOINT + "?name=" + encodedQuery + "&type=SRV");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/dns-json");
            connection.setConnectTimeout(10000); // 10秒
            connection.setReadTimeout(10000);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                // 解析响应
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
                // 处理 SRV 记录
                String response = responseBuilder.toString();
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("Answer")) {
                    JSONArray answers = jsonResponse.getJSONArray("Answer");
                    String result = processSRVRecords(answers, srvQuery);
                    if (result != null) {
                        resultCode.setCode(0);
                        resultCode.setResult(result);
                    }
                    else {
                        resultCode.setCode(1);
                        resultCode.setResult("未找到 SRV 记录: " + srvQuery);
                    }
                } else {
                    resultCode.setCode(1);
                    resultCode.setResult("未找到 SRV 记录: " + srvQuery);
                }
            } else {
                resultCode.setCode(1);
                resultCode.setResult("解析 SRV 记录失败，HTTP 状态码: " + responseCode);
            }
        } catch (Exception e) {
            resultCode.setCode(1);
            resultCode.setResult("解析 SRV 记录失败: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resultCode;
    }

    // 处理 SRV 记录
    static String processSRVRecords(JSONArray answers,
                                    String expectedRecordName)
            throws JSONException {
        String normalizedExpectedName =
                trimTrailingDot(expectedRecordName);
        for (int i = 0; i < answers.length(); i++) {
            JSONObject record = answers.getJSONObject(i);
            String data = record.getString("data");
            String name = record.getString("name");
            if (TextUtils.isEmpty(name) ||
                    !trimTrailingDot(name).equalsIgnoreCase(
                            normalizedExpectedName)) {
                continue;
            }
            // 解析 SRV 数据 (优先级、权重、端口、目标域名)
            String[] srvParts = data.trim().split("\\s+");
            if (srvParts.length >= 4) {
                String port = srvParts[2];
                String target = trimTrailingDot(srvParts[3]);
                return target + ":" + port;
            }
        }
        return null;
    }

    private static String trimTrailingDot(String value) {
        if (value != null && value.endsWith(".")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}

