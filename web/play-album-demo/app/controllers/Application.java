package controllers;

import play.mvc.Controller;

public class Application extends Controller {

    /**
     * Default action.
     * renders the app/views/Application/index.html template
     */
    public static void index() {
        render();
    }
    
}
