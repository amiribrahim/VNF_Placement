
public class VNF {
	
	protected String name;
	protected int CPU; //CPU cores required for this VNF to operate 
	protected int RAM; //Memory in MB required for this VNF to operate 
	
	protected int maxInFlow; //maximum flow this VNF can take and generate given the allotted resources
	protected boolean shareable;
	protected boolean dropsCompresses;
	//protected int actualInFlow; //actual flow this VNF is getting and generating
	
	//private boolean usingOtherVNF; //true if this VNF isn't deployed, rather it shares 
	//private int amountShared; //this VNF share of the shared/utilized VNF (in terms of actual flow amount shared)
	
	

	public VNF() {}
	
	public VNF(String name, int cpu) {
		//this.usingOtherVNF = true;
		this.name = name;
		this.CPU = cpu;
		this.RAM = Utils.getVNFRAMControlled(this.CPU);
		this.maxInFlow = Utils.getVNFMaxInFlowFixed(this.CPU,this.RAM);
		this.shareable = true;
		this.dropsCompresses = false;
		//this.actualInFlow = actualInFlow;
		//this.amountShared = amountShared;
	}
	
	
	
	public VNF(String name, int cpu, boolean shareable, boolean dc) {
		//this.usingOtherVNF = true;
		this.name = name;
		this.CPU = cpu;
		this.RAM = Utils.getVNFRAMControlled(this.CPU);
		this.maxInFlow = Utils.getVNFMaxInFlowFixed(this.CPU,this.RAM);;
		this.shareable = shareable;
		this.dropsCompresses = dc;
		//this.actualInFlow = actualInFlow;
		//this.amountShared = amountShared;
	}
	
	public VNF(VNF vnf,boolean s) {
		//this.usingOtherVNF = true;
		this.name = vnf.name;
		this.CPU = vnf.CPU;
		this.RAM = vnf.RAM;
		this.maxInFlow = vnf.maxInFlow;
		this.shareable = s;
		this.dropsCompresses = vnf.dropsCompresses;
		//this.actualInFlow = actualInFlow;
		//this.amountShared = amountShared;
	}
	
	@Override
	public String toString() {
		return "VNF: "+this.name+" | Required-CPU: "+this.CPU+" | Required-RAM: "+this.RAM+" | "+" | Max-Flow: "+this.maxInFlow+" | shareable: "+this.shareable+
				" | Drops-Copresses: "+this.dropsCompresses;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCPU() {
		return CPU;
	}

	public void setCPU(int cPU) {
		CPU = cPU;
	}

	public int getRAM() {
		return RAM;
	}

	public void setRAM(int rAM) {
		RAM = rAM;
	}

	public int getMaxInFlow() {
		return maxInFlow;
	}

	public void setMaxInFlow(int maxInFlow) {
		this.maxInFlow = maxInFlow;
	}

	public int isShareable() {
		if(shareable)
			return 1;
		return 0;
	}

	public void setShareable(boolean shareable) {
		this.shareable = shareable;
	}

	public boolean isDropsCompresses() {
		return dropsCompresses;
	}

	public void setDropsCompresses(boolean dropsCompresses) {
		this.dropsCompresses = dropsCompresses;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
