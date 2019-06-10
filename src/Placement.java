import java.awt.DisplayMode;
import java.util.ArrayList;


import gurobi.*;

public class Placement {
	//network model
	NetworkModel NM;
	ArrayList<VNF> availableVNFs;
	SFCRequest sfcj;
	
	
	double cpuUnitCost, ramUnitCost, bwUnitCost;
	
	//Decision variables
	GRBVar[][] Xjin;//VNF i of SFC j is placed on node n
	GRBVar[][] Rjin;//VNF i of SFC j is sharing already deployed i on node n
	GRBVar[] Pj;//Represents nodes ON SFC j path.  
	//GRBVar[][] virt_Phy_Links_map; //maps virtual links between SFC adjacent VNFs and physical links between nodes (could be one-to-one or one-to-many)
	
	int[] solXR;
	char[] XorR;
	boolean solved;
	
	GRBEnv env;	
	GRBModel optModel;// = new GRBModel(env);
	GRBLinExpr objExpr;
	
	public Placement(NetworkModel nm,SFCRequest sfcj) throws GRBException {
		this.NM = nm;
		this.sfcj = sfcj;
		availableVNFs = new ArrayList<VNF>();
		Xjin = new GRBVar[this.sfcj.getVnfList().size()][NM.getNodes().length];
		solXR = new int[this.sfcj.getVnfList().size()];
		Rjin = new GRBVar[this.sfcj.getVnfList().size()][NM.getNodes().length];
		XorR = new char[this.sfcj.getVnfList().size()];
		//Pj = new GRBVar[NM.getNodes().length];
		//virt_Phy_Links_map = new GRBVar[sfcj.length()-1][NM.getLinks().length];
		solved=false;
		env = new GRBEnv(true);
		env.set("logFile", "mip1.log");
		env.start();
		
		optModel = new GRBModel(env);
	}
	
