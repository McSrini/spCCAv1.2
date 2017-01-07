/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cca;

import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class IndexNode {
    
    //is than a leaf index-node ?
    public boolean isLeaf = false;
    //is this a CCA node ?
    public boolean isCCA = false;
    //use this variable if node marked as CCA
    public int numNodeLPsToSolveToArriveAtLeafs = -ONE;
    
    public String nodeID = EMPTY_STRING + -ONE; //default for subtree root 
    public int depthFromSubtreeRoot = ZERO;
    
    public IndexNode leftChild =null, rightChild =null;//, parent = null ; 
    
    public List<NodeAttachment> migratableLeafNodesToTheLeft = new ArrayList<NodeAttachment>();
    public List<NodeAttachment> migrateableLeafNodesToTheRight = new ArrayList<NodeAttachment>();

    public List<NodeAttachment> leafNodesToTheLeft = new ArrayList<NodeAttachment>();
    public List<NodeAttachment> leafNodesToTheRight = new ArrayList<NodeAttachment>();
                
    
    //branching instructions needed to create this node, populated when required
    public List <BranchingInstruction> cumulativeBranchingInstructions = new ArrayList <BranchingInstruction>();
         
    
    public   IndexNode(  List<NodeAttachment> leafNodesUnderMe , int depthFromSubtreeRoot, String nodeID /*, IndexNode parent*/){
        this. depthFromSubtreeRoot=   depthFromSubtreeRoot;
        this.nodeID=nodeID;
        //this.parent = parent; parent of index node not needed
        //no children at creation time
         
        //populate leafs to the left and right
        for (NodeAttachment leafNode : leafNodesUnderMe) {
            
            if (   this.nodeID.equals(leafNode.nodeID )  && leafNodesUnderMe.size()==ONE){
                //  this index-node is itself a leaf, no kids
                isLeaf = true;
            } else{
                if (!leafNode.isRightChildCumulative.get(ZERO+depthFromSubtreeRoot)) {
                    this.leafNodesToTheLeft. add(leafNode  );
                    if (  leafNode.isMigrateable ) this.migratableLeafNodesToTheLeft. add(leafNode  );
                }
                if ( leafNode.isRightChildCumulative.get(ZERO+depthFromSubtreeRoot)) {
                    this.leafNodesToTheRight.add(leafNode  );
                    if (  leafNode.isMigrateable ) this.migrateableLeafNodesToTheRight.add(leafNode  );
                }                  
               
            }
            
        }
        
    }
    
    //split tree until we have identified CCA nodes
    public void splitToCCA ( ) {
        
        //if  either side of subtree root has >= NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE , split
        //else if one side has 90% and the other side has 10%, then also split
        boolean isSplitNeeded = this.migratableLeafNodesToTheLeft.size() >=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE;
        isSplitNeeded = isSplitNeeded || this.migrateableLeafNodesToTheRight.size() >=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE;
        
        boolean isSplitDesirable = this.migratableLeafNodesToTheLeft.size() >=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE *(ONE-CCA_TOLERANCE_FRACTION);
        isSplitDesirable = isSplitDesirable &&  this.migrateableLeafNodesToTheRight.size() <= NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE * CCA_TOLERANCE_FRACTION;
        
        if (!isSplitDesirable) {
            //check the reverse
            isSplitDesirable =    this.migrateableLeafNodesToTheRight.size() >= NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE * (ONE-CCA_TOLERANCE_FRACTION);
            isSplitDesirable = isSplitDesirable && this.migratableLeafNodesToTheLeft.size() <=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE * CCA_TOLERANCE_FRACTION;
        }
        
        isSplitNeeded = isSplitNeeded ||isSplitDesirable;
        
        if (isSplitNeeded) {
            if (this.migratableLeafNodesToTheLeft.size()>ZERO) {
                //create left subtree
                this.leftChild = new IndexNode(this.leafNodesToTheLeft, ONE + this.depthFromSubtreeRoot,  IndexTree.leftChildMap.get( this.nodeID)  );
                //recursive call
                this.leftChild.splitToCCA();
            }
            if (this.migrateableLeafNodesToTheRight.size()>ZERO) {
                //create right sub tree
                this.rightChild = new IndexNode(this.leafNodesToTheRight, ONE + this.depthFromSubtreeRoot, IndexTree.rightChildMap.get( this.nodeID) );
                //recursive call
                this.rightChild.splitToCCA();
            }
        } else {
            //if number of migratable leafs under this node >=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE, mark as CCA
            //in case NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE=1 and this is a migratable leaf , then also mark as CCA
            if (this.migrateableLeafNodesToTheRight.size()+ this.migratableLeafNodesToTheLeft.size()>=NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE*(ONE-CCA_TOLERANCE_FRACTION)) 
                this.isCCA=true;
            if (this.isLeaf && NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE==ONE && IndexTree.solutionTreeNodeMap.get( this.nodeID).isMigrateable) this.isCCA=true;
            
            //if marked CCA, populate cumulative branching instructions needed to create this CCA node
            if (isCCA) {
                //populate cumulative branching instructions
                this.cumulativeBranchingInstructions= IndexTree.getCumulativeBranchingInstructions(this.nodeID)  ;
                this.setNumberOfNodeLPsRequiredToConstructAllLeafs();
            }
        }
         
    }
    
    public List<IndexNode> getCCANodes () {
        List<IndexNode> retval = new ArrayList<IndexNode> ();
        
        if (  this.isCCA  )         {
            retval.add(this);
        }else {
            if (this.leftChild!=null)  retval.addAll(this.leftChild.getCCANodes());
            if (this.rightChild!=null) retval.addAll(this.rightChild.getCCANodes() );            
        }
         
        return retval;
    }
    
    public String toString () {
        String result = EMPTY_STRING+"\n";
        
        result = "\n CCA node is "+this.nodeID+"\n";
        
        result+= " Migratable Leaf nodes to Left:\n(";
        for (NodeAttachment leafNode  : this.migratableLeafNodesToTheLeft  ){
            result+=leafNode.nodeID+" ";
        }
        result+= ")\n";
        
        result+= " Migratable Leaf nodes to Right:\n(";
        for (NodeAttachment leafNode  : this.migrateableLeafNodesToTheRight){
            result+=leafNode.nodeID+" ";
        }
        result+= ")\n";
        
        result+= " All Leaf nodes to Left:\n(";
        for (NodeAttachment leafNode  : this.leafNodesToTheLeft){
            result+=leafNode.nodeID+" ";
        }
        result+= ")\n";
        
        result+= " All Leaf nodes to Right:\n(";
        for (NodeAttachment leafNode  : this.leafNodesToTheRight){
            result+=leafNode.nodeID+" ";
        }
        result+= ")\n";
        
        return result;
    }
    
    //this number , when multiplied by average node LP solve time, should be much less than expected solution time of CCA node
    public void setNumberOfNodeLPsRequiredToConstructAllLeafs( ){
        //move up from every leaf towards CCA node, and count the number of 
        //unique node IDs encountered along the way, including the CCA node itself
        //
        //Be careful of the case when the CCA node is itself a leaf, in which case no need to solve any node LPs
        
        numNodeLPsToSolveToArriveAtLeafs = ZERO ;
        
        if (leafNodesToTheLeft.size() + leafNodesToTheRight.size() > ZERO){
            
            Map<String, Integer> uniqueNodeMap = new HashMap<String, Integer>();
            
            for (NodeAttachment node :  leafNodesToTheLeft) {
                NodeAttachment thisNode = node;
                NodeAttachment parentNode = node.parentData;
                while (!thisNode.nodeID.equals(  nodeID)){
                    uniqueNodeMap.put(parentNode.nodeID, ONE);
                    thisNode = parentNode;
                    parentNode = thisNode.parentData;
                }
            }
            for (NodeAttachment node :  leafNodesToTheRight) {
                NodeAttachment thisNode = node;
                NodeAttachment parentNode = node.parentData;
                while (!thisNode.nodeID.equals(  nodeID)){
                    uniqueNodeMap.put(parentNode.nodeID, ONE);
                    thisNode = parentNode;
                    parentNode = thisNode.parentData;
                }
            }
             
            numNodeLPsToSolveToArriveAtLeafs =uniqueNodeMap.keySet().size();
        } 
        
    }
    
}
