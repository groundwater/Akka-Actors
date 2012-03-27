package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.AkkaPromise

import akka.actor.{Actor,ActorRef,Props}

// Necessary for `actor ? message`
import akka.pattern.ask 

import akka.util.Timeout
import akka.util.Duration

// Use the Applications Default Actor System
import play.libs.Akka.system

// Possible Requests to Our Actors
case class Get(path:String)

// Application with dependency `ActorRef`
class Application( val actor: ActorRef ) extends Controller {
    
    // Implicit argument required by `actor ?`
    implicit val timeout : Timeout = Timeout(Duration(5,"seconds"))
    
    // `index` is Possible router destination defined in conf/routes
    def index = Action { implicit request =>
        
        // Async necessary because actor messaging is not synchronous
        Async {
            new AkkaPromise( actor ? Get(request.path) ) map {
                
                // These cases are processed when the future promise is fulfilled
                
                // Our delegate returned something we can use
                case Content(title,body) => Ok( views.html.index(title,body) )
                
                // Our delegate returned a known error
                case Error(reason) => BadRequest(reason)
                
                // Our delegate returned unknown data
                case _ => InternalServerError
                
            }
        }
        
    }
}

// Application Dependency Injector
trait ApplicationContext {
    
    // To be injected by a mixing bowl
    val applicationActor: ActorRef 
    
    // This _must_ be a `def` otherwise it will not inject properly
    def application: Application = new Application(applicationActor)
    
}

// Possible Replies by Our Actor
case class Content(title: String, body: String)
case class Error(reason: String)

class Getter extends Actor {
    def receive = {
        
        // We can handle Get messages
        case Get(path) => 
            sender ! Content(
                title = "Play[2.0] and Akka[2.0]",
                body = "Welcome to your new Actor driven Play website!"
            )
        
        // Unknown Message Handling
        case _ =>
            sender ! Error("Unknown Request")
        
    }
}

// This is our mixing bowl where we throw our ingredients together
object Main extends ApplicationContext {
    
    // Dependency required by the `ApplicationContext`
    // This is our one and only ingredient
    val applicationActor = system.actorOf( Props[Getter], name="application" )
    
    // More ingredients/dependencies would go here, in the same format as above
    
}

