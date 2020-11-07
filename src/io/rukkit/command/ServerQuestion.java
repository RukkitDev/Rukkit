package io.rukkit.command;

import io.rukkit.entity.*;

public class ServerQuestion
 {
	public Player from;
	public String responce;
	public int type;
	public ServerQuestionCallback callback;
	//public int qid;
	public ServerQuestion(Player fromPlayer, String responeString, ServerQuestionCallback callback/*,int qid*/) {
		this.from = fromPlayer;
		this.responce = responeString;
		this.callback = callback;
		//this.qid = qid;
	}

	public ServerQuestion(Player fromPlayer, ServerQuestionCallback callback/*,int qid*/) {
		this.from = fromPlayer;
		//this.responce = responeString;
		this.callback = callback;
		//this.qid = qid;
	}
}
