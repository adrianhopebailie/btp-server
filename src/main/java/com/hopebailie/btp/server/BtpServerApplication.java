package com.hopebailie.btp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BtpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BtpServerApplication.class, args);
	}

  @Bean
  public BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry() {
    BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry();
    registry.putHandler("ilp", new MockIlpHandler());
    return registry;
  }

  class MockIlpHandler extends BtpSubProtocolHandler {

    @Override
    public byte[] handleBinaryMessage(BtpSession session, int order, byte[] data) {
      System.out.println("ILP Packet from " + session.getAccount());
      return new byte[]{};
    }
  }

}
