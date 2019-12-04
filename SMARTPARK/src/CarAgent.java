import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

/* car agent class */
public class CarAgent extends Agent {

    // sekcja mockowych  agentów xd
    private AID[] parkingAgents = {new AID("parking1", AID.ISLOCALNAME),    //list of parking agent list
            new AID("parking2", AID.ISLOCALNAME)};
    // sekcja mockowych  agentów xd

    @Override
    protected void setup() {  //call when create the agent
        System.out.println("hello " + getAID().getName() + " is ready");

        addBehaviour(new ExampleBehaviour());  // example behaviour implement protocol(inf- ref protocol
    }

    @Override
    protected void takeDown() { //method call when agent is killed ps. jakie standardy komentarzy w javie? nk poprawi
        System.out.println("car is killed / outside the city");

    }
    /* zachowanie definiujace protokol mappera | inner class*/
    private class ExampleBehaviour extends Behaviour {

        private int step = 0;

        public void action() { //action in protocol |obligatory
            switch (step) {
                case 0:
                    ACLMessage m_ir = new ACLMessage(ACLMessage.INFORM_REF); // call parking agent for coordinates
                    for (int i = 0; i < parkingAgents.length; ++i) // al parking agents loop
                        m_ir.addReceiver(parkingAgents[i]); //ad every agent as reciever
                    step++;
                    break;
                case 1:
                    // czy to jest potrzebne / co tu bedzie
                    step++;
                    break;
            }
        }

        public boolean done() { // call when done behaviour | obligatory
            return true;
        }
    }
}