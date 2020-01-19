import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;

import static utils.agentUtils.sendCarData;
import java.util.*;

import static utils.agentUtils.*;

public class ParkingAgent extends Agent {

    private int[] location;

    private int[] previousCarLocation={-99,-99};
    private int freeParkingSlots = 3;
    private boolean isFree;
    Random rand = new Random(); // Creating Random object.

    private Map<AID, int[]> carAgentLocations = new HashMap<>();

    public static String consoleIndentation = "\t\t";

    private boolean isConnectedToDatabase = false;

    //zmienna tylko do testu
    private boolean test=true;

    private void decreaseFreeParkingSlotValue() {
        if (freeParkingSlots > 0) {
            freeParkingSlots--;
        }

        if (freeParkingSlots == 0) {
            isFree = false;
        }

        try {
            sendParkingData(getAID().getName(), "parking", Arrays.toString(location), Integer.toString(freeParkingSlots));
            isConnectedToDatabase = true;
        } catch (Exception e) {
            System.out.println("Database ERROR");
        }
    }

    private void increaseFreeParkingSlotValue() {
        freeParkingSlots++;
        isFree = true;
        try {
            sendParkingData(getAID().getName(), "parking", Arrays.toString(location), Integer.toString(freeParkingSlots));
            isConnectedToDatabase = true;
        } catch (Exception e) {
            System.out.println("Database ERROR");
        }
    }

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");

        //Flag indicating parking availability
        if (freeParkingSlots > 0) {
            isFree = true;
        } else {
            isFree = false;
        }

        location = initializeParkingLocation();

        System.out.println(Arrays.toString(location));


        // Send position to the middleware server.
        try {
            sendParkingData(getAID().getName(), "parking", Arrays.toString(location), Integer.toString(freeParkingSlots));
            isConnectedToDatabase = true;
        } catch (Exception e) {
            System.out.println("Database ERROR");
        }

        addBehaviour(new SendCoordinates());
        addBehaviour(new SendAvailablePlaceInfo());
        addBehaviour(new ConfirmClientReservation());
        addBehaviour(new GetReservationInfo());
        addBehaviour(new ConfirmClientCancellation());
        addBehaviour(new TrackCar());
        addBehaviour(new CancelReservationIncreaseDistance());
    }

    protected void takeDown() {
        freeParkingLocation(location);
        System.out.println("Parking-agent " + getAID().getName() + " terminating.");
        if (isConnectedToDatabase) {
            try {
                removeAgentFromDatabase(getAID().getName());
            } catch (Exception e) {
                System.out.println("Database ERROR");
            }
        }
    }


    /**
     * Send coordinates to carAgent.
     * Part of MapParkings protocol.
     */
    private class SendCoordinates extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF),
                    MessageTemplate.MatchConversationId("update-location"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(Arrays.toString(location));

                System.out.println(myAgent.getName() + consoleIndentation + "Sent reply with location to car agent.");
                myAgent.send(reply);
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
     * Part of PlaceReservation protocol.
     */
    private class SendAvailablePlaceInfo extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("offer-place"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if(isFree) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(Boolean.toString(isFree));
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }

                System.out.println(myAgent.getName()+ consoleIndentation + "Sent reply with information about availability");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Send information about reservation to carAgent.
     * Part of PlaceReservation protocol.
     */
    private class ConfirmClientReservation extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("offer-place"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (isFree) {
                    decreaseFreeParkingSlotValue();
                    reply.setPerformative(ACLMessage.INFORM);
                } else {
                    reply.setPerformative(ACLMessage.FAILURE);
                }

                System.out.println(myAgent.getName() + consoleIndentation + "Sent reply with information about reservation.");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Part of TrackReservation protocol.
     */
    private class GetReservationInfo extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("send-reservation-info"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //GET RESERVATION INFO
                AID client_ID = msg.getSender();
                String client_name = client_ID.getName();

                carAgentLocations.put(client_ID, null);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                System.out.println(myAgent.getName() + consoleIndentation + "Got reservation info from " + client_name);
                myAgent.send(reply);

                /**
                 *
                 *  SUBSCRIBE FOR CLIENT LOCATION
                 *
                 */
                ACLMessage sub = new ACLMessage(ACLMessage.SUBSCRIBE);
                sub.addReceiver(client_ID);
                sub.setConversationId("send-subscription-request");
                sub.setReplyWith("subscribe" + System.currentTimeMillis());

                sub.setContent("I, parking " + myAgent.getAID().getName() + " added " + client_name + " to track Car.");
                System.out.println(myAgent.getName() + consoleIndentation + "I am tracking now " + client_name);

                myAgent.send(sub);
            } else {
                block();
            }

        }
    }

    /**
     * Part of TrackCar protocol.
     */
    private class TrackCar extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("send-location-info"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                AID client_ID = msg.getSender();
                int[] carLocation = parseLocation(msg.getContent());
                if (carAgentLocations.containsKey(client_ID)) {
                    carAgentLocations.put(client_ID, carLocation);
                    System.out.println(myAgent.getName() + consoleIndentation + "Current location of " + client_ID.getName() + " is " + Arrays.toString(carLocation));
                    if (calculateDistance(location,carLocation) > calculateDistance(location, previousCarLocation))
                    {   if(test){
                            ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                            cancel.addReceiver(client_ID);
                            cancel.addReceiver(myAgent.getAID());
                            cancel.setConversationId("cancel-reservation-ct");
                            cancel.setReplyWith("cancel" + System.currentTimeMillis()); // Unique value.
                            myAgent.send(cancel);
                            System.out.println(myAgent.getName() + consoleIndentation + "Car agent is going far away. Reservation cancelled.");
                            System.out.println(myAgent.getName() + consoleIndentation + "Sent CANCEL to target parking-myself.");
                            test=false;
                    }else {block();}}
                    previousCarLocation = carLocation;
                    if (carLocation[0] == location[0] && carLocation[1] == location[1]) {
                        // Cancel communication if car arrived at the parking.
                        ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                        cancel.setConversationId("send-subscription-cancel");
                        cancel.addReceiver(client_ID);
                        myAgent.send(cancel);
                    }
                } else {
                    System.out.println(myAgent.getName() + consoleIndentation + "Received information from car we do not track, ignore it.");
                }
            } else {
                block();
            }
        }
    }

    /**
     * Send confirmation about cancelling reservation to carAgent.
     * Part of ReservationCancellation protocol.
     */
    private class ConfirmClientCancellation extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CANCEL),
                    MessageTemplate.MatchConversationId("cancel-reservation"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);
                increaseFreeParkingSlotValue();

                System.out.println(myAgent.getName() + consoleIndentation + "Send info about cancelling the reservation. This place is available for further customers");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class CancelReservationIncreaseDistance extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CANCEL),
                    MessageTemplate.MatchConversationId("cancel-reservation-ct"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);
                increaseFreeParkingSlotValue();

                System.out.println(myAgent.getName() + consoleIndentation + "Send info about cancelling the reservation. Car increased distance.");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}
