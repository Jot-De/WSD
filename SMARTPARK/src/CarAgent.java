import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * car agent class
 */
public class CarAgent extends Agent {

    private Map<AID, String> parkingAgentLocations = new HashMap<AID, String>();

    // List of other agents in the container.
    private AID[] parkingAgents = {
            new AID("parking1", AID.ISLOCALNAME),
            new AID("parking2", AID.ISLOCALNAME)
    };

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");
        
        addBehaviour(new updateListOfParkings());
    }

    protected void takeDown() {
        System.out.println("Car-agent " + getAID().getName() + " terminating.");
    }

    /**
     * Get and cache locations of parking agents.
     * Part of MapParking Protocol.
     * TODO: Move this class to a separate file/package.
     */
    private class updateListOfParkings extends Behaviour {

        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);

                    for (AID parkingAgent : parkingAgents) msg.addReceiver(parkingAgent);

                    msg.setConversationId("update-location");
                    msg.setReplyWith("inform_ref" + System.currentTimeMillis()); // Unique value
                    myAgent.send(msg);

                    // Prepare the template to get replies.
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("update-location"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    step = 1;
                    System.out.println("Sent INFORM_REF to parking agents. Waiting for replies...");
                    break;
                case 1:
                    // Receive all locations from parking agents.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            AID sender = reply.getSender();
                            String location = reply.getContent();
                            parkingAgentLocations.put(sender, location);
                        }
                        repliesCnt++;
                        if (repliesCnt >= parkingAgents.length) {
                            // We received all replies so print locations.
                            System.out.println("Received locations from all parking agents.");
                            for (AID parkingAgent : parkingAgents) {
                                System.out.println("Location: " + parkingAgentLocations.get(parkingAgent));
                            }
                            step = 2;
                        }
                    } else {
                        // This method marks the behaviour as "blocked" so that agent does not
                        // schedule if for execution anymore.
                        // When a new message is inserted in the queue behaviour will automatically unblock itself.
                        block();
                    }
                    break;
            }
        }

        public boolean done() { // if we return true this behaviour will end its cycle
            return step == 2;
        }
    }
}