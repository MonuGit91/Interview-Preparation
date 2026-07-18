package com.supai.app.otcsapis.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class OtdsTokenResponseDto {
	private String token;
	private String userId;
	private String ticket;
	private String resourceID;
	private String failureReason;
	private String passwordExpirationTime;
	private String continuation;
	private String continuationContext;
	private String continuationData;
	
}
/*
 * Sample:
{
    "token": "6F7464735F73657373696F6E5F6B6579",
    "userId": "iv_api_client@OAuthClients",
    "ticket": "*OTDSSSO*AbhBQlRKcWhxU0Z4Z1V3Ml9sY0UtOXQwQnRlajRjZEFBTXh6OVJ6enpsQUtYS3BkYlNBU0tlWGJ6QnRaSEh2aThqb2psYjBxZ3NmZXctU1JhVGNFdjJjNlZIUWFzVmNodGtQV1BtMVNJZWd2N3ZfMGJGa0pROTFTU29rWVZBVzJ6QVY2aVFHRHFSYm9jWWNWLWFoY0lHcXN1a3l6aDVhYlZjMHJCU281eUI3RXR1aFoyWDlqTmdtUHowdmY0dTZRakNpa2N5Z2VvZDRURmJLVXBLSmpLSV9nalFhcXB3QmxJWlpwQ2JScmJrV0VHM0s4TW5aMUYycUVfTGdhOENKMW1UaUN2dUVwVmZSVE1DSWVfeGxWSmN1N2ZtYWpGSDNZcXdtZ3ljZTQ5OU1jWFNHSDNRcXl6a1pQMnhqcTR5TlNxZTQ4RTVraThjUm5UNjlxeWotMVBHa01MYmxYVW9hSVczMnBWMHpWbzZLT1V0bktFSERmQnJUbm9GRndhM2NibkRGUFdFUFVKMkd0bTVIMW54WUpNOTVwaXItdHEwWUcyREpQcVI3bXpaek4yQWZQUUNlMTAyMXcqKgBOAEoAFIiQgWaLyZAP1_ON9nMQHLX7wkNWABAocUIBMRGSAqgdnH6axyBiACDyK0R6dgItnd0cX05v-01hJglvxbQR70JQS7kFcpvjQAAA",
    "resourceID": null,
    "failureReason": null,
    "passwordExpirationTime": 0,
    "continuation": false,
    "continuationContext": null,
    "continuationData": null
}
*/
