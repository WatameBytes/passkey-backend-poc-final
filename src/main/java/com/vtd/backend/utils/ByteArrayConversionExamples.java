package com.vtd.backend.utils;

import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.data.exception.HexException;
import java.util.Arrays;

public class ByteArrayConversionExamples {
    public static void main(String[] args) throws Base64UrlException, HexException {
        // ByteArray to Base64 String
        ByteArray byteArray = new ByteArray(new byte[]{1, 2, 3, 4});
        String base64String = byteArray.getBase64();
        System.out.println("Base64 String: " + base64String);

        // Base64 String to ByteArray
        ByteArray byteArrayFromBase64 = ByteArray.fromBase64(base64String);
        System.out.println("Converted ByteArray from Base64: " + Arrays.toString(byteArrayFromBase64.getBytes()));

        // ByteArray to Base64 URL String
        String base64UrlString = byteArray.getBase64Url();
        System.out.println("Base64 URL String: " + base64UrlString);

        // Base64 URL String to ByteArray
        ByteArray byteArrayFromBase64Url = ByteArray.fromBase64Url(base64UrlString);
        System.out.println("Converted ByteArray from Base64 URL: " + Arrays.toString(byteArrayFromBase64Url.getBytes()));

        // ByteArray to byte[]
        byte[] byteArrayToBytes = byteArray.getBytes();
        System.out.println("byte[]: " + Arrays.toString(byteArrayToBytes));

        // byte[] to ByteArray
        byte[] bytes = new byte[]{1, 2, 3, 4};
        ByteArray byteArrayFromBytes = new ByteArray(bytes);
        System.out.println("ByteArray from byte[]: " + Arrays.toString(byteArrayFromBytes.getBytes()));

        // Hex String to ByteArray
        String hexString = "01020304";
        ByteArray byteArrayFromHex = ByteArray.fromHex(hexString);
        System.out.println("ByteArray from Hex: " + Arrays.toString(byteArrayFromHex.getBytes()));

        // ByteArray to Hex String
        String hexStringFromByteArray = byteArray.getHex();
        System.out.println("Hex String: " + hexStringFromByteArray);
    }
}

