package com.cmcc.newcalllib.tool;

import androidx.annotation.NonNull;

import com.cmcc.newcalllib.tool.lang.RandomStringUtils;
import com.google.gson.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for token creation and verification
 */
public class TokenUtil {

    public static final String AES = "AES";
    public static final String HMAC_MD5 = "HmacMD5";
    private static final Key aesKey = getAESKey();
    private static final String hmacKey = "8Juyo+uCueWaje2EquSrreSvh+aCkeOxpuWNjfCgsojwrI298KyEug==";

    public static class TokenVerifyResult {
        public static final int RESULT_SUCCESS = 0;
        public static final int RESULT_LENGTH_MISMATCH = -1;
        public static final int RESULT_INVALID = -2;
        public static final int RESULT_TIMEOUT = -3;

        private final int code;
        private final String message;

        public TokenVerifyResult(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public boolean isSuccess() {
            return code == RESULT_SUCCESS;
        }

        @NonNull
        @Override
        public String toString() {
            return "Status{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    /**
     * 校验token，主要是校验两个方面
     * 1、校验签名是否合法
     * 2、校验token是否过期
     * @param token token in string
     * @param appId
     * @return TokenVerifyResult
     */
    public static TokenVerifyResult verify(String token, String appId) {
        List<String> list = Arrays.asList(token.split("\\."));
        if (list.size() != 3) {
            return new TokenVerifyResult(TokenVerifyResult.RESULT_LENGTH_MISMATCH, "token length is not valid");
        }
        String headerJson = list.get(0);
        String encryptJson = list.get(1);
        String sign = list.get(2);
        try {
            if (!verifySign(encryptJson, sign)) {
                return new TokenVerifyResult(TokenVerifyResult.RESULT_INVALID, "token is invalid");
            }

            if (!verifyHeader(headerJson, appId)) {
                return new TokenVerifyResult(TokenVerifyResult.RESULT_TIMEOUT, "token has timed out");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new TokenVerifyResult(TokenVerifyResult.RESULT_SUCCESS, "token is valid");
    }

    /**
     * 校验token是否过期
     * @param headerJson  请求头的base64编码
     * @return verify result. true if no 'expire' header
     */
    private static boolean verifyHeader(String headerJson, String appId) {
        byte[] headerBytes = base64Decoder(headerJson);
        String json = new String(headerBytes);
        Gson gson = JsonUtil.INSTANCE.getDefaultGson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        JsonElement element = jsonObject.get("expire");
        if (element == null) {
            return true;
        }
        long expireTime = element.getAsLong();
        boolean expire = expireTime <= System.currentTimeMillis() / 1000;
        JsonElement appIdEle = jsonObject.get("appId");
        if (appIdEle == null) {
            return false;
        }
        boolean idSame = appIdEle.getAsString().equals(appId);
        return idSame && !expire;
    }

    /**
     * 校验签名 使用base64解码后的字节数组来生成签名，并与传递来的签名进行比较得出合法性
     * @param encryptJson
     * @param sign
     * @return
     * @throws Exception
     */
    private static boolean verifySign(String encryptJson, String sign) throws Exception{
        byte[] encryptBody = base64Decoder(encryptJson);
        byte[] bytes = generateHmacSign(encryptBody);
        return StringUtil.INSTANCE.equals(sign, base64Encoder(bytes));
    }

    /**
     * Create token
     * @param tokenBody bean object or just string
     * @return token
     */
    public static <T> String createTokenNeverTimeout(T tokenBody, String appId) {
        return createToken(tokenBody, -1, appId);
    }

    /**
     * Create token
     * @param tokenBody bean object or just string
     * @param sec timeout seconds
     * @return token
     */
    public static <T> String createToken(T tokenBody, int sec, String appId) {
        long now = System.currentTimeMillis() / 1000;
        Gson gson = JsonUtil.INSTANCE.getDefaultGson();
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("body", gson.toJson(tokenBody));

        String randomAlphabetic = RandomStringUtils.randomAlphabetic(3);
        JsonObject jsonHeader = new JsonObject();
        jsonHeader.addProperty("rand_num", randomAlphabetic);
        jsonHeader.addProperty("now", now);
        if (sec > 0) {
            jsonHeader.addProperty("expire", (now + sec));
        }
        jsonHeader.addProperty("appId", appId);

        String token = null;
        try {
            byte[] jsonHeaderBytes = jsonHeader.toString().getBytes(StandardCharsets.UTF_8);
            byte[] encryptContent = generateEncryptBody(jsonHeader.toString(), jsonBody.toString());
            byte[] signWithEncrypt = generateSignWithEncrypt(encryptContent);
            StringBuilder builder = new StringBuilder();
            token = builder.append(base64Encoder(jsonHeaderBytes))
                    .append(".")
                    .append(base64Encoder(encryptContent))
                    .append(".")
                    .append(base64Encoder(signWithEncrypt))
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.INSTANCE.v("create token: " + token);
        return token;
    }

    private static byte[] generateEncryptBody(String header, String body) throws Exception{
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        byte[] content = xor(headerBytes, bodyBytes);
        return AESEncrypt(content);
    }

    private static byte[] generateSignWithEncrypt(byte[] encryptContent) throws Exception{
        return generateHmacSign(encryptContent);
    }

    private static byte[] generateHmacSign(byte[] content) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), HMAC_MD5);
        Mac mac = Mac.getInstance(HMAC_MD5);
        mac.init(secretKey);
        return mac.doFinal(content);
    }

    private static String base64Encoder(byte[] data) {
        return StringUtil.INSTANCE.base64Encode(data);
    }

    private static byte[] base64Decoder(String content) {
        return StringUtil.INSTANCE.base64Decode(content);
    }

    private static byte[] xor(byte[] header, byte[] body) {
        byte[] xorData = new byte[body.length];
        for (int i = 0; i < body.length; i ++) {
            int idx = i % header.length;
            xorData[i] = (byte)(body[i] ^ header[idx]);
        }
        return xorData;
    }

    private static Key getAESKey() {
        Key key = null;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
            keyGenerator.init(new SecureRandom());
            byte[] encodedKey = keyGenerator.generateKey().getEncoded();
            key = new SecretKeySpec(encodedKey, AES);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return key;
    }

    private static byte[] AESEncrypt(byte[] content) throws Exception {
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(content);
    }

    private static byte[] AESDecrypt(byte[] content) throws Exception {
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(content);
    }

    private static byte[] AESDecryptBody(byte[] encryptBody) throws Exception{
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(encryptBody);
    }
}

