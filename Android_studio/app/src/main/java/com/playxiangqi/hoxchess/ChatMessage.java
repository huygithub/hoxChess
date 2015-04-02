/**
 * http://javapapers.com/android/android-chat-bubble/
 */
package com.playxiangqi.hoxchess;

public class ChatMessage {
	public boolean left;
	public String message;

	public ChatMessage(boolean left, String message) {
		super();
		this.left = left;
		this.message = message;
	}
}