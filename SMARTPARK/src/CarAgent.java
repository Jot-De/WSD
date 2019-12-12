import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;


/**
 * car agent class
 */
public class CarAgent extends Agent {

    private Map<AID, String> parkingAgentLocations = new HashMap<AID, String>();
    //Check if list of parkings has been updated.
    private boolean isParkingListUpdated = false;
    //Chceck if reservation was successful.
    private boolean isPlaceReservationSuccessful = false;
    //Agent's location.
    private int[] agentLocation = {0, 0};
    private AID parkingTarget;
    private boolean isPlaceAccepted = false;
    private int conversationNumber=0; //variable to create conversation uniqe ID

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
        addBehaviour(new CancelClientReservation());
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
                            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                AID sender = reply.getSender();
                                Boolean isParkingFree = Boolean.parseBoolean(reply.getContent());
                                parkingAgentAvailability.put(sender, isParkingFree);

                                if (isParkingFree) {
                                    //Change type of location from String to Array.
                                    String arr = parkingAgentLocations.get(sender);
                                    String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                                    int[] results = new int[items.length];
                                    //Create an Array.
                                    for (int i = 0; i < items.length; i++) {
                                        try {
                                            results[i] = Integer.parseInt(items[i]);
                                        } catch (NumberFormatException nfe) {
                                            System.out.println("Error occurred");
                                        }
                                    }

                                    //Calculate the distance between car and parking on 2D plane.
                                    int x1 = agentLocation[0];
                                    int y1 = agentLocation[1];
                                    int x2 = results[0];
                                    int y2 = results[1];
                                    double distance = Math.hypot(x1 - x2, y1 - y2);

                                    if (closestParking == null || distance < shortestDistance) {
                                        //This is the best parking match for car agent.
                                        shortestDistance = distance;
                                        closestParking = reply.getSender();
                                    }
                                }
                            }
                            repliesCnt++;
                            if (repliesCnt >= parkingAgents.length) {
                                for (AID parkingAgent : parkingAgents) {
                                    if (parkingAgentAvailability.get(parkingAgent)) {
                                        System.out.println("Location: " + parkingAgentLocations.get(parkingAgent) + " is available for reservation.");
                                    } else {
                                        System.out.println("Location: " + parkingAgentLocations.get(parkingAgent) + " is occupied.");
                                    }
                                }
                                if (shortestDistance != 0) {
                                    /**
                                     * TODO: parking location equal to car location
                                     */
                                    System.out.println("Best Location: " + parkingAgentLocations.get(closestParking) + " and shortestDistance is " + shortestDistance);
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
                        inform.setConversationId("send-reservation-info-" + myAgent.getAID() + conversationNumber);
                        inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique value.
                        //TODO zamienić id parkingu na id konkretnego miejsca/wsp miejsca/inne  pomysły
                        inform.setContent("Moje ID " + myAgent.getAID() + " . Mój parking: " + parkingTarget);

                        myAgent.send(inform);
                        System.out.println("Client sent reservation info to car tracker.");
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
    }

