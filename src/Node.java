import java.util.ArrayList;

public class Node {
	private static int currentMaxID=-1;
	
	private int ID;
	private String name;
	private int capacityCPU;
	private int availableCPU;
	private int capacityRAM;
	private int availableRAM;
	private float processingDelay;
	private float virtualizationDelay; //Delay inclined by hypervisor
	//it should be per interface
	private float queuingDelay; //should be a function of the utilization of links connected to this node
	private int inFlow;
	private int outFlow;
	private ArrayList<DeployedVNF> hostedVNFs;  
	
	public Node(String name) {
		this.ID = ++Node.currentMaxID;
		this.name = name;
		this.capacityCPU = Utils.getNodeCores();
		this.availableCPU = this.capacityCPU;
		//this.capacityRAM = Utils.getNodeRAM();
		this.capacityRAM = Utils.getNodeRAMControlled(this.capacityCPU);
		this.availableRAM = this.capacityRAM;
		this.processingDelay = 0;
		this.virtualizationDelay = 0;
		this.queuingDelay = 0;
		this.inFlow = 0;
		this.outFlow = 0;
		hostedVNFs = new ArrayList<DeployedVNF>();
	}
	
	public Node() {
		this.ID = ++Node.currentMaxID;
		this.name = "Node"+ID;
		this.capacityCPU = Utils.getNodeCores();
		this.availableCPU = this.capacityCPU;
		//this.capacityRAM = Utils.getNodeRAM();
		this.capacityRAM = Utils.getNodeRAMControlled(this.capacityCPU);
		this.availableRAM = this.capacityRAM;
		this.processingDelay = 0;
		this.virtualizationDelay = 0;
		this.queuingDelay = 0;
		this.inFlow = 0;
		this.outFlow = 0;
		hostedVNFs = new ArrayList<DeployedVNF>();
	}
	
	public Node(Node v) {
		this.ID = v.getID();
		this.name = v.getName();
		this.capacityCPU = v.getCapacityCPU();
		this.availableCPU = v.getAvailableCPU();
		//this.capacityRAM = Utils.getNodeRAM();
		this.capacityRAM = v.getCapacityRAM();
		this.availableRAM = v.getAvailableRAM();
		this.processingDelay = v.getProcDelay();
		this.virtualizationDelay = v.getVirtDelay();
		this.queuingDelay = v.getQueuingDelay();
		this.inFlow = v.getInFlow();
		this.outFlow = v.getOutFlow();
		hostedVNFs = new ArrayList<DeployedVNF>();
	}
	
	public boolean placeVNF(DeployedVNF vnf, int sfcRequestID) {
		if(vnf.getCPU() > this.availableCPU || vnf.getRAM() > this.availableRAM) {		
			System.err.println("CPU and/or RAM requirements of VNF_"+vnf.getID()+" of SFC_"+sfcRequestID+" exceed Node_"+this.getID()+":"+this.getName()+" resources");
			return false;
		}
		hostedVNFs.add(vnf);
		int[] flow = {vnf.getInFlow(),vnf.getOutFlow()};
		vnf.getSfcRequestsServed().put(sfcRequestID, flow);
		availableCPU -= vnf.getCPU();
		availableRAM -= vnf.getRAM();
		inFlow += vnf.getInFlow();
		outFlow += vnf.getOutFlow();
		//delay should be incremented in a certain way
		virtualizationDelay =(float)(0.338 * hostedVNFs.size() * (inFlow^12) + 0.51 * inFlow);
		if(availableCPU==0)
			processingDelay = 1;//needs scaling to be within milli or nano sec scale
		else
			processingDelay = 1/availableCPU;//needs scaling to be within milli or nano sec scale
		return true;
	}
	
	public boolean shareVNF(DeployedVNF vnf, int sfcRequestID) {
		DeployedVNF dVNF = isVNFDeployed(vnf);
		if(dVNF == null) {
			System.err.println("VNF:"+vnf.getName()+" is not even deployed on this node: "+this.getName());
			return false;
		}
		if(dVNF.getAvailableFlow() >= vnf.getInFlow()) {
			dVNF.addSFCRequest(sfcRequestID, vnf.getInFlow(), vnf.getOutFlow());
			inFlow += vnf.getInFlow();
			outFlow += vnf.getOutFlow();
			//delay should be incremented in a certain way
			virtualizationDelay =(float)(0.338 * hostedVNFs.size() * (inFlow^12) + 0.51 * inFlow);
			//processingDelay = 1/availableCPU;//needs scaling to be within milli or nano sec scale
			return true;
		}
		return false;
	}
	
