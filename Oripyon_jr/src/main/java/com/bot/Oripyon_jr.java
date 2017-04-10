/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.bot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import com.bot.Enum.MultiKeyMap;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

@SpringBootApplication
@LineMessageHandler
public class Oripyon_jr {
	
	static HashMap<String, String[]> randomArrayCommand;
	static HashMap<String, String> binaryCommand;
	static HashMap<String, String> unaryCommand;
	
	static{
		try {
			TypeReference<HashMap<String, String>> typeMapString = new TypeReference<HashMap<String,String>>(){};
    		TypeReference<HashMap<String, String[]>> typeMapArray = new TypeReference<HashMap<String, String[]>>(){};
			ObjectMapper mapper = new ObjectMapper();
	    		
			binaryCommand = mapper.readValue(Oripyon_jr.class.getResourceAsStream("/command/binary.json"), typeMapString);
			unaryCommand = mapper.readValue(Oripyon_jr.class.getResourceAsStream("/command/unary.json"), typeMapString);
			randomArrayCommand = mapper.readValue(Oripyon_jr.class.getResourceAsStream("/command/randomArray.json"), typeMapArray);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Autowired
	private LineMessagingService lineMessagingService;
	
	int seed;
	Random random = new Random();
	
	
    public static void main(String[] args) {
        SpringApplication.run(Oripyon_jr.class, args);
    }

    @EventMapping
    public Message handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
    	
        String stringMessage = replyString(event);
        
        if(!StringUtils.isEmpty(stringMessage)){
        	return new TextMessage(stringMessage);
        }
        
        return null;
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        System.out.println("event: " + event);
    }
    
    private String replyString(MessageEvent<TextMessageContent> event){
	String message = event.getMessage().getText();
        
        try {
			if(message.startsWith("!")){
				String key = message.split(" ")[0].substring(1);
				String target = message.substring(key.length() + 1);
	
				if(event.getSource().getUserId() != null){
					UserProfileResponse sender = lineMessagingService.getProfile(event.getSource().getUserId()).execute().body();
					if(binaryCommand.get(key) != null && !StringUtils.isEmpty(target)){
						return binaryCommand.get(key).replace("@{}", target).replace("{}", sender.getDisplayName());
					}
					if(unaryCommand.get(key) != null){
						return unaryCommand.get(key).replace("{}", sender.getDisplayName());
					}
				}
	
				//key取不到value則檢查是否為multikey
				if(randomArrayCommand.get(key) == null){
					key = MultiKeyMap.getTrueKey(key);
				}
				String[] randomArray =  randomArrayCommand.get(key);
				if(randomArray != null){
					seed = random.nextInt(randomArray.length);
					return randomArray[seed];
				}
			}
        } catch (IOException e) {
          e.printStackTrace();
        }
        
        return null;
    }
	
}
