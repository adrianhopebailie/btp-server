package com.hopebailie.btp.server;

import org.springframework.web.socket.WebSocketSession;

public class BtpSession {

  private static final String ACCOUNT_KEY = "ILP-Account";
  private final String account;

  private BtpSession(String account) {
    this.account = account;
  }

  public static void storeInWebSocketSession(WebSocketSession session, String username, String token) {
    session.getAttributes().put(ACCOUNT_KEY, username + ":" + token);
  }

  public static boolean isAuthenticated(WebSocketSession session) {
    return session.getAttributes().containsKey(ACCOUNT_KEY);
  }

  public static BtpSession fromWebSocketSession(WebSocketSession session) {
    String account = (String) session.getAttributes().get(ACCOUNT_KEY);
    if(account == null) {
      throw new RuntimeException("Account not found in WebSocket session attributes.");
    }
    return new BtpSession(account);
  }

  public String getAccount() {
    return account;
  }

}