	//should be revisited, in case
	public boolean removeVNF(DeployedVNF vnf) {
		if(!this.hostedVNFs.remove(vnf)) {
			System.err.println("Asking to remove VNF:"+vnf.getName()+" that is not currently deployed on node_"+this.ID+" "+this.getName());
			return false;
		}
		
		if(!vnf.getSfcRequestsServed().isEmpty()) {
			System.err.println("Asking to remove VNF:"+vnf.getName()+", which is currently serving some SFCs");
			return false;
		}
		
		this.availableCPU += vnf.getCPU();
		this.availableRAM += vnf.getRAM();
		this.inFlow -= vnf.getInFlow();
		this.outFlow -= vnf.getOutFlow();
		//delay should be decremented in a certain way
		virtualizationDelay =(float)(0.338 * hostedVNFs.size() * (inFlow^12) + 0.51 * inFlow);
		processingDelay = 1/availableCPU;//needs scaling to be within milli or nano sec scale
		return true;
	}
	
	public boolean stopShareVNF(DeployedVNF vnf, int sfcRequestID) {
		DeployedVNF dVNF = isVNFDeployed(vnf.getName());
		if(dVNF == null) {
			System.err.println("VNF:"+vnf.getName()+" is not even deployed on htis node: "+this.getName());
			return false;
		}
		dVNF.removeSFCRequest(sfcRequestID);
		inFlow -= vnf.getInFlow();
		outFlow -= vnf.getOutFlow();
		//delay should be incremented in a certain way
		virtualizationDelay =(float)(0.338 * hostedVNFs.size() * (inFlow^12) + 0.51 * inFlow);
		//processingDelay = 1/availableCPU;//needs scaling to be within milli or nano sec scale
		
		return false;
	}
	
	public DeployedVNF isVNFDeployed(String vnfName) {
		for (DeployedVNF deployedVNF : hostedVNFs) {
			if(deployedVNF.getName().compareToIgnoreCase(vnfName)==0)
				return deployedVNF;
		}
		return null;
	}
	//modefied to check effect
	public DeployedVNF isVNFDeployed(VNF vnf) {
		for (DeployedVNF deployedVNF : hostedVNFs) {
			if(deployedVNF.getName().compareToIgnoreCase(vnf.getName())==0)
				if(deployedVNF.getAvailableFlow() >= ((DeployedVNF)vnf).getInFlow() && deployedVNF.isShareable()==1)
					return deployedVNF;
		}
		return null;
	}
	
	public DeployedVNF isVNFDeployedandShareable(String vnfName) {
		for (DeployedVNF deployedVNF : hostedVNFs) {
			if(deployedVNF.getName().compareToIgnoreCase(vnfName)==0 && deployedVNF.isShareable()==1)
				return deployedVNF;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return name+" | "+ID+" | cCPU: "+capacityCPU+" | avCPU: "+availableCPU+" | cRAM: "+capacityRAM+" | avRAM: "+availableRAM+" | InFlow: "+inFlow+
		" | OutFlow: "+outFlow+" | procDelay"+processingDelay+"| virtDelay: "+virtualizationDelay+"\n"+printHostedVNFs()+"\n";
	}
	
	public String printHostedVNFs() {
		StringBuilder s= new StringBuilder(); 
		for (int i=0;i<this.hostedVNFs.size();i++)
			s.append(hostedVNFs.get(i).toString());
		return s.toString();
	}
	
	public void resetNode() {
		hostedVNFs = null;
		hostedVNFs = new ArrayList<DeployedVNF>();
		
		this.availableCPU = this.capacityCPU;
		this.availableRAM = this.capacityRAM;
		
		this.processingDelay = 0;
		this.virtualizationDelay = 0;
		this.queuingDelay = 0;
		this.inFlow = 0;
		this.outFlow = 0;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public int getCapacityCPU() {
		return capacityCPU;
	}



	public void setCapacityCPU(int capacityCPU) {
		this.capacityCPU = capacityCPU;
	}



	public int getAvailableCPU() {
		return availableCPU;
	}



	public void setAvailableCPU(int availableCPU) {
		this.availableCPU = availableCPU;
	}



	public int getCapacityRAM() {
		return capacityRAM;
	}



	public void setCapacityRAM(int capacityRAM) {
		this.capacityRAM = capacityRAM;
	}



	public int getAvailableRAM() {
		return availableRAM;
	}



	public void setAvailableRAM(int availableRAM) {
		this.availableRAM = availableRAM;
	}



	public float getProcDelay() {
		return processingDelay;
	}

	public void setProcDelay(float delay) {
		this.processingDelay = delay;
	}

	public float getVirtDelay() {
		return virtualizationDelay;
	}

	public void setVirtDelay(float delay) {
		this.virtualizationDelay = delay;
	}

	public float getQueuingDelay() {
		return queuingDelay;
	}

	public void setQueuingDelay(float delay) {
		this.queuingDelay = delay;
	}
	
	public int getInFlow() {
		return inFlow;
	}



	public void setInFlow(int inFlow) {
		this.inFlow = inFlow;
	}



	public int getOutFlow() {
		return outFlow;
	}



	public void setOutFlow(int outFlow) {
		this.outFlow = outFlow;
	}



	public ArrayList<DeployedVNF> getHostedVNFs() {
		return hostedVNFs;
	}



	public void setHostedVNFs(ArrayList<DeployedVNF> hostedVNFs) {
		this.hostedVNFs = hostedVNFs;
	}



	public int getID() {
		return ID;
	}

	
}
