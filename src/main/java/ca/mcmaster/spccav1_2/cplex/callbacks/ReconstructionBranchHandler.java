/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex.callbacks;

import static ca.mcmaster.spccav1_2.Constants.IS_MAXIMIZATION;
import static ca.mcmaster.spccav1_2.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.spccav1_2.Constants.LOG_FOLDER;
import static ca.mcmaster.spccav1_2.Constants.MINUS_ONE_STRING;
import static ca.mcmaster.spccav1_2.Constants.ONE;
import static ca.mcmaster.spccav1_2.Constants.TWO;
import static ca.mcmaster.spccav1_2.Constants.ZERO;
import ca.mcmaster.spccav1_2.controlledBranching.BranchingInstructionNode;
import ca.mcmaster.spccav1_2.controlledBranching.BranchingInstructionTree;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import ca.mcmaster.spccav1_2.cplex.utilities.UtilityLibrary;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ReconstructionBranchHandler extends BranchCallback{
    
    private static Logger logger=Logger.getLogger(ReconstructionBranchHandler.class);
         
    private  double bestKnownSOlutionValue ;
    private    BranchingInstructionTree branchingInstructionTree;
    private IloCplex cplex;
    private IloNumVar[]  modelVars;
    
    //map of new node, to old node ID
    private Map<String, String> newToOldMap = new HashMap<String, String>();
    //map of old  node, to new node ID, for every old node whose kids needs to be created
    private Map<String, String> oldToNewMap = new HashMap<String, String>();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ReconstructionBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
          
    }
    
    public ReconstructionBranchHandler( double bestKnownSOlutionValue,Map<String, String> newToOldMap, 
            Map<String, String> oldToNewMap , BranchingInstructionTree branchingInstructionTree, IloCplex cplex) throws IloException {
        this.bestKnownSOlutionValue =    bestKnownSOlutionValue;
        this.oldToNewMap =oldToNewMap;
        this.newToOldMap =newToOldMap;
        this. branchingInstructionTree=   branchingInstructionTree;
        this.cplex = cplex;
        modelVars =  UtilityLibrary.getVariablesInModel(cplex);
        
    }

    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
             
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);
                
            } 
            
            if (IS_MAXIMIZATION && getObjValue() < bestKnownSOlutionValue){
                prune();
            }else if (!IS_MAXIMIZATION && getObjValue() > bestKnownSOlutionValue){
                prune();
            }else {
           
                CreateChildrenAsPerInstructions(nodeData);
            }
            
        }
        
    }
    
    //create two child nodes, as per instructions for this node in the instruction Tree
    //update old to new map, and new to old map
    private void  CreateChildrenAsPerInstructions(NodeAttachment nodeData) throws IloException{
        
        IloNumVar[][] vars = new IloNumVar[TWO][];
        double[ ][] bounds = new double[TWO ][];
        BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
        
        //node ID in this tree
        String newNodeID = nodeData.nodeID;
        //corresponding node ID in the original host
        String oldNodeID = newToOldMap.get(newNodeID );
        
        //now allow  both kids to spawn
        BranchingInstructionNode biNode = this.branchingInstructionTree.nodeMap.get(oldNodeID );
        int numKids = biNode.childList.size();
        for (int childNum = ZERO ;childNum< numKids;  childNum++) {   

            //apply the bound changes specific to this child
            BranchingInstruction bi =  biNode.branchingInstructionList.get(childNum);
            int numVars = bi.varNames.size();
            vars[childNum]= new IloNumVar[numVars];
            bounds[childNum]= new double[numVars];
            dirs[childNum] = new BranchDirection[numVars];
            this.convertBranchingInstructionToArray(bi, vars[childNum],  bounds[childNum],dirs[childNum]);

            NodeAttachment thisChild  =  createChildNode( nodeData,   childNum ); 

            IloCplex.NodeId nodeid = makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
                          
            thisChild.nodeID =nodeid.toString();

            logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " using "+ bi ) ;

            if (childNum == ZERO) {
                //update left child info
                nodeData.leftChildNodeID =thisChild.nodeID;
                nodeData.branchingInstructionForLeftChild =bi;
            }else {
                nodeData.rightChildNodeID =thisChild.nodeID;
                nodeData.branchingInstructionForRightChild =bi;
            }
            
            //update our maps    
            
            //this is the kid we have created
            oldNodeID =biNode.childList.get(childNum).nodeID;
            if (oldToNewMap.containsKey(oldNodeID )) {
                //replace null value, representing an as yet uncreated node, with the node ID just created
                this.oldToNewMap.put( oldNodeID,  thisChild.nodeID );
                this.newToOldMap.put( thisChild.nodeID , oldNodeID );
                logger.info(oldNodeID + " and new child id " + thisChild.nodeID) ;
            }
        
        }//end for 2 kids          
         
    }
    
    private NodeAttachment createChildNode (NodeAttachment parentNodeData,int childNum ){
        NodeAttachment thisChild  = new NodeAttachment (); 
        
        thisChild.parentData = parentNodeData;
        thisChild.depthFromSubtreeRoot=parentNodeData.depthFromSubtreeRoot + ONE;
        
        //thisChild.isMigrateable= rand.nextBoolean() || rand.nextBoolean();
     
        //0 or false indicates L
        thisChild.isRightChildCumulative.addAll( parentNodeData.isRightChildCumulative);
        thisChild.isRightChildCumulative.add(( ONE==childNum));
         
        return     thisChild;    
    }
    
    //get the bound and dirs for model vars in array format used by CPLEX
    private void convertBranchingInstructionToArray ( BranchingInstruction bi ,   IloNumVar[]  vars,  double[ ]  bounds , IloCplex.BranchDirection[ ]   dirs ){
        for ( IloNumVar var : this.modelVars){
            //find the index of this var
            int index = bi.varNames.indexOf(var.getName());
            if (index>=ZERO) {
                vars[index]= var;
                bounds[index] = bi.varBounds.get(index);
                dirs[index]=bi.isBranchDirectionDown.get(index) ? IloCplex.BranchDirection.Down : IloCplex.BranchDirection.Up;
            }
        }
    }
    
}
