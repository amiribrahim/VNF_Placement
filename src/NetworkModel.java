import java.util.HashMap;

public class NetworkModel {

	private Node[] nodes;
	private Link[] links;
	private int[][] conMatrix;
	//int[][] capacityBW;
	//int[][] availableBW;
	private Link[][] linksMatrix;
	private String[] nodesNames;
	private double avgDelay;
	private double utilization;
	private int cpuCapacity;
	private int cpuAvailable;
	private int ramCapacity;
	private int ramAvailable;
	private int bwCapacity;
	private int bwAvailable;
	private HashMap<SFCRequest, int[][]> nodesHostingSFCs;
	
	
	public NetworkModel(int numberOfNodes, int numberOfLinks, int[][] connectivity,String[] nodesNames) {
		nodes = new Node[numberOfNodes];
		links = new Link[numberOfLinks*2];
		linksMatrix = new Link[numberOfNodes][numberOfNodes];//needs improvement 
		nodesHostingSFCs = new HashMap<SFCRequest, int[][]>();
		//capacityBW = new int[numberOfNodes][numberOfNodes];
		//availableBW = new int[numberOfNodes][numberOfNodes];
		this.nodesNames = nodesNames;
		conMatrix = connectivity;
		avgDelay = 0.0;
		for (int i = 0; i < numberOfNodes; i++) { 
			nodes[i] = new Node(this.nodesNames[i]);
			this.cpuCapacity += nodes[i].getCapacityCPU();
			this.ramCapacity += nodes[i].getCapacityRAM();
		}
		
		this.cpuAvailable = this.cpuCapacity;
		this.ramAvailable = this.ramCapacity;
		
		int linkNum=0;
		Link L;
		for (int i = 0; i < connectivity.length; i++) { //loops over rows/from node
			for (int j = i+1; j < connectivity[i].length; j++) {
				if(conMatrix[i][j] == 1) {
					links[linkNum] = new Link(nodes[i], nodes[j]);
					L = links[linkNum];
					avgDelay+=links[linkNum].getDelay();
					linksMatrix[i][j] = links[linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					links[++linkNum] = new Link(L);
					linksMatrix[j][i]=links[linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					//capacityBW[i][j]=linksMatrix[i][j].getCapacityBW();
					//capacityBW[j][i]=capacityBW[i][j];
					//availableBW[i][j]=linksMatrix[i][j].getAvailableBW();
					//availableBW[j][i]=availableBW[i][j];
					linkNum++;
				}else {
					linksMatrix[i][j] = null;
					linksMatrix[j][i] = null;
					//capacityBW[i][j]=0;
					//capacityBW[j][i]=0;
					//availableBW[i][j]=0;
					//availableBW[j][i]=0;
				}
			}
		}
		this.bwAvailable = this.bwCapacity;
		avgDelay = avgDelay/numberOfLinks;
		utilization = 0.0;
	}
	
	public NetworkModel(int numberOfNodes, int numberOfLinks, int[][] connectivity) {
		nodes = new Node[numberOfNodes];
		links = new Link[numberOfLinks*2];
		linksMatrix = new Link[numberOfNodes][numberOfNodes];//needs improvement
		nodesHostingSFCs = new HashMap<SFCRequest, int[][]>();
		//capacityBW = new int[numberOfNodes][numberOfNodes];
		//availableBW = new int[numberOfNodes][numberOfNodes];
		nodesNames = new String[numberOfNodes];
		conMatrix = connectivity;
		avgDelay = 0.0;
		for (int i = 0; i < numberOfNodes; i++) {
			nodes[i] = new Node();
			nodesNames[i]=nodes[i].getName();
			this.cpuCapacity += nodes[i].getCapacityCPU();
			this.ramCapacity += nodes[i].getCapacityRAM();
		}
		
		this.cpuAvailable = this.cpuCapacity;
		this.ramAvailable = this.ramCapacity;	 
		
		int linkNum=0;
		Link L;
		for (int i = 0; i < connectivity.length; i++) { //loops over rows/from node
			for (int j = i+1; j < connectivity[i].length; j++) {
				if(conMatrix[i][j] == 1) {
					links[linkNum] = new Link(nodes[i], nodes[j]);
					L = links[linkNum];
					avgDelay+=links[linkNum].getDelay();
					linksMatrix[i][j] = links[linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					links[++linkNum] = new Link(L);
					linksMatrix[j][i]=links[linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					linkNum++;
				}else {
					linksMatrix[i][j] = null;
					linksMatrix[j][i] = null;
					//capacityBW[i][j]=0;
					//capacityBW[j][i]=0;
					//availableBW[i][j]=0;
					//availableBW[j][i]=0;
				}
				
			}
		}
		this.bwAvailable = this.bwCapacity;
		avgDelay = avgDelay/numberOfLinks;
		utilization = 0.0;
	}
	
	public NetworkModel(NetworkModel model) {
		nodes = new Node[model.getNodes().length];
		links = new Link[model.getLinks().length];
		linksMatrix = new Link[nodes.length][nodes.length];//needs improvement
		nodesHostingSFCs = new HashMap<SFCRequest, int[][]>();
		nodesNames = new String[nodes.length];
		conMatrix = model.conMatrix;
		avgDelay = 0.0;
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new Node(model.getNodes()[i]);
			nodesNames[i]=nodes[i].getName();
			this.cpuCapacity += nodes[i].getCapacityCPU();
			this.ramCapacity += nodes[i].getCapacityRAM();
		}
		
		this.cpuAvailable = this.cpuCapacity;
		this.ramAvailable = this.ramCapacity;	 
		
		for (int i = 0; i < model.getLinks().length; i++) {
			this.links[i] = new Link(model.getLinks()[i], true);
		}
		
		int linkNum=0;
		//Link L;
		for (int i = 0; i < conMatrix.length; i++) { //loops over rows/from node
			for (int j = i+1; j < conMatrix[i].length; j++) {
				if(conMatrix[i][j] == 1) {
					avgDelay+=links[linkNum].getDelay();
					linksMatrix[i][j] = links[linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					linksMatrix[j][i]=links[++linkNum];
					this.bwCapacity+=links[linkNum].getCapacityBW();
					linkNum++;
				}else {
					linksMatrix[i][j] = null;
					linksMatrix[j][i] = null;
				}
				
			}
		}
		this.bwAvailable = this.bwCapacity;
		avgDelay = avgDelay/links.length;
		utilization = 0.0;
	}
	
	public boolean placeSFCRequest(SFCRequest sfc, int[] solXR, char[] XorR) {
		boolean placed, shared;
		int[][] sol =new int[2][sfc.length()];
		for(int i=0 ;  i < sfc.length() ; i++) {
			sol[0][i]=solXR[i];
			if(XorR[i] == 'X') {//Deploy VNFi
				placed=this.placeVNF(nodes[solXR[i]], (DeployedVNF)sfc.getVnfList().get(i), sfc.getID());
				sol[1][i]=1;
				if(placed) {
					this.cpuAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getCPU();
					this.ramAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getRAM();
					this.bwAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getOutFlow();
				}else {
					System.err.println("VNF_"+sfc.getVnfList().get(i).getName()+" of SFC_"+sfc.getID()+" could not be placed on node_"+nodes[solXR[i]].getName());
					return false;
				}
			}
			else if (XorR[i] == 'R') {//share already deployed VNF
				shared=this.shareVNF(nodes[solXR[i]], (DeployedVNF)sfc.getVnfList().get(i), sfc.getID());
				sol[1][i]=2;
				if(shared) {
					this.bwAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getOutFlow();
				}else {
					System.err.println("VNF_"+sfc.getVnfList().get(i).getName()+" of SFC_"+sfc.getID()+" could not be shared on node_"+nodes[solXR[i]].getName());
					return false;
				}
			}
			else {
				System.err.println("Neither Xjin[i][n] Nor Rjin[i][n] are 1, wrong solution :((( ");
				return false;
			}
		}
		
		Link l=null;
		int index;
		for(int i=0 ;  i < sfc.length()-1 ; i++) {
			
			index = getLinkIndex(nodes[solXR[i]], nodes[solXR[i+1]]);
			
			if(index!=-1) {
				l = links[index];
				l.addSFC(sfc.getID(),((DeployedVNF)sfc.getVnfList().get(i)));
			}
			else {
				System.err.println("link between nodes"+nodes[solXR[i]]+" and "+nodes[solXR[i+1]]+" doesn't exist");
				return false;
			}
		}
		this.nodesHostingSFCs.put(sfc, sol);
		return true;
	}
	
	public boolean placeSFCRequest(SFCRequest sfc, int[][] sol) {
		boolean placed, shared;
		for(int i=0 ;  i < sfc.length() ; i++) {
			if(sol[1][i] == 1) {//Deploy VNFi
				placed=this.placeVNF(nodes[sol[0][i]], (DeployedVNF)sfc.getVnfList().get(i), sfc.getID());
				if(placed) {
					this.cpuAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getCPU();
					this.ramAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getRAM();
					this.bwAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getOutFlow();
				}else {
					System.err.println("VNF_"+sfc.getVnfList().get(i).getName()+" of SFC_"+sfc.getID()+" could not be placed on node_"+nodes[sol[0][i]].getName());
					return false;
				}
			}
			else if (sol[1][i] == 2) {//share already deployed VNF
				shared=this.shareVNF(nodes[sol[0][i]], (DeployedVNF)sfc.getVnfList().get(i), sfc.getID());
				if(shared) {
					this.bwAvailable -= ((DeployedVNF)sfc.getVnfList().get(i)).getOutFlow();
				}else {
					System.err.println("VNF_"+sfc.getVnfList().get(i).getName()+" of SFC_"+sfc.getID()+" could not be shared on node_"+nodes[sol[0][i]].getName());
					return false;
				}
			}
			else {
				System.err.println("Neither Xjin[i][n] Nor Rjin[i][n] are 1, wrong solution :((( ");
				return false;
			}
		}
		
		Link l=null;
		int index;
		for(int i=0 ;  i < sfc.length()-1 ; i++) {
			
			index = getLinkIndex(nodes[sol[0][i]], nodes[sol[0][i+1]]);
			
			if(index!=-1) {
				l = links[index];
				l.addSFC(sfc.getID(),((DeployedVNF)sfc.getVnfList().get(i)));
			}
			else {
				System.err.println("link between nodes"+nodes[sol[0][i]]+" and "+nodes[sol[0][i+1]]+" doesn't exist");
				return false;
			}
		}
		this.nodesHostingSFCs.put(sfc, sol);
		return true;
	}
	
	public boolean placeVNF(Node node, DeployedVNF vnf, int sfcRequestID) {
		return node.placeVNF(vnf,sfcRequestID);
	}
	
	public boolean shareVNF(Node node, DeployedVNF vnf, int sfcRequestID) {
		return node.shareVNF(vnf, sfcRequestID);
	}
	
	public boolean removeVNF(Node node, DeployedVNF vnf) {
		return node.removeVNF(vnf);
	}
	
	public boolean stopShareVNF(Node node, DeployedVNF vnf, int sfcRequestID) {
		return node.stopShareVNF(vnf, sfcRequestID);
	}
	
	//returns index of node is it exists, -1 otherwise
	public int nodeExists(Node n) {
		for (int i = 0 ; i < nodes.length ; i++)
			if(nodes[i].getName().compareToIgnoreCase(n.getName())==0 && nodes[i].getID()==n.getID())
				return i;
		return -1;
	}
	
	//if a link exists it returns the link, null otherwise.
	public Link isLinked(Node n, Node v) {
		int nIndex = nodeExists(n);
		int vIndex = nodeExists(v);
		if(nIndex != -1 && vIndex != -1 && nIndex !=vIndex) {
			Link L = this.linksMatrix[nIndex][vIndex];
			if(L != null)
				return L;
		}
		return null;		
	}
	
	public int haveCommonNode(Link n, Link v) {
		if(n.getToNode().getID() == v.getFromNode().getID())
			return 1;
		return 0;
	}
	
	public int getLinkIndex(Node from, Node to) {
		for (int i = 0 ; i < links.length ; i++) {
			if(links[i].getFromNode().getID()==from.getID() && links[i].getToNode().getID() == to.getID())
				return i;
		}
		return -1;
	}
	
//	public void displayLinks() {
//		for (Link  L : this.links) {
//			System.out.print(L.toString());
//		}
//	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Network Model of "+nodes.length+" node(s)"+" and "+links.length+" link(s) \n");
		s.append("CPU capacity is: "+this.cpuCapacity+" cores \n");
		s.append("RAM capacity is: "+this.ramCapacity+" GBs \n");
		s.append("BW capacity is: "+this.bwCapacity+" Mbps \n");
//		for (int i = 0; i < conMatrix.length; i++) {
//			s.append(nodes[i].toString());
//			for (int j = i+1; j < conMatrix[i].length; j++) {
//				if (linksMatrix[i][j]!=null) {
//					s.append("Link from: "+linksMatrix[i][j].getFromNode().getName()+"-to->"+linksMatrix[i][j].getToNode().getName()+"\n");
//					//s.append("Link from: "+linksMatrix[j][i].getToNode().getName()+"-to->"+linksMatrix[j][i].getFromNode().getName()+"\n");
//				}
//					
//			}
//		}
//		s.append("============================================================================================================================\n");
		for (int i = 0; i < conMatrix.length; i++) {
			s.append(nodes[i].toString());
			for (int j = 0; j < conMatrix[i].length; j++) {
				if (linksMatrix[i][j]!=null) {
					s.append("Link from: "+linksMatrix[i][j].getFromNode().getName()+"-to->"+linksMatrix[i][j].getToNode().getName()+"\n");
					//s.append("Link from: "+linksMatrix[j][i].getToNode().getName()+"-to->"+linksMatrix[j][i].getFromNode().getName()+"\n");
				}
					
			}
		}
		return s.toString();
	}
	
	public String displayNode(int nodeIndex) {
		return nodes[nodeIndex].toString();
	}
	
	public String displayNodes() {
		StringBuilder s = new StringBuilder();
		double cpuutil=((double)(this.cpuCapacity-this.cpuAvailable)/this.cpuCapacity)*100;
		double ramutil=((double)(this.ramCapacity-this.ramAvailable)/this.ramCapacity)*100;
		double bwutil=((double)(this.bwCapacity-this.bwAvailable)/this.bwCapacity)*100;
		s.append("Netowkr Model of capacities: \n");
		s.append("Resource | Capacity | Vaialable | Utilization \n");
		s.append("CPU:     | "+this.cpuCapacity+"      | "+this.cpuAvailable+"      | "+cpuutil+"\n");
		s.append("RAM:     | "+this.ramCapacity+"      | "+this.ramAvailable+"     | "+ramutil+"\n");
		s.append("BW:      | "+this.bwCapacity+"     | "+this.bwAvailable+"    | "+bwutil+"\n");
		for(Node n : nodes)
			s.append(n.toString());
		
		return s.toString();
	}
	
	public String displayLinks() {
		StringBuilder s = new StringBuilder();
		for(Link l : links)
			s.append(l.toString());
		
		return s.toString();
	}
	
	public void resetModel() {
		nodesHostingSFCs = null;
		nodesHostingSFCs = new HashMap<SFCRequest, int[][]>();
		
		this.cpuAvailable = this.cpuCapacity;
		this.ramAvailable = this.ramCapacity;
		
		this.bwAvailable = this.bwCapacity;
		
		utilization = 0.0;
		
		for (int i = 0; i < nodes.length; i++)
			this.nodes[i].resetNode();
		
		for (int i = 0; i < links.length; i++)
			this.links[i].resetLink();
	}
	
	public Node[] getNodes() {
		return nodes;
	}

	public void setNodes(Node[] nodes) {
		this.nodes = nodes;
	}

	public Link[] getLinks() {
		return links;
	}

	public void setLinks(Link[] links) {
		this.links = links;
	}

	public int[][] getConMatrix() {
		return conMatrix;
	}

	public void setConMatrix(int[][] conMatrix) {
		this.conMatrix = conMatrix;
	}

	public Link[][] getLinksMatrix() {
		return linksMatrix;
	}

	public void setLinksMatrix(Link[][] linksMatrix) {
		this.linksMatrix = linksMatrix;
	}

	public String[] getNodesNames() {
		return nodesNames;
	}

	public void setNodesNames(String[] nodesNames) {
		this.nodesNames = nodesNames;
	}

	
	public double getAvgDelay() {
		return avgDelay;
	}

	public void setAvgDelay(double avgDelay) {
		this.avgDelay = avgDelay;
	}

	
	
	public double getUtilization() {
		return utilization;
	}

	public void setUtilization(double utilization) {
		this.utilization = utilization;
	}

	public int getCpuCapacity() {
		return cpuCapacity;
	}

	public void setCpuCapacity(int cpuCapacity) {
		this.cpuCapacity = cpuCapacity;
	}

	public int getRamCapacity() {
		return ramCapacity;
	}

	public void setRamCapacity(int ramCapacity) {
		this.ramCapacity = ramCapacity;
	}

	public int getBwCapacity() {
		return bwCapacity;
	}

	public void setBwCapacity(int bwCapacity) {
		this.bwCapacity = bwCapacity;
	}

	
	
	public int getCpuAvailable() {
		return cpuAvailable;
	}

	public void setCpuAvailable(int cpuAvailable) {
		this.cpuAvailable = cpuAvailable;
	}

	public int getRamAvailable() {
		return ramAvailable;
	}

	public void setRamAvailable(int ramAvailable) {
		this.ramAvailable = ramAvailable;
	}

	public int getBwAvailable() {
		return bwAvailable;
	}

	public void setBwAvailable(int bwAvailable) {
		this.bwAvailable = bwAvailable;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int noNodes = 10;
		int noLinks = 15;
		int noNodes2 = 5;
		int noLinks2 = 4;
		int[][] con = new int[noNodes][noNodes];
		int index = 0;
		int i,j=0;
		while(index < noLinks) {
			i = Utils.getRandInt(noNodes);
			j = Utils.getRandInt(noNodes-i)+i;
			if(con[i][j]==0 && i!=j) {
				con[i][j]=1;
				con[j][i]=1;
				index++;
			}
		}
		
		int[][] con2 = {{0,1,1,0,0},
			       {1,0,0,1,0},
			       {1,0,0,0,1},
			       {0,1,0,0,0},
			       {0,0,1,0,0}};
		
		for (int j2 = 0; j2 < con2.length; j2++) {
			for (int k = 0; k < con2[j2].length; k++) {
				System.out.print(con2[j2][k]+" ");
			}
			System.out.println();
		}
		
		NetworkModel model = new NetworkModel(noNodes2, noLinks2, con2);			
		System.out.println(model.toString());
		System.out.println(model.displayLinks());
		
		NetworkModel model2 = new NetworkModel(model);
		for (int j2 = 0; j2 < model2.getConMatrix().length; j2++) {
			for (int k = 0; k < model2.getConMatrix()[j2].length; k++) {
				System.out.print(model2.getConMatrix()[j2][k]+" ");
			}
			System.out.println();
		}
		model.getNodes()[0].setCapacityCPU(14);
		model.getLinks()[0].setCapacityBW(14);
		System.out.println(model2.toString());
		System.out.println(model2.displayLinks());
	}

	
	
}
