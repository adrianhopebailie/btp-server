package com.hopebailie.btp.server;

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocolContentType;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.btp.asn.framework.BtpCodecs;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class BtpSocketHandler extends BinaryWebSocketHandler {

  private final BtpSubProtocolHandlerRegistry registry;
  private final CodecContext context;

  public BtpSocketHandler(BtpSubProtocolHandlerRegistry registry) {
    this.registry = registry;
    this.context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
    BtpCodecs.register(context);
  }

  public void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws InterruptedException, IOException {

    if (!BtpSession.isAuthenticated(session)) {
      authenticate(session, message);
      return;
    }

    BtpSession btpSession = BtpSession.fromWebSocketSession(session);
    BtpMessage btpMessage = getBtpMessage(message);

    BtpSubProtocols requests = btpMessage.getSubProtocols();
    BtpSubProtocols responses = new BtpSubProtocols();

    int i = 0;
    for (BtpSubProtocol protocol : requests) {

      BtpSubProtocolHandler handler = this.registry.getHandler(protocol.getProtocolName());

      if (handler != null) {

        switch (protocol.getContentType()) {

          case MIME_APPLICATION_OCTET_STREAM:
            byte[] binaryResponse = handler.handleBinaryMessage(btpSession, i++, protocol.getData());
            responses.add(BtpSubProtocol.builder()
                .contentType(BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM)
                .protocolName(protocol.getProtocolName())
                .data(binaryResponse)
                .build()
            );
            break;

          case MIME_TEXT_PLAIN_UTF8:
            String textResponse = handler.handleTextMessage(btpSession, i++, protocol.getDataAsString());
            responses.add(BtpSubProtocol.builder()
                .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
                .protocolName(protocol.getProtocolName())
                .data(textResponse.getBytes(StandardCharsets.UTF_8))
                .build()
            );
            break;

          case MIME_APPLICATION_JSON:
            String jsonResponse = handler.handleJsonMessage(btpSession, i++, protocol.getDataAsString());
            responses.add(BtpSubProtocol.builder()
                .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
                .protocolName(protocol.getProtocolName())
                .data(jsonResponse.getBytes(StandardCharsets.UTF_8))
                .build()
            );
            break;
        }

      } else {
        //TODO WARNING - no handler
      }
    }

    session.sendMessage(getBinaryMessage(
        BtpResponse.builder()
            .requestId(btpMessage.getRequestId())
            .subProtocols(responses)
            .build()
    ));

  }

  private void authenticate(WebSocketSession session, BinaryMessage message) throws IOException {

    BtpMessage btpMessage = getBtpMessage(message);

    if (btpMessage.hasSubProtocol("auth")) {
      //This should be an auth message
      String auth_token = btpMessage.getSubProtocol("auth_token").getDataAsString();
      String auth_user = btpMessage.getSubProtocol("auth_username").getDataAsString();

      BtpSession.storeInWebSocketSession(session, auth_user, auth_token);

      session.sendMessage(
          getBinaryMessage(
              BtpResponse.builder()
                  .requestId(btpMessage.getRequestId())
                  .subProtocols(new BtpSubProtocols())
                  .build()
          )
      );

    } else {

      session.sendMessage(getBinaryMessage(
          BtpError.builder()
              .requestId(btpMessage.getRequestId())
              .errorCode(BtpErrorCode.F00_NotAcceptedError)
              .errorName("NotAcceptedError")
              .errorData(new byte[] {})
              .triggeredAt(Instant.now())
              .build()
      ));

    }

  }

  private BtpMessage getBtpMessage(BinaryMessage binaryMessage) throws IOException {

    ByteBuffer buffer = binaryMessage.getPayload();
    ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit());
    return context.read(BtpMessage.class, stream);

  }

  private BinaryMessage getBinaryMessage(BtpPacket packet) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    context.write(packet, baos);
    return new BinaryMessage(baos.toByteArray());

  }

}