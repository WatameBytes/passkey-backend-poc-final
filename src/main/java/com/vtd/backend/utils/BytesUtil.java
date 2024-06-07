package com.vtd.backend.utils;

import org.bson.types.ObjectId;

import java.nio.ByteBuffer;
import java.util.UUID;


/**
 * Utility class for converting between strings and byte arrays,
 * handling both MongoDB ObjectId and UUID formats.
 */
public class BytesUtil {

  /**
   * Converts a string to a byte array.
   * The input string can be either a MongoDB ObjectId or a UUID.
   *
   * @param objectIdStr the string to convert, either in ObjectId or UUID format
   * @return a byte array representing the input string
   * @throws IllegalArgumentException if the input string is neither a valid ObjectId nor a UUID
   */
  public static byte[] stringToBytes(String objectIdStr) {
    try {
      // Attempt to interpret the string as a MongoDB ObjectId
      ObjectId objectId = new ObjectId(objectIdStr);
      return objectId.toByteArray();
    } catch (IllegalArgumentException e) {
      // If the string is not a valid ObjectId, attempt to interpret it as a UUID
      UUID uuid = UUID.fromString(objectIdStr);
      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
      byteBuffer.putLong(uuid.getMostSignificantBits());
      byteBuffer.putLong(uuid.getLeastSignificantBits());
      return byteBuffer.array();
    }
  }

  /**
   * Converts a byte array to a string.
   * The byte array can represent either a MongoDB ObjectId or a UUID.
   *
   * @param bytes the byte array to convert
   * @return a string representing the input byte array, either in ObjectId or UUID format
   * @throws IllegalArgumentException if the byte array cannot be interpreted as a valid ObjectId or UUID
   */
  public static String bytesToString(byte[] bytes) {
    try {
      // Attempt to interpret the byte array as a MongoDB ObjectId
      ObjectId objectId = new ObjectId(bytes);
      return objectId.toHexString();
    } catch (IllegalArgumentException e) {
      // If the byte array is not a valid ObjectId, attempt to interpret it as a UUID
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      long high = byteBuffer.getLong();
      long low = byteBuffer.getLong();
      return new UUID(high, low).toString();
    }
  }

  public static byte[] classicStringToBytes(String objectIdStr) {
    ObjectId objectId = new ObjectId(objectIdStr);
    return objectId.toByteArray();
  }

  public static String classicBytesToString(byte[] bytes) {
    ObjectId objectId = new ObjectId(bytes);
    return objectId.toHexString();
  }

  // Method to convert Long to byte[]
  public static byte[] longToBytes(Long value) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(value);
    return buffer.array();
  }

  // Method to convert byte[] to Long
  public static Long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    return buffer.getLong();
  }
}

