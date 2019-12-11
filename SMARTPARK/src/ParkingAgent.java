import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

import static utils.agentUtils.initializeParkingLocation;
import static utils.agentUtils.freeParkingLocation;

public class ParkingAgent extends Agent {

    private int[] location;
    private boolean isFree;
    Random rand = new Random(); // Creating Random object.

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");

        //Flag indicating parking availability
        isFree = rand.nextBoolean();
        location = initializeParkingLocation();


        addBehaviour(new SendCoordinates());
        addBehaviour(new SendAvailablePlaceInfo());
        addBehaviour(new ConfirmReservation());
    }

    protected void takeDown() {
        freeParkingLocation(location);
        System.out.println("Parking-agent " + getAID().getName() + " terminating.");
    }


    /**
     * Send coordinates to carAgent.
     */
    private class SendCoordinates extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Message received. Process it
                String conversationID = msg.getConversationId();

                // Return early if conversation id is not set to update-location.
                if (!conversationID.equals("update-location")) return;

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(Arrays.toString(location));

                myAgent.send(reply);
                System.out.println("Sent reply with location to car agent.");
            } else {
                // This method marks the behaviour as "blocked" so that agent does not
                // schedule if for execution anymore.
                // When a new message is inserted in the queue behaviour will automatically unblock itself.
                block();
            }
        }

    }
    /**
     * Send information about availability to carAgent.
     */
    private class SendAvailablePlaceInfo extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //Message received. Process it
                String conversationID = msg.getConversationId();

                //Return early if conversation id is not set to offer-place.
                if (!conversationID.equals("offer-place")) return;

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Boolean.toString(isFree));

                myAgent.send(reply);
                System.out.println("Sent reply with information about availability");
            }
            else{
                block();
            }
        }
    }
    /**
     * Send information about reservation to carAgent.
     */
    private class ConfirmReservation extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //ACCEPT_PROPOSAL Message received. Process it
                String conversationID = msg.getConversationId();

                //Return early if conversation id is not set to offer-place.
                if (!conversationID.equals("offer-place")) return;

                ACLMessage reply = msg.createReply();

                if(isFree){
                    reply.setPerformative(ACLMessage.INFORM);
                } else{
                    //Send FAILURE if the place was booked faster by a different agent.
                    reply.setPerformative(ACLMessage.FAILURE);
                }

                myAgent.send(reply);
                System.out.println(("Sent reply with information about reservation"));
            }
            else{
                block();
            }
        }
    }
}
