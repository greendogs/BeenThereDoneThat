package com.example.janebabra.beentheredonethat;

import android.app.Activity;

public class QuizActivity extends Activity {

    // Game preference values
    public static final String GAME_PREFERENCES = "GamePrefs";
    public static final String GAME_PREFERENCES_NICKNAME = "Nickname"; // String
    public static final String GAME_PREFERENCES_EMAIL = "Email"; // String
    public static final String GAME_PREFERENCES_PASSWORD = "Password"; // String
    public static final String GAME_PREFERENCES_DOB = "DOB"; // Long
    public static final String GAME_PREFERENCES_GENDER = "Gender";  // Integer, in array order: Male (1), Female (2), and Undisclosed (0)
    public static final String GAME_PREFERENCES_SCORE = "Score"; // Integer
    public static final String GAME_PREFERENCES_CURRENT_QUESTION = "CurQuestion"; // Integer
    public static final String GAME_PREFERENCES_AVATAR = "Avatar"; // String URL to image
    public static final String GAME_PREFERENCES_FAV_PLACE_NAME = "FavPlaceName"; // String
    public static final String GAME_PREFERENCES_FAV_PLACE_LONG = "FavPlaceLong"; // float
    public static final String GAME_PREFERENCES_FAV_PLACE_LAT = "FavPlaceLat"; // float
    public static final String GAME_PREFERENCES_PLAYER_ID = "ServerId"; // Integer


    // Question XML Tag Names
    public static final String XML_TAG_QUESTION_BLOCK = "questions";
    public static final String XML_TAG_QUESTION = "question";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_NUMBER = "number";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_TEXT = "text";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_IMAGEURL = "imageUrl";
    public static final int QUESTION_BATCH_SIZE = 5;

    public static final String DEBUG_TAG = "Been There, Done That! Activity Log";
    
    // Server URLs
    public static final String TRIVIA_SERVER_BASE = "http://tqs.mamlambo.com/";
    public static final String TRIVIA_SERVER_SCORES = TRIVIA_SERVER_BASE + "scores.jsp";
    public static final String TRIVIA_SERVER_QUESTIONS = TRIVIA_SERVER_BASE + "questions.jsp";
    public static final String TRIVIA_SERVER_ACCOUNT_EDIT = TRIVIA_SERVER_BASE + "receive";
    public static final String TRIVIA_SERVER_FRIEND_ADD = TRIVIA_SERVER_BASE + "friend";
   
}