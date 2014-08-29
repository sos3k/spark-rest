package de.kp.spark.rest.actor
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Spark-REST project
* (https://github.com/skrusche63/spark-rest).
* 
* Spark-REST is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Spark-REST is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Spark-REST. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import akka.actor.{Actor,ActorLogging,ActorRef,Props}

import akka.pattern.ask
import akka.util.Timeout

import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.routing.RoundRobinRouter

import de.kp.spark.rest.{Configuration,MiningMessage,MiningResponse,ResponseStatus}
import de.kp.spark.rest.mining.MiningContext

import scala.concurrent.duration.DurationInt

class MiningMaster extends Actor with ActorLogging {
  
  /* Load configuration for routers */
  val (time,retries,workers) = Configuration.router   
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries=retries,withinTimeRange = DurationInt(time).minutes) {
    case _ : Exception => SupervisorStrategy.Restart
  }

  val mc = new MiningContext()
  val miningRouter = context.actorOf(Props(new MiningActor(mc)).withRouter(RoundRobinRouter(workers)))

  def receive = {
    
    case req:MiningMessage => {
      
      implicit val ec = context.dispatcher

      val duration = Configuration.actor      
      implicit val timeout:Timeout = DurationInt(duration).second
	  	    
	  val origin = sender
      val response = ask(miningRouter, req).mapTo[MiningResponse]
      
      response.onSuccess {
        case result => origin ! result
      }
      response.onFailure {
        case result => origin ! new MiningResponse(ResponseStatus.FAILURE)	      
	  }
      
    }
  
    case _ => {}
    
  }
  
}