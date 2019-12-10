import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
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
    private Map<AID, String> parkingAgentAvailability = new HashMap<AID, String>();
    //
    private boolean isParkingListUpdated = false;
    //Agents location
    private int[] location = {0, 0};

    // List of other agents in the container.
    private AID[] parkingAgents = {
            new AID("parking1", AID.ISLOCALNAME),
            new AID("parking2", AID.ISLOCALNAME)
    };

    protected void setup() {
        // Print a welcome message.
        System.out.println("Hello " + getAID().getName() + " is ready.");


        addBehaviour(new updateListOfParkings());
        addBehaviour(new callForParkingOffers());
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

    private class callForParkingOffers extends Behaviour {
        private MessageTemplate mt;
        private int step = 0;
        private int repliesCnt = 0;

        public void action() {
            if (isParkingListUpdated) {
                switch (step) {
                    case 0:
                        //Send the cfp to all parking
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                        for (AID parkingAgent : parkingAgents) cfp.addReceiver(parkingAgent);

                        cfp.setConversationId("offer-place");
                        cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                        myAgent.send(cfp);

                        //Prepare the template to get proposals
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offer-place"),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                        step = 1;
                        System.out.println("Sent CFP to parking agents. Waiting for replies...");
                        break;
                    case 1:
                        //Receive all proposals/refusals from parking agents
                        ACLMessage reply = myAgent.receive(mt);

                        if (reply != null) {
                            //Reply received
                            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                AID sender = reply.getSender();
                                String isFree = reply.getContent();
                                parkingAgentAvailability.put(sender, isFree);
                                repliesCnt++;
                            }
                            if (repliesCnt >= parkingAgents.length) {
                                for (AID parkingAgent : parkingAgents) {
                                    System.out.println("Location: " + parkingAgentLocations.get(parkingAgent) + " " + parkingAgentAvailability.get(parkingAgent));
                                }
                                //All replies received
                                System.out.println("Received place proposals from all parking agents.");

                                step = 2;
                            }
                        } else {
                            block();
                        }
                        break;
//                case 2:
//                    //Send the place reservation order to the parking that provided the best offer
//                    ACLMessage reservation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
//                    //reservation.addReceiver(bestParking);
//                    //reservation.setContent();
//                    reservation.setConversationId("offer-place");
//                    reservation.setReplyWith("reservation"+System.currentTimeMillis());
//                    myAgent.send(reservation);
//                    //Prepare the template to get the purchase order reply
//                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offer-place"),
//                                            MessageTemplate.MatchInReplyTo(reservation.getReplyWith()));
//                    step = 3;
//                    break;
//                case 3:
//                    //Receive the place reservation order reply
//                    reply = myAgent.receive(mt);
//                    if (reply != null){
//                        // place reservation order reply received
//                        if (reply.getPerformative() == ACLMessage.INFORM){
//                            // Reservation successful
//                            System.out.println("Location of reserved place" );
//                        }
//                        step = 4;
//                    }
//                    else{
//                        block();
//                    }
//                    break;
                }
            }
        }

        public boolean done() { // if we return true this behaviour will end its cycle
            return step == 2;
        }
    }
}

