package com.vtd.backend.utils;

import java.sql.Blob;
import java.sql.SQLException;

public class BlobUtil {
    public static byte[] blobToBytes(Blob blob) {
        if (blob == null) {
            return null;
        }
        try {
            int blobLength = (int) blob.length();
            return blob.getBytes(1, blobLength);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert Blob to byte[]", e);
        }
    }
}
