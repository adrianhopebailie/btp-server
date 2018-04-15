package com.hopebailie.btp.server;

import java.util.HashMap;
import java.util.Map;

public class BtpSubProtocolHandlerRegistry {

  private Map<String, BtpSubProtocolHandler> handlers = new HashMap<>();

  public BtpSubProtocolHandler getHandler(String subProtocolName) {
    return handlers.get(subProtocolName);
  }

  public void putHandler(String subProtocolName, BtpSubProtocolHandler handler) {
    handlers.put(subProtocolName, handler);
  }
}
