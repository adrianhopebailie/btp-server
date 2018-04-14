package com.hopebailie.btp.server;

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocolContentType;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.btp.BtpTransfer;
import org.interledger.btp.asn.codecs.AsnBtpErrorCodec;
import org.interledger.btp.asn.codecs.AsnBtpMessageCodec;
import org.interledger.btp.asn.codecs.AsnBtpPacketCodec;
import org.interledger.btp.asn.codecs.AsnBtpResponseCodec;
import org.interledger.btp.asn.codecs.AsnBtpSubProtocolCodec;
import org.interledger.btp.asn.codecs.AsnBtpSubProtocolsCodec;
import org.interledger.btp.asn.codecs.AsnBtpTransferCodec;
import org.interledger.btp.asn.framework.BtpCodecs;
import org.interledger.core.InterledgerPacket;
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
import java.time.Instant;

@Component
public class BtpSocketHandler extends BinaryWebSocketHandler {

  CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(BtpError.class, AsnBtpErrorCodec::new)
        .register(BtpMessage.class, AsnBtpMessageCodec::new)
        .register(BtpPacket.class, AsnBtpPacketCodec::new)
        .register(BtpResponse.class, AsnBtpResponseCodec::new)
        .register(BtpSubProtocol.class, AsnBtpSubProtocolCodec::new)
        .register(BtpSubProtocols.class, AsnBtpSubProtocolsCodec::new)
        .register(BtpTransfer.class, AsnBtpTransferCodec::new);

  public void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws InterruptedException, IOException {

    authenticate(session, message);

  }

  public InterledgerPacket handleInterledgerPacket(String account, InterledgerPacket packet) {
    throw new RuntimeException("Not implemented.");
  }

  private void authenticate(WebSocketSession session, BinaryMessage message) throws IOException {

    //Do we have a session?
    //TODO Use user principal
    if (!session.getAttributes().containsKey("account")) {

      BtpMessage btpMessage = getBtpMessage(message);

      if(btpMessage.hasSubProtocol("auth")) {
        //This should be an auth message
        String auth_token = btpMessage.getSubProtocol("auth_token").getDataAsString();
        String auth_user = btpMessage.getSubProtocol("auth_username").getDataAsString();

        session.getAttributes().put("account", auth_token);

        session.sendMessage( getBinaryMessage(
            BtpMessage.builder()
            .requestId(btpMessage.getRequestId())
            .subProtocols(
                BtpSubProtocols.fromPrimarySubProtocol(
                  BtpSubProtocol.builder()
                    .protocolName("ilp")
                    .contentType(BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM)
                    .build())
            )
            .build()
        )  );

      } else {

        session.sendMessage( getBinaryMessage(
            BtpError.builder()
                .requestId(btpMessage.getRequestId())
                .errorCode(BtpErrorCode.F00_NotAcceptedError)
                .errorName("NotAcceptedError")
                .errorData(new byte[]{})
                .triggeredAt(Instant.now())
                .build()
        )); 

      }

      //TODO return error response
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