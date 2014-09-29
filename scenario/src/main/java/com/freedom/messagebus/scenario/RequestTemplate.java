package com.freedom.messagebus.scenario;

import com.freedom.messagebus.client.*;
import com.freedom.messagebus.common.message.Message;
import com.freedom.messagebus.common.message.MessageFactory;
import com.freedom.messagebus.common.message.MessageType;
import com.freedom.messagebus.common.message.messageBody.AppMessageBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestTemplate {

    private static final Log    logger = LogFactory.getLog(RequestTemplate.class);
    private static final String appkey = "LAJFOWFALSKDJFALLKAJSDFLKSDFJLWKJ";
    private static final String host   = "115.29.96.85";
    private static final int    port   = 2181;

    public static void main(String[] args) {
        Messagebus messagebus = Messagebus.getInstance(appkey);
        messagebus.setZkHost(host);
        messagebus.setZkPort(port);

        Message msg = MessageFactory.createMessage(MessageType.AppMessage);
        String queueName = "crm";

        AppMessageBody appMessageBody = (AppMessageBody) msg.getMessageBody();
        appMessageBody.setMessageBody("test".getBytes());

        Message respMsg = null;

        try {
            messagebus.open();
            IRequester requester = messagebus.getRequester();

            respMsg = requester.request(msg, queueName, 10);
            //use response message...
            logger.info("response message : [" + respMsg.getMessageHeader().getMessageId() + "]");
        } catch (MessagebusConnectedFailedException | MessagebusUnOpenException |
            MessageResponseTimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            messagebus.close();
        }
    }

}
