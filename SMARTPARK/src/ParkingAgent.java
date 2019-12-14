import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

import static utils.agentUtils.*;

public class ParkingAgent extends Agent {

    private int[] location;
    private boolean isFree;
    Random rand = new Random(); // Creating Random object.

    private Map<AID, int[]> carAgentLocations = new HashMap<>();

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");

        //Flag indicating parking availability
        isFree = rand.nextBoolean();
        location = initializeParkingLocation();


        addBehaviour(new SendCoordinates());
        addBehaviour(new SendAvailablePlaceInfo());
        addBehaviour(new COnfirmClientReservation());
        addBehaviour(new getReservationInfo());
        addBehaviour(new ConfirmClientCancellation());
        addBehaviour(new TrackCar());
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
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF).MatchConversationId("update-location");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
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
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP), MessageTemplate.MatchConversationId("offer-place"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if(isFree) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(Boolean.toString(isFree));
                }else{
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
                System.out.println("Sent reply with information about availability");
            } else {
                block();
            }
        }
    }

    /**
     * Send information about reservation to carAgent.
     */
    private class COnfirmClientReservation extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL).MatchConversationId("offer-place");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (isFree) {
                    reply.setPerformative(ACLMessage.INFORM);
                    isFree = false;
                } else {
                    //Send FAILURE if the place was booked faster by a different agent.
                    reply.setPerformative(ACLMessage.FAILURE);
                }

                myAgent.send(reply);
                System.out.println(("Sent reply with information about reservation."));
            } else {
                block();
            }
        }
    }

    /**
     * Send confirmation about cancelling reservation to carAgent.
     */
    private class ConfirmClientCancellation extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL).MatchConversationId("cancel-reservation");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);
                isFree = true;

                myAgent.send(reply);
                System.out.println(("Send info about cancelling the reservation. This place is available for further customers"));
            } else {
                block();
            }
        }
    }

    private class getReservationInfo extends CyclicBehaviour {
        private AID client_ID;
        private String client_name;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM).MatchConversationId("send-reservation-info");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //GET RESERVATION INFO
                client_ID = msg.getSender();
                client_name = client_ID.getName();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                myAgent.send(reply);
                System.out.println("Car Tracker got reservation info from Client");

                /* SUBSCRIBE FOR CAR LOCATION */
                ACLMessage sub = new ACLMessage(ACLMessage.SUBSCRIBE);
                sub.addReceiver(client_ID);
                System.out.println("I am going to subscribe to " + client_name);
                //variable to create conversation unique ID
                int conversationNumber = 0;
                sub.setConversationId("send-subscription-request");
                //inform
                sub.setContent("I, parking " + myAgent.getAID().getName() + " send request for subscription of " + client_name);
                System.out.println("I, parking " + myAgent.getAID().getName() + " send request for subscription of " + client_name);

                carAgentLocations.put(client_ID, null);
                sub.setContent("I, parking " + myAgent.getAID().getName() + " added " + client_name + " to track Car.");
                System.out.println("I, parking " + myAgent.getAID().getName() + " added " + client_name + " to track Car.");
                System.out.println(("Car Tracker got reservation into from Client"));
                myAgent.send(sub);
            } else {
                block();
            }

        }
    }

    private class TrackCar extends CyclicBehaviour {
        private AID client_ID;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM).MatchConversationId("send-location-info");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                client_ID = msg.getSender();
                int[] carLocation = parseLocation(msg.getContent());
                if (carAgentLocations.containsKey(client_ID)) {
                    carAgentLocations.put(client_ID, carLocation);
                    System.out.println("Location of " + client_ID.getName() + " to " + Arrays.toString(carLocation));
                }
            } else {
                block();
            }
        }
    }
}
