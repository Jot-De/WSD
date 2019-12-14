import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.agentUtils;

import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static utils.agentUtils.parseLocation;


/**
 * car agent class
 */
public class CarAgent extends Agent {

    private Map<AID, int[]> parkingAgentLocations = new HashMap<AID, int[]>();
    //Check if list of parkings has been updated.
    private boolean isParkingListUpdated = false;
    //Chceck if reservation was successful.
    private boolean isPlaceReservationSuccessful = false;
    //Agent's location.
    private int[] agentLocation = {0, 0};
    private int[] oldAgentLocation = {-1,-1};
    private AID parkingTarget;
    private boolean isPlaceAccepted = false;
    private int conversationNumber=0; //variable to create conversation uniqe ID

    private boolean isApproacher = false;
    private AID subscribingCarTracker_ID;
    boolean hasCarTracker = false;

    // List of other agents in the container.
    private AID[] parkingAgents = {
            new AID("parking1", AID.ISLOCALNAME),
            new AID("parking2", AID.ISLOCALNAME)
    };

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");

        addBehaviour(new sendReservationInfo());
        addBehaviour(new UpdateListOfParkings());
        addBehaviour(new CallForParkingOffers());
        //addBehaviour(new CancelClientReservation());
        addBehaviour(new ListenForLocationSubscriptionFromCarTracker());
        addBehaviour(new SendLocationInfo());
    }

    protected void takeDown() {
        System.out.println("Car-agent " + getAID().getName() + " terminating.");
    }

    /**
     * Get and cache locations of parking agents.
     * Part of MapParking Protocol.
     * TODO: Move this class to a separate file/package.
     */
    private class UpdateListOfParkings extends Behaviour {

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
                            parkingAgentLocations.put(sender, parseLocation(location));
                        }
                        repliesCnt++;
                        if (repliesCnt >= parkingAgents.length) {
                            // We received all replies so print locations.
                            System.out.println("Received locations from all parking agents.");
                            for (AID parkingAgent : parkingAgents) {
                                System.out.println("Location: " + Arrays.toString(parkingAgentLocations.get(parkingAgent)));
                            }
                            step = 2;
                            isParkingListUpdated = true;
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

    /**
     * Make a reservation for a parking space.
     * Part of PlaceReservation Protocol.
     * TODO: Move this class to a separate file/package.
     */
    private class CallForParkingOffers extends Behaviour {
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private int repliesCnt = 0;
        //Shortest path between car and parking.
        private double shortestDistance;
        private AID closestParking;
        private Map<AID, Boolean> parkingAgentAvailability = new HashMap<AID, Boolean>();

        public void action() {
            if (isParkingListUpdated) {
                switch (step) {
                    case 0:
                        //Send the cfp to parking agents.
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                        for (AID parkingAgent : parkingAgents) cfp.addReceiver(parkingAgent);

                        cfp.setConversationId("offer-place");
                        cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value.
                        myAgent.send(cfp);
                        System.out.println("Sent CFP to parking agents. Waiting for replies...");

                        //Prepare the template to get proposals.
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offer-place"),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                        step = 1;
                        break;
                    case 1:
                        //Receive info about availability of parking spaces.
                        ACLMessage reply = myAgent.receive(mt);

                        if (reply != null) {
                            //Reply received.
                            if(reply.getPerformative() == ACLMessage.PROPOSE) {
                                AID sender = reply.getSender();
                                parkingAgentAvailability.put(sender, true);

                                    //Change type of location from String to Array.
                                    int[] parkingLocation = parkingAgentLocations.get(sender);

                                    //Calculate the distance between car and parking on 2D plane.
                                    int x1 = agentLocation[0];
                                    int y1 = agentLocation[1];
                                    int x2 = parkingLocation[0];
                                    int y2 = parkingLocation[1];
                                    double distance = Math.hypot(x1 - x2, y1 - y2);

                                    if (closestParking == null || distance < shortestDistance) {
                                        //This is the best parking match for car agent.
                                        shortestDistance = distance;
                                        closestParking = reply.getSender();
                                    }

                            } else {
                                parkingAgentAvailability.put(reply.getSender(), false);
                            }

                            repliesCnt++;
                            if (repliesCnt >= parkingAgents.length) {
                                for (AID parkingAgent : parkingAgents) {
                                    if (parkingAgentAvailability.get(parkingAgent)) {
                                        System.out.println("Location: " + Arrays.toString(parkingAgentLocations.get(parkingAgent)) + " is available for reservation.");
                                    } else {
                                        System.out.println("Location: " + Arrays.toString(parkingAgentLocations.get(parkingAgent)) + " is occupied.");
                                    }
                                }
                                if (shortestDistance != 0) {
                                    /**
                                     * TODO: parking location equal to car location
                                     */
                                    System.out.println("Best Location: " + Arrays.toString(parkingAgentLocations.get(closestParking)) + " and shortestDistance is " + shortestDistance);
                                } else {
                                    /**
                                     * TODO: start again from step = 0
                                     */
//                                    step = 0;
//                                    break;
                                    System.out.println("there is no match");
                                    step = 4;
                                }
                                //All replies received.
                                step = 2;
                            }
                        } else {
                            // This method marks the behaviour as "blocked" so that agent does not
                            // schedule if for execution anymore.
                            // When a new message is inserted in the queue behaviour will automatically unblock itself.
                            block();
                        }
                        break;
                    case 2:
                        //Ask for reservation at the parking which provided the best offer.
                        ACLMessage reservation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

                        reservation.addReceiver(closestParking);
                        reservation.setConversationId("offer-place");
                        reservation.setReplyWith("reservation" + System.currentTimeMillis());
                        myAgent.send(reservation);

                        //Prepare the template to get the reply
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offer-place"),
                                MessageTemplate.MatchInReplyTo(reservation.getReplyWith()));
                        step = 3;
                        break;
                    case 3:
                        reply = myAgent.receive(mt);
                        if (reply != null) {
                            //Receive the place reservation order reply.
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                // Reservation successful.
                                parkingTarget = reply.getSender();
                                isPlaceAccepted = true;
                                System.out.println("Place reserved.");
                                isPlaceReservationSuccessful = true;
                            } else {
                                //Reservation failed.
                                System.out.println("Failure. Parking is occupied.");
                            }
                            step = 4;
                        } else {
                            block();
                        }
                        break;
                }
            }
        }

        public boolean done() { // if we return true this behaviour will end its cycle
            return step == 4;
        }
    }

    /*implementation of TrackReservation protocol*/
    private class sendReservationInfo extends Behaviour {
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            if (isPlaceAccepted) {
                switch (step) {
                    case 0:
                        //Send the cfp to parking agents.
                        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);

                        inform.addReceiver(parkingTarget);
                        inform.setConversationId("send-reservation-info");
                        inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique value.
                        inform.setContent("My ID " + myAgent.getAID() + " . My parking: " + parkingTarget);

                        myAgent.send(inform);
                        isApproacher = true;
                        System.out.println("SendReservationInfo: Client sent reservation info to carTracker.");
                        step = 1;
                        break;
                }
            }
        }

        public boolean done() { // if we return true this behaviour will end its cycle
            return step == 1;
        }
    }

    /**
     * Cancel a reservation for a parking space.
     * Part of CancelReservation Protocol.
     * TODO: Move this class to a separate file/package.
     */

    /*
    private class CancelClientReservation extends Behaviour {
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            if (isPlaceReservationSuccessful) {
                switch (step) {
                    case 0:
                        //Send a request to cancel reservation.
                        ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                        cancel.addReceiver(parkingTarget);
                        cancel.setConversationId("cancel-reservation");
                        cancel.setReplyWith("cancel" + System.currentTimeMillis()); // Unique value.
                        myAgent.send(cancel);
                        System.out.println("Sent CANCEL to target parking.");

                        //Prepare the template to get proposals.
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("cancel-reservation"),
                                MessageTemplate.MatchInReplyTo(cancel.getReplyWith()));
                        step = 1;
                        break;
                    case 1:
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null) {
                            //Receive the place reservation order reply.
                            if (reply.getPerformative() == ACLMessage.CONFIRM) {
                                // Reservation successful.
                                isPlaceAccepted = false;
                                System.out.println("Reservation cancelled.");
                                step = 2;
                            }
                        } else {
                            block();
                        }
                        break;
                }
            }
        }

        public boolean done() { // if we return true this behaviour will end its cycle
            return step == 2;
        }
    }
    */
    private class ListenForLocationSubscriptionFromCarTracker extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE).MatchConversationId("send-subscription-request");
            ACLMessage msg = myAgent.receive(mt);
            if (isApproacher) {
                if (msg != null) {
                    subscribingCarTracker_ID = msg.getSender();
                    hasCarTracker = true;
                    System.out.println(subscribingCarTracker_ID.getName() + "has subscribed for info about my location");
                } else {
                    block();
                }
            }
        }
    }

    private class SendLocationInfo extends CyclicBehaviour {

        private boolean carHasMoved;
        public void action() {
            //TODO make the car move
            carHasMoved = oldAgentLocation[0] != agentLocation[0] && oldAgentLocation[1] != agentLocation[1];
            if (hasCarTracker) {
                if (carHasMoved) {
                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);

                    inform.addReceiver(subscribingCarTracker_ID);
                    inform.setConversationId("send-location-info");
                    inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique value.
                    inform.setContent(Arrays.toString(agentLocation));
                    oldAgentLocation = agentLocation; //update oldAgentLocation
                    System.out.println("My location info is " + Arrays.toString(oldAgentLocation));
                    myAgent.send(inform);
                } else {
                    block();
                }
            }
        }
    }
}


