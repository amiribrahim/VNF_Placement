
public class VirtLink {
	private static int maxID=-1;
	
	private int ID;
	private String name;
	private VNF from;
	private VNF to;
	private int flow;
	
	public VirtLink(VNF from, VNF to) {
		this.ID = ++VirtLink.maxID;
		this.name = from.getName()+" -> "+to.getName();
		this.from = from;
		this.to = to;
		this.flow = ((DeployedVNF)from).getOutFlow();
	}
	
	@Override
	public String toString() {
		return this.name+" with flow/BW: "+this.flow+" Mbps";
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

	public VNF getFrom() {
		return from;
	}

	public void setFrom(VNF from) {
		this.from = from;
	}

	public VNF getTo() {
		return to;
	}

	public void setTo(VNF to) {
		this.to = to;
	}

	public int getFlow() {
		return flow;
	}

	public void setFlow(int flow) {
		this.flow = flow;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
