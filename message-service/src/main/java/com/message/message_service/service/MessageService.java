package com.message.message_service.service;

import com.message.message_service.entity.Message;
import com.message.message_service.web.data.MessageDto;

import java.util.List;

public interface MessageService {

      List<MessageDto> getMessageBySenderAndReceiverId(Long senderId, Long receiverId);

      MessageDto saveNewMessage(MessageDto messageDto);

      void updateMessageDeliveryStatus(Long messageId);

     void updateMessageReadStatus(Long messageId);

}