	public void createObjective(double UcCPU, double UcRAM, double UcBW ) throws GRBException {
		double vnfCPUCost, vnfRAMCost, vnfBWCost;
		objExpr = new GRBLinExpr();
		//int linked;
		for(int i = 0; i < Xjin.length; i++) {//loops over VNFs in SFCj
			vnfCPUCost = sfcj.getVnfList().get(i).getCPU()*UcCPU;
			vnfRAMCost = sfcj.getVnfList().get(i).getRAM()*UcRAM;
			vnfBWCost =  ((DeployedVNF)sfcj.getVnfList().get(i)).getOutFlow()*UcBW;
			
			for (int n = 0; n < Xjin[i].length; n++) {//loops over nodes in Network Model
				Xjin[i][n] = optModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "Xj"+i+n);
				Rjin[i][n] = optModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "Rj"+i+n);
				//Pj[n] = optModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "Pj["+n+"]"); //useless
				
				objExpr.addTerm(vnfCPUCost+vnfRAMCost+vnfBWCost, Xjin[i][n]);
				objExpr.addTerm(vnfBWCost, Rjin[i][n]);
			}
			/*
			//we are calc twice the first phy link mapped to virt link 
			for(int l = 0 ; l < NM.getLinks().length && i < virt_Phy_Links_map.length ; l++) {
				virt_Phy_Links_map[i][l]= optModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "Lv"+i+1+","+i+2+"->"+NM.getLinks()[l].getFromNode().getName()+
						","+NM.getLinks()[l].getToNode().getName());
				//linked=0;
				//if(NM.isLinked(NM.getLinks()[l].getFromNode(), NM.getLinks()[l].getToNode())!=null)
				//	linked = 1;
				//objExpr.addTerm(vnfBWCost*linked, virt_Phy_Links_map[i][l]); ///still not convinced
				objExpr.addTerm(vnfBWCost, virt_Phy_Links_map[i][l]); ///still not convinced
			}*/
		}
		optModel.setObjective(objExpr,GRB.MINIMIZE);
	}
	
	//For each node CPU and RAM constraints
	public void capacityConstraints() throws GRBException{
				GRBLinExpr nodeCpuExpr, nodeRamExpr;
				for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
					nodeCpuExpr = new GRBLinExpr();
					nodeRamExpr = new GRBLinExpr();
					for(int i = 0 ; i < sfcj.length() ; i++) {
						nodeCpuExpr.addTerm(sfcj.getVnfList().get(i).getCPU(), Xjin[i][n]);
						nodeRamExpr.addTerm(sfcj.getVnfList().get(i).getRAM(), Xjin[i][n]);
					}
					optModel.addConstr(nodeCpuExpr, GRB.LESS_EQUAL, NM.getNodes()[n].getAvailableCPU(), "cpuContr"+n);
					optModel.addConstr(nodeRamExpr, GRB.LESS_EQUAL, NM.getNodes()[n].getAvailableRAM(), "ramContr"+n);
				}
	}
	
	//Xjin+Rjin=1 for all n E N 
	public void X_RConstraints1() throws GRBException{
		GRBLinExpr XoRExpr;
		for(int i = 0 ; i < sfcj.length() ; i++) {
			XoRExpr = new GRBLinExpr();
			for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
				XoRExpr.addTerm(1.0, Xjin[i][n]);
				XoRExpr.addTerm(1.0, Rjin[i][n]);
			}
			optModel.addConstr(XoRExpr, GRB.EQUAL, 1.0, "XoR"+i);
		}
	}
	
	//Xjin+Xin*Si*Rjin=1 for all n E N
	public void X_RConstraints2() throws GRBException{
		//For each Vi in SFCj either Xjin or Rjin
		GRBLinExpr rExpr;
		int vnfDeployedOnce =0;
		int shareable=0;
		DeployedVNF dVNF;
		for(int i = 0 ; i < sfcj.length() ; i++) {
			rExpr = new GRBLinExpr(); 
			for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
				vnfDeployedOnce=0;
				shareable=0;
				//dVNF = NM.getNodes()[n].isVNFDeployed(sfcj.getVnfList().get(i).getName());
				dVNF = NM.getNodes()[n].isVNFDeployed(sfcj.getVnfList().get(i));
				if(dVNF != null) {
					vnfDeployedOnce = 1;
					shareable=dVNF.isShareable();
				}
				rExpr.addTerm(1.0, Xjin[i][n]);
				rExpr.addTerm(vnfDeployedOnce*shareable, Rjin[i][n]);	
			}
			optModel.addConstr(rExpr, GRB.EQUAL, 1.0, "R"+i);
		}
	}
	
	//Fin(Vi)*Rjin <= Fav(Xin) : When sharing the available flow in the Deployed VNF should be greater than or equal to SFCj Vi
	public void X_RConstraints3() throws GRBException{
		//For each Vi in SFCj either Xjin or Rjin
		GRBLinExpr flowExprLhs,flowExprRhs;
		int availableFlow= 0;
		DeployedVNF dVNF;
		for(int i = 0 ; i < sfcj.length() ; i++) {
			flowExprLhs = new GRBLinExpr();
			flowExprRhs = new GRBLinExpr();
			for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
				//dVNF = NM.getNodes()[n].isVNFDeployed(sfcj.getVnfList().get(i).getName());
				dVNF = NM.getNodes()[n].isVNFDeployed(sfcj.getVnfList().get(i));
				availableFlow = 0;
				if(dVNF != null) {
					availableFlow = dVNF.getMaxInFlow()-dVNF.getInFlow(); //get the available/unused flow
					System.out.println("Vailable flow of deployed VNF_"+dVNF.getName()+" at node_"+n+" is:"+availableFlow);
					System.out.println("Required flow of VNF_"+((DeployedVNF)sfcj.getVnfList().get(i)).getName()+" = "+((DeployedVNF)sfcj.getVnfList().get(i)).getInFlow());
				}
				flowExprLhs.addTerm(((DeployedVNF)sfcj.getVnfList().get(i)).getInFlow(), Rjin[i][n]);
				flowExprRhs.addTerm(availableFlow, Rjin[i][n]);
			}
			optModel.addConstr(flowExprLhs, GRB.LESS_EQUAL, flowExprRhs, "FlowConstr"+i);
		}
	}
	
	
	//if Xjin+Rjin and Xj(i+1)v+Rj(i+1)v -> there hase to be a path between nodes n and v
	public void pathContinuityConstraints() throws GRBException{
		GRBQuadExpr XoRExpr,XoRExprRhs;int linked=0;
		for(int i = 0 ; i < sfcj.length()-1; i++) {
			
			for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
				for(int v=0; v < NM.getNodes().length ; v++) {	
					XoRExpr = new GRBQuadExpr();
					XoRExprRhs = new GRBQuadExpr();
					linked=0;
					if(NM.isLinked(NM.getNodes()[n], NM.getNodes()[v])!=null)
						linked=1;
					XoRExpr.addTerm(1.0, Xjin[i][n], Xjin[i+1][v]);
					XoRExpr.addTerm(1.0, Xjin[i][n], Rjin[i+1][v]);
					XoRExpr.addTerm(1.0, Rjin[i][n], Xjin[i+1][v]);
					XoRExpr.addTerm(1.0, Rjin[i][n], Rjin[i+1][v]);
					
					XoRExprRhs.addTerm(linked, Xjin[i][n], Xjin[i+1][v]);
					XoRExprRhs.addTerm(linked, Xjin[i][n], Rjin[i+1][v]);
					XoRExprRhs.addTerm(linked, Rjin[i][n], Xjin[i+1][v]);
					XoRExprRhs.addTerm(linked, Rjin[i][n], Rjin[i+1][v]);
					optModel.addQConstr(XoRExpr, GRB.EQUAL, XoRExprRhs, "XoR");
				}
			}
			
		}
	}
	
	
	//(Xjin+Rjin)(Xj(i+1)v + Xj(i+1)v) Fout(Vi) <= BWav(Lnv)
	public void linkBandWidthConstarint() throws GRBException{
		Link L;
		int availableBW, vnfOutFlow=0;
		GRBQuadExpr qBWExpr;
		for (int i = 0; i < sfcj.length()-1; i++) {
			vnfOutFlow = ((DeployedVNF)sfcj.getVnfList().get(i)).getOutFlow();
			for (int n = 0; n < NM.getNodes().length; n++) {
				for (int v = 0; v < NM.getNodes().length; v++) {
					qBWExpr = new GRBQuadExpr();
					availableBW = 0;
					L = NM.isLinked(NM.getNodes()[n], NM.getNodes()[v]);
					if(L != null)
						availableBW = L.getAvailableBW();
					qBWExpr.addTerm(vnfOutFlow, Xjin[i][n], Xjin[i+1][v]);
					qBWExpr.addTerm(vnfOutFlow, Xjin[i][n], Rjin[i+1][v]);
					qBWExpr.addTerm(vnfOutFlow, Rjin[i][n], Xjin[i+1][v]);
					qBWExpr.addTerm(vnfOutFlow, Rjin[i][n], Rjin[i+1][v]);
					
					optModel.addQConstr(qBWExpr, GRB.LESS_EQUAL, availableBW, "CBW");
				}
			}
		}
	}
	
	//(Xjin+Rjin)(Xj(i+1)v + Xj(i+1)v) Fout(Vi) <= BWav(Lnv)
	public void linkBandWidthConstarint2() throws GRBException{
		Link L;
		int n,v;
		int availableBW, vnfOutFlow=0;
		GRBQuadExpr qBWExpr;
		for (int l = 0; l < NM.getLinks().length; l++) {
			L=NM.getLinks()[l];
			n=L.getFromNode().getID();
			v=L.getToNode().getID();
			availableBW = L.getAvailableBW();
			qBWExpr = new GRBQuadExpr();
			for (int i = 0; i < sfcj.length()-1; i++) {
				vnfOutFlow = ((DeployedVNF)sfcj.getVnfList().get(i)).getOutFlow();	
				qBWExpr.addTerm(vnfOutFlow, Xjin[i][n], Xjin[i+1][v]);
				qBWExpr.addTerm(vnfOutFlow, Xjin[i][n], Rjin[i+1][v]);
				qBWExpr.addTerm(vnfOutFlow, Rjin[i][n], Xjin[i+1][v]);
				qBWExpr.addTerm(vnfOutFlow, Rjin[i][n], Rjin[i+1][v]);
			}
			optModel.addQConstr(qBWExpr, GRB.LESS_EQUAL, availableBW, "CBW"+l);
		}
	}
	
	public void e2eLatencyConstraints2() throws GRBException{
		Link L;
		double delay;
		GRBQuadExpr qBWExpr = new GRBQuadExpr();
		for (int i = 0; i < sfcj.length()-1; i++) {
			for (int n = 0; n < NM.getNodes().length; n++) {
				for (int v = 0; v < NM.getNodes().length; v++) {
					delay = 0;
					L = NM.isLinked(NM.getNodes()[n], NM.getNodes()[v]);
					if(L != null)
						delay = L.getDelay();
					qBWExpr.addTerm(delay, Xjin[i][n], Xjin[i+1][v]);
					qBWExpr.addTerm(delay, Xjin[i][n], Rjin[i+1][v]);
					qBWExpr.addTerm(delay, Rjin[i][n], Xjin[i+1][v]);
					qBWExpr.addTerm(delay, Rjin[i][n], Rjin[i+1][v]);
				}
			}
		}
		optModel.addQConstr(qBWExpr, GRB.LESS_EQUAL, sfcj.getE2eMaxLatency(), "CBW");
	}
	/*
	//useless as I removed virt_phy link mapping from objective
	public void linkMappingConstraints() throws GRBException{
		GRBLinExpr linkExprRhs, linkExpr;
		for(int i = 0 ; i < virt_Phy_Links_map.length ; i++) {//loop over Vi->Vi+1 links
			linkExpr = new GRBLinExpr();
			for(int nv = 0 ; nv < virt_Phy_Links_map[i].length ; nv++) {//loop over Vi->Vi+1 links
				linkExpr.addTerm(1.0, virt_Phy_Links_map[i][nv]);
			}
			optModel.addConstr(linkExpr, GRB.EQUAL, 1.0, "linkedConstr"+i);
		}
		
		//Link L;
		int fromNode=-1, toNode=-1;
		for(int i = 0 ; i < virt_Phy_Links_map.length-1; i++) {//loop over Vi->Vi+1 links
			for(int l = 0 ; l < virt_Phy_Links_map[i].length ; l++) {
				linkExpr = new GRBLinExpr(); linkExprRhs = new GRBLinExpr();
				toNode = NM.getLinks()[l].getToNode().getID();
				linkExpr.addTerm(toNode, virt_Phy_Links_map[i][l]);
				for(int v = 0 ; v < virt_Phy_Links_map[i+1].length ; v++) {
					fromNode = NM.getLinks()[v].getFromNode().getID();
					linkExprRhs.addTerm(fromNode, virt_Phy_Links_map[i+1][v]);
				}
				optModel.addConstr(linkExpr, GRB.EQUAL, linkExprRhs, "linkedConstr2"+i);
			}
		}
	}
	*/
	public void createConstraints2() throws GRBException{
		
//		
//		Link l;double linked =0.0;
//		int linkIndex=-1;
//		int addedOnce=0;
//		GRBQuadExpr qexprXRLhs,qexprXRRhs;
//		for (int i = 0; i < sfcj.getvLinks().length-1; i++) {
//			for(int n = 0 ; n < Xjin[i].length ; n++) {
//				qexprXRLhs = new GRBQuadExpr(); 
//				
//				qexprXRRhs = new GRBQuadExpr();
//				for (int v = 0; v < Xjin[i].length; v++) {
//					linked = 0;
//					linkIndex = NM.getLinkIndex(NM.getNodes()[n], NM.getNodes()[v]);
//					if(linkIndex != -1)
//						linked=1.0;
//					
//					if(addedOnce == 0 ) {
//						qexprXRLhs.addTerm(1.0, virt_Phy_Links_map[i][linkIndex], Xjin[((DeployedVNF)sfcj.getvLinks()[i].getTo()).getID()][n]);
//						qexprXRLhs.addTerm(1.0, virt_Phy_Links_map[i][linkIndex], Rjin[((DeployedVNF)sfcj.getvLinks()[i].getTo()).getID()][n]);
//						addedOnce=1;
//					}
//						
//					
//					qexprXRRhs.addTerm(linked,virt_Phy_Links_map[i+1][v], Xjin[((DeployedVNF)sfcj.getvLinks()[i+1].getFrom()).getID()][v]);
//					qexprXRRhs.addTerm(linked,virt_Phy_Links_map[i+1][v], Rjin[((DeployedVNF)sfcj.getvLinks()[i+1].getFrom()).getID()][v]);
//				}
//				optModel.addQConstr(qexprXRLhs, GRB.EQUAL, qexprXRRhs, "c");
//			}
//		}
		
		/*
		GRBLinExpr exprXRLhs,exprXRRhs;
		Link L;
		for (int i = 0; i < virt_Phy_Links_map.length; i++) { //loop over Vi-Vi+1 mappings 
			
			for (int nv = 0; nv < virt_Phy_Links_map[i].length; nv++) {//loop over nodes n E N
				exprXRLhs = new GRBLinExpr(); exprXRRhs = new GRBLinExpr();
				L = NM.getLinks()[nv];
				
				exprXRLhs.addTerm(1.0, Xjin[i][L.getFromNode().getID()]);
				exprXRLhs.addTerm(1.0, Rjin[i][L.getFromNode().getID()]);
				exprXRLhs.addTerm(1.0, Xjin[i+1][L.getToNode().getID()]);
				exprXRLhs.addTerm(1.0, Rjin[i+1][L.getToNode().getID()]);
				
				exprXRRhs.addTerm(2.0, virt_Phy_Links_map[i][nv]);		
				
				optModel.addConstr(exprXRLhs, GRB.EQUAL, exprXRRhs, "c");
			}
			
		}
		*/
		
////////////////////////////////////////////////////////////////////////////////////////////////////////////
				
		/*should be revisited 
		int fromNodeXR=-1, toNodeXR=-1; fromNode=-1; toNode=-1;
		for(int i = 0 ; i < virt_Phy_Links_map.length-1; i++) {//loop over Vi->Vi+1 links
			//linkExprRhs = new GRBLinExpr();
			for(int l = 0 ; l < virt_Phy_Links_map[i].length ; l++) {
				linkExpr = new GRBLinExpr(); linkExprRhs = new GRBLinExpr();
				
				toNodeXR = ((DeployedVNF)sfcj.getvLinks()[i].getTo()).getID();
				toNode = NM.getLinks()[l].getToNode().getID();
				
				linkExpr.addTerm(toNodeXR,Xjin[toNodeXR][toNode]); 
				linkExpr.addTerm(toNodeXR,Rjin[toNodeXR][toNode]); //either X or R will be 1.
				
				linkExpr.addTerm(toNode, virt_Phy_Links_map[i][l]);
				
				for(int v = 0 ; v < virt_Phy_Links_map[i+1].length ; v++) {
					fromNodeXR = ((DeployedVNF)sfcj.getvLinks()[i+1].getFrom()).getID();
					fromNode = NM.getLinks()[v].getFromNode().getID();
					
					linkExpr.addTerm(fromNodeXR,Xjin[fromNodeXR][fromNode]); 
					linkExpr.addTerm(fromNodeXR,Rjin[fromNodeXR][fromNode]); //either X or R will be 1.
					
					linkExprRhs.addTerm(fromNode, virt_Phy_Links_map[i+1][v]);
					//linkExpr.addTerm(commonNode, virt_Phy_Links_map[i+1][l]);
				}
				optModel.addConstr(linkExpr, GRB.EQUAL, linkExprRhs, "linkedConstr2"+i);
				}
		}*/
		
		/*
		//review this
		for(int i = 0 ; i < virt_Phy_Links_map.length ; i++) {//loop over Vi->Vi+1 links
			linkExpr = new GRBLinExpr();
			for(int nv = 0 ; nv < virt_Phy_Links_map[i].length ; nv++) {//loop over physical links links
				linkExpr.addTerm(1.0, virt_Phy_Links_map[i][nv]);
				linkExpr.addTerm(1.0, Xjin[i][NM.getLinks()[nv].getFromNode().getID()]);
				linkExpr.addTerm(1.0, Rjin[i][NM.getLinks()[nv].getFromNode().getID()]);
				linkExpr.addTerm(1.0, Xjin[i+1][NM.getLinks()[nv].getToNode().getID()]);
				linkExpr.addTerm(1.0, Rjin[i+1][NM.getLinks()[nv].getToNode().getID()]);
			}
			optModel.addConstr(linkExpr, GRB.EQUAL, 3.0, "linkedConstr"+i);
		}*/
		
		/*
		//virt_Phy_Links_map[i][l]
		//GRBLinExpr linkedExpr;
		GRBQuadExpr qLinkedExpr,qBWExpr;
		GRBQuadExpr qDelExpr = new GRBQuadExpr();
		for(int i = 0 ; i < virt_Phy_Links_map.length ; i++) {//loop over Vi->Vi+1 links
			qLinkedExpr = new GRBQuadExpr();
			qBWExpr = new GRBQuadExpr();
			//linkedExpr = new GRBLinExpr();
			for(int nv = 0; nv < virt_Phy_Links_map[i].length ; nv++) {
				
				//link mapping constraint
				qLinkedExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Xjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qLinkedExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Xjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
				
				qLinkedExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Rjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qLinkedExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Rjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
				
				
				
				//BW constraint
				qBWExpr.addTerm(((DeployedVNF)sfcj.getVnfList().get(i)).getOutFlow()-1, virt_Phy_Links_map[i][nv], Xjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qBWExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Xjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
				
				qBWExpr.addTerm(((DeployedVNF)sfcj.getVnfList().get(i)).getOutFlow()-1, virt_Phy_Links_map[i][nv], Rjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qBWExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Rjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
				qBWExpr.addTerm(-1.0*NM.getLinks()[nv].getAvailableBW(), virt_Phy_Links_map[i][nv]);
				
				//Delay constraint
				qDelExpr.addTerm(NM.getLinks()[nv].getDelay()-1, virt_Phy_Links_map[i][nv], Xjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qDelExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Xjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
				
				qDelExpr.addTerm(NM.getLinks()[nv].getDelay()-1, virt_Phy_Links_map[i][nv], Rjin[i][NM.getLinks()[nv].getFromNode().getID()]);//n
				qDelExpr.addTerm(1.0, virt_Phy_Links_map[i][nv], Rjin[i+1][NM.getLinks()[nv].getToNode().getID()]);//v
			}
			//optModel.addQConstr(qLinkedExpr, GRB.EQUAL, 2.0, "LinkConstr"+i);
			optModel.addQConstr(qBWExpr, GRB.LESS_EQUAL, 0.0, "BWConstr"+i);
		}
		optModel.addQConstr(qDelExpr, GRB.LESS_EQUAL, sfcj.getE2eMaxLatency(), "LatencyConstr");
		
GRBLinExpr exprXRLhs,exprXRRhs;
		int islinked=0;
		for (int i = 0; i < Xjin.length-1; i++) { //loop over Vi's 
			for (int n = 0; n < Xjin[i].length; n++) {//loop over nodes n E N
				exprXRLhs=new GRBLinExpr();exprXRRhs=new GRBLinExpr();
				exprXRLhs.addTerm(1.0, Xjin[i][n]);
				exprXRLhs.addTerm(1.0, Rjin[i][n]);
				for (int v = 0; v < Xjin[i+1].length; v++) {//loop over nodes v E N
					islinked=0;
					if( NM.isLinked(NM.getNodes()[n], NM.getNodes()[v]) != null)
						islinked = 1;
					//System.out.println("Node_"+n+" & Node_"+v+": "+islinked);
					exprXRRhs.addTerm(islinked, Xjin[i+1][v]);
					exprXRRhs.addTerm(islinked, Rjin[i+1][v]);
					
				}
				optModel.addConstr(exprXRLhs, GRB.EQUAL, exprXRRhs, "c");
			}
		}
		
		//Xjin+Rjin=1
		GRBLinExpr XoRExpr;
		for(int i = 0 ; i < sfcj.length() ; i++) {
			XoRExpr = new GRBLinExpr();
			for(int n = 0 ; n < NM.getNodes().length ; n++ ) {
				XoRExpr.addTerm(1.0, Xjin[i][n]);
				XoRExpr.addTerm(1.0, Rjin[i][n]);
			}
			optModel.addConstr(XoRExpr, GRB.EQUAL, 1.0, "XoR"+i);
		}
		
		
		*/
	}
	
	public void addConstraints() throws GRBException{
		X_RConstraints1();
		X_RConstraints2();
		X_RConstraints3();
		capacityConstraints();
		//p.linkMappingConstraints();
		pathContinuityConstraints();
		//linkBandWidthConstarint();//should be removed
		linkBandWidthConstarint2();
		e2eLatencyConstraints2();
	}
	
	public int[][] getSolution() throws GRBException {
		int[][] sol = new int[2][Xjin.length];
		System.out.println("        X0|R0"+" , "+"X1|R1"+" , "+"X2|R2"+" , "+"X3|R3"+" , "+"X4|R4");
		for(int i = 0; i < Xjin.length; i++) {//loops over VNFs in SFCj
			System.out.print("VNF "+sfcj.getVnfList().get(i).getName()+" ");
			for(int n = 0; n < Xjin[i].length; n++) {//loops over VNFs in SFCj
				if((int) Math.round(Xjin[i][n].get(GRB.DoubleAttr.X))==1) {
					solXR[i] = n;
					XorR[i]='X';
					sol[0][i]=n;
					sol[1][i]=1;//1=X
				}else if ((int) Math.round(Rjin[i][n].get(GRB.DoubleAttr.X)) ==1) {
					solXR[i] = n;
					XorR[i]='R';
					sol[0][i]=n;
					sol[1][i]=2;//2=R
				}
					
				System.out.print(" "+Math.round(Xjin[i][n].get(GRB.DoubleAttr.X))+"|"+Math.round(Rjin[i][n].get(GRB.DoubleAttr.X))+"  , ");
			}
			System.out.println();
		}
		System.out.println("Obj: " + optModel.get(GRB.DoubleAttr.ObjVal) + " " +objExpr.getValue());
		
		for (int i = 0; i < solXR.length; i++) {
			System.out.println("VNF_"+i+"-> node_"+solXR[i]+XorR[i]);
		}
		
		/*
		for (int i = 0; i < virt_Phy_Links_map.length; i++) { //loop over VNFs Vi of SFCj 
			System.out.print(sfcj.getVnfList().get(i).getName()+"->"+sfcj.getVnfList().get(i+1).getName()+": ");
			for (int j = 0; j < virt_Phy_Links_map[i].length; j++) { //loop over physical links between nodes
				System.out.print(NM.getLinks()[j].getFromNode().getID()+"->"+NM.getLinks()[j].getToNode().getID()+" ="+Math.round(virt_Phy_Links_map[i][j].get(GRB.DoubleAttr.X))+" | ");
			}
			System.out.println();
		}*/
		return sol;
	}
	
	public boolean optimize() throws GRBException {
		solved=false;
		optModel.optimize();
		if(optModel.get(GRB.IntAttr.Status)==GRB.Status.OPTIMAL) {
			System.out.println("Solution found:D");
			solved=true;
			return true;
		}
		System.out.println("Unfortunately, model is infeasible :(");
		return false;
	}
	
	
	public void disposeModel() throws GRBException{
		optModel.dispose();
	    env.dispose();
	}
	public int[] getSolXR() {
		return solXR;
	}

	public char[] getXorR() {
		return XorR;
	}

	public static void main(String[] args) {
		
		int noNodes = 5;
		int noLinks = 4;
		//small network connectivity matrix
		int[][] con2 = {{0,1,1,0,0},
				       {1,0,0,1,0},
				       {1,0,0,0,1},
				       {0,1,0,0,0},
				       {0,0,1,0,0}};
		//NSFNET network connectivity matrix
		int[][] con3 = {{0,1,0,1,0,0,0,0,0,0,0,0,0},
				        {1,0,1,0,0,0,1,0,0,0,0,0,0},
				        {0,1,0,0,1,0,0,0,0,0,0,0,0},
				        {1,0,0,0,0,1,0,0,0,0,0,0,0},
				        {0,0,1,0,0,1,0,0,1,0,0,0,0},
				        {0,0,0,1,1,0,1,0,0,0,0,0,0},
				        {0,1,0,0,0,1,0,1,0,0,0,0,0},
				        {0,0,0,0,0,0,1,0,0,0,0,1,0},
				        {0,0,0,0,1,0,0,0,0,1,0,0,1},
				        {0,0,0,0,0,0,0,0,1,0,1,0,0},
				        {0,0,0,0,0,0,0,0,0,1,0,1,0},
				        {0,0,0,0,0,0,0,1,0,0,1,0,1},
				        {0,0,0,0,0,0,0,0,1,0,0,1,0}};
		
		
		for (int j2 = 0; j2 < con2.length; j2++) {
			for (int k = 0; k < con2[j2].length; k++) {
				System.out.print(con2[j2][k]+" ");
			}
			System.out.println();
		}
		
		NetworkModel model = new NetworkModel(noNodes, noLinks, con2);			
		System.out.println(model.toString());
		System.out.println("/////////////////////////////////////////////////////Links/////////////////////////////////////////////////////////");
		System.out.println(model.displayLinks());
		System.out.println("/////////////////////////////////////////////////////Links/////////////////////////////////////////////////////////");
		ArrayList<VNF> availableVNFs = new ArrayList<VNF>();
		availableVNFs.add(new VNF("NAT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("FW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("VID", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("OPT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("MMS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("PAR", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("SGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("PGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("HSS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		availableVNFs.add(new VNF("RRU", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		
		
		SFCRequest sfc = SFCRequest.createSFCRequest(availableVNFs, 4, 2.5*model.getAvgDelay());
		System.out.println(sfc.toString());
		/*System.out.println("SmallestCPU: "+SFCRequest.smallestCPU+"LargestCPU:"+SFCRequest.largestCPU);
		model.getNodes()[0].setCapacityCPU(SFCRequest.smallestCPU);
		model.getNodes()[0].setAvailableCPU(SFCRequest.smallestCPU);
		System.out.println(model.getNodes()[0].toString());
		model.getNodes()[1].setCapacityCPU(SFCRequest.largestCPU);
		model.getNodes()[1].setAvailableCPU(SFCRequest.largestCPU);
		System.out.println(model.getNodes()[1].toString());
		model.getNodes()[2].setCapacityCPU(SFCRequest.largestCPU);
		model.getNodes()[2].setAvailableCPU(SFCRequest.largestCPU);
		System.out.println(model.getNodes()[2].toString());
		model.getNodes()[3].setCapacityCPU(SFCRequest.largestCPU);
		model.getNodes()[3].setAvailableCPU(SFCRequest.largestCPU);
		System.out.println(model.getNodes()[3].toString());
		model.getNodes()[4].setCapacityCPU(SFCRequest.largestCPU);
		model.getNodes()[4].setAvailableCPU(SFCRequest.largestCPU);
		System.out.println(model.getNodes()[4].toString());
		*/
		
//		sfc.getVnfList().get(0).setShareable(true);
//		sfc.getVnfList().get(1).setShareable(true);
//		sfc.getVnfList().get(2).setShareable(true);
//		
//		DeployedVNF v0 = new DeployedVNF(sfc.getVnfList().get(0));
//		DeployedVNF v1 = new DeployedVNF(sfc.getVnfList().get(1));
//		DeployedVNF v2 = new DeployedVNF(sfc.getVnfList().get(2));
//		
//		
//		model.getNodes()[3].placeVNF(v0, sfc.getID());
//		model.getNodes()[1].placeVNF(v1, sfc.getID());
//		model.getNodes()[0].placeVNF(v2, sfc.getID());
//		
//		//model.getLinks()[0].setDelay(40);
//		System.out.println("VNF_0 out-flow:"+((DeployedVNF)sfc.getVnfList().get(0)).getOutFlow());
//		model.getLinks()[5].setAvailableBW(((DeployedVNF)sfc.getVnfList().get(0)).getOutFlow()-1);
//		System.out.println("Link: "+model.getLinks()[5].toString());
//		//model.getLinks()[4].setDelay(40);
//		System.out.println("VNF_1 out-flow:"+((DeployedVNF)sfc.getVnfList().get(1)).getOutFlow());
//		model.getLinks()[1].setAvailableBW(((DeployedVNF)sfc.getVnfList().get(1)).getOutFlow()-1);
//		System.out.println("Link: "+model.getLinks()[1].toString());
//		
//		model.getLinks()[0].setLength(Utils.LINK_MAX_LENGTH);
//		model.getLinks()[4].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[1].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[2].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[3].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[5].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[6].setLength(Utils.LINK_MAX_LENGTH);
////		model.getLinks()[7].setLength(Utils.LINK_MAX_LENGTH);
//		
//		
//		//System.out.println("VNF 0 is deployed on node 4? "+model.getNodes()[4].isVNFDeployed(sfc.getVnfList().get(0).getName()));
//		//System.out.println("VNF 1 is deployed on node 4? "+model.getNodes()[4].isVNFDeployed(sfc.getVnfList().get(1).getName()));
//		//System.out.println("VNF 2 is deployed on node 4? "+model.getNodes()[4].isVNFDeployed(sfc.getVnfList().get(2).getName()));
		
		try {
			Placement p = new Placement(model, sfc);
			p.createObjective(2.5, 1.7, 2);
//			p.X_RConstraints1();
//			p.X_RConstraints2();
//			p.X_RConstraints3();
//			p.capacityConstraints();
//			p.pathContinuityConstraints();
//			p.linkBandWidthConstarint();
//			p.e2eLatencyConstraints2();
			p.addConstraints();
			p.optimize();
			int[][]sol=p.getSolution();char xr;
			for (int i = 0; i < sol[0].length; i++) {
				if(sol[1][i]==1)
					xr='X';
				else
					xr='R';
				System.out.println("VNF_"+i+" at node_"+sol[0][i]+" "+xr);
			}
			
			if(p.NM.placeSFCRequest(p.sfcj, sol)) {
				System.out.println("SFC-request placed");
				System.out.println(p.NM.displayNodes());
				System.out.println(p.NM.displayLinks());
			}else
				System.err.println("Something wrong happend while deploying the SFC");
			
			p.disposeModel();
			//p.displaySolution();
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			
			System.out.println("Error code: " + e.getErrorCode() + ". " +
			          e.getMessage());
			e.printStackTrace();
		}
	}
	
}
