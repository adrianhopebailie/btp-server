package com.hopebailie.btp.server;

public class BtpSubProtocolHandler {

  public byte[] handleBinaryMessage(BtpSession session, int order, byte[] data) {
    throw new RuntimeException("Unexpected content type. Expected binary message.");
  }

  public String handleTextMessage(BtpSession account, int order, String data) {
    throw new RuntimeException("Unexpected content type. Expected text message.");
  }

  public String handleJsonMessage(BtpSession account, int order, String data) {
    throw new RuntimeException("Unexpected content type. Expected JSON message.");
  }

}
