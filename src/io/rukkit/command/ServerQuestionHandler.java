package io.rukkit.command;

import java.util.ArrayList;
import java.util.HashMap;

import io.rukkit.entity.Player;

public class ServerQuestionHandler {
	public static volatile ArrayList<ServerQuestion> questionQuere = new ArrayList<ServerQuestion>();
	//public HashMap<String, Question> questionQuere;
	
	
	public static int addQuestion(ServerQuestion question) {
		questionQuere.add(question);
		return questionQuere.indexOf(question);
	}
	
	public static ServerQuestion getQuestionById(int id) {
		return questionQuere.get(id);
	}
}
