import java.util.HashMap;
import java.util.Map.Entry;

public class Link {
	private static int currentMaxID=-1;
	
	private int ID;
	private String name;
	private Node fromNode;
	private Node toNode;
	private int capacityBW;
	private int availableBW;
	private int length; //in meters
	private double propagationDelay;
	private HashMap<Integer, Integer> sfcRequestsServed; //holds sfcRequest ID and outflow into this link
	
	public Link(Node from, Node to, int capBW, int lenght) {
		this.ID = ++Link.currentMaxID;
		this.name = from.getName()+"->"+to.getName();
		this.fromNode = from;
		this.toNode = to;
		this.capacityBW = capBW;
		this.availableBW = this.capacityBW;
		this.length = lenght;
		this.propagationDelay = this.length/(2*10^8);//in sec
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	public Link(Node from, Node to, int capBW) {
		this.ID = ++Link.currentMaxID;
		this.name = from.getName()+"->"+to.getName();
		this.fromNode = from;
		this.toNode = to;
		this.capacityBW = capBW;
		this.availableBW = this.capacityBW;
		this.length = Utils.getLinkLength();
		this.propagationDelay = this.length/(2*10^8);//in sec
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	public Link(Node from, Node to) {
		this.ID = ++Link.currentMaxID;
		this.name = from.getName()+"->"+to.getName();
		this.fromNode = from;
		this.toNode = to;
		this.capacityBW = Utils.getLinkBW();
		this.availableBW = this.capacityBW;
		this.length = Utils.getLinkLength();
		this.propagationDelay = this.length/(2*10^8);//in sec
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	//Creates a new link from link L but with reversed direction
	public Link(Link L) {
		this.ID = ++Link.currentMaxID;
		this.name = L.getToNode().getName()+" -> "+L.getFromNode().getName();
		this.fromNode = L.getToNode();
		this.toNode = L.getFromNode();
		this.capacityBW = L.getCapacityBW();
		this.availableBW = this.capacityBW;
		this.length = L.getLength();
		this.propagationDelay = L.getDelay();//in sec
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	//Creates a new link from link L, same direction and same ID
	public Link(Link L,boolean cc) {
		this.ID = L.getID();
		this.name = L.getName();
		this.fromNode = L.getFromNode();
		this.toNode = L.getToNode();
		this.capacityBW = L.getCapacityBW();
		this.availableBW = L.getAvailableBW();
		this.length = L.getLength();
		this.propagationDelay = L.getDelay();//in sec
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	public void addSFC(int sfcID, VNF vnf) {
		this.availableBW-=((DeployedVNF)vnf).getOutFlow();
		this.sfcRequestsServed.put(sfcID, ((DeployedVNF)vnf).getOutFlow());
	}
	
	public boolean removeSFC(int sfcID) {
		
		Integer outFlow=this.sfcRequestsServed.get(sfcID);
		if(outFlow !=null) {
			this.sfcRequestsServed.remove(sfcID);
			this.availableBW+=outFlow;
			return true;
		}
		
		System.err.println("couldn't remove SFC_"+sfcID+" becasue it is not currently served by this link_"+this.ID+": "+this.name);
		return false;
		
	}
	
	@Override
	public String toString() {
		return ID+" | "+name+" | From: "+fromNode.getID()+" -->> "+toNode.getID()+" | cBW: "+capacityBW+
				" | avBW: "+availableBW+" | Length(m): "+length+" | delay(sec): "+propagationDelay+"\n"+displayServedSFCs();
	}
	
	public String displayServedSFCs() {
		StringBuilder s = new StringBuilder();
		int sfcID=0, outFlow=0;
		s.append("\n");
		for(Entry<Integer, Integer> sfc : sfcRequestsServed.entrySet()) {
		    sfcID = sfc.getKey();
		    outFlow = sfc.getValue();

		    s.append("- Serving SFC_"+sfcID+" with outFlow= "+outFlow+"\n");
		}
		return s.toString();
	}
	
	public void resetLink() {
		this.availableBW = this.capacityBW;
		this.sfcRequestsServed = null;
		this.sfcRequestsServed = new HashMap<Integer, Integer>();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Node getFromNode() {
		return fromNode;
	}

	public void setFromNode(Node fromNode) {
		this.fromNode = fromNode;
	}

	public Node getToNode() {
		return toNode;
	}

	public void setToNode(Node toNode) {
		this.toNode = toNode;
	}

	public int getCapacityBW() {
		return capacityBW;
	}

	public void setCapacityBW(int capacityBW) {
		this.capacityBW = capacityBW;
	}

	public int getAvailableBW() {
		return availableBW;
	}

	public void setAvailableBW(int availableBW) {
		this.availableBW = availableBW;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
		this.propagationDelay = this.length/(2*10^8);
	}

	public double getDelay() {
		return propagationDelay;
	}

	public void setDelay(double delay) {
		this.propagationDelay = delay;
	}
	
	

}
