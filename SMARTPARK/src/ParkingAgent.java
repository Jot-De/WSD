import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.Arrays;

import static utils.agentUtils.getRandomLocation;
import static utils.agentUtils.sendData;

public class ParkingAgent extends Agent {

    private int[] location;

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");

        location = getRandomLocation();

        // Send position to the middleware server.
        try {
            sendData(getAID().getName(), "parking", Arrays.toString(location));
        } catch (Exception e) {
            System.out.println("ERROR");
        }

        addBehaviour(new SendCoordinates());
    }

    protected void takeDown() {
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
}
