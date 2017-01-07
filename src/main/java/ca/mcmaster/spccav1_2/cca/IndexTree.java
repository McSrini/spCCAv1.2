/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cca;

import static ca.mcmaster.spccav1_2.Constants.*;
import static ca.mcmaster.spccav1_2.Constants.ZERO;
import ca.mcmaster.spccav1_2.cplex.callbacks.BranchHandler;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import static java.lang.System.exit;
import java.util.*;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class IndexTree {
    
    private static Logger logger=Logger.getLogger(IndexTree.class);
    
    //every time an index tree is constructed on a partition, we prepare these maps which serve as indices
    public static Map<String,NodeAttachment > solutionTreeNodeMap = new HashMap<String,NodeAttachment> ();
    public static Map<String, String> leftChildMap = new HashMap<String, String>();
    public static Map<String, String> rightChildMap = new HashMap<String, String>();
    public static Map<String, String> parentsMap = new HashMap<String, String>();
     
    private IndexNode subtreeRoot ;
    
    private List<NodeAttachment> allLeafNodes;
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+IndexTree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
          
    }
    
    public IndexTree (List<NodeAttachment> allLeafNodes ) {
        this.allLeafNodes= allLeafNodes;
        prepareIndexMaps();
        
        subtreeRoot = new IndexNode(   allLeafNodes, ZERO, EMPTY_STRING+-ONE);
        
    }
    
    public void splitToCCA ( ) {
        subtreeRoot.splitToCCA();
    }
    
    //return the CCA subtrees found  
    public List<IndexNode> getCCANodes () {
        return this.subtreeRoot.getCCANodes();          
    }
    
    //find the CCA node by climbing up from every leaf.
    //The last leaf which encounters an ancestor with ref count = # of leafs is the CCA
    public IndexNode getCCANode  (List<String> selectedLeafNodeIDs) {
        NodeAttachment ccaNode  =null;
        int selectedLeafCount = selectedLeafNodeIDs.size();
        
        if (selectedLeafCount<TWO) {
            logger.error( "At least 2 leafs must be selected to find CCA.");
            exit(4);
        }
        
        Map<String, Integer> refCountMap = new HashMap<String, Integer>();
        
        for (int index = ZERO; index < selectedLeafCount; index ++){
            
            String thisLeafID = selectedLeafNodeIDs.get(index);            
            NodeAttachment node = solutionTreeNodeMap.get( thisLeafID);
            //
            //climb up
            NodeAttachment parentNode = node.parentData;
            while (parentNode!=null) {                
                if (refCountMap.containsKey( parentNode.nodeID)){
                    int value = refCountMap.get( parentNode.nodeID);
                    refCountMap.put(parentNode.nodeID, value+ONE);
                    
                    if ((index==selectedLeafCount-ONE) && (value+ONE==selectedLeafCount)) {
                        ccaNode  = parentNode;
                        break; //we have found the CCA node
                    }
                    
                }else {
                    refCountMap.put(parentNode.nodeID, ONE);
                }
                node = parentNode;
                parentNode = node.parentData;                
            }
            
        }
        
        return getIndexNodeFromNodeAttachment(ccaNode) ;
    }
    
    public static List<BranchingInstruction> getCumulativeBranchingInstructions(String nodeID){
        return getCumulativeBranchingInstructions(nodeID, MINUS_ONE_STRING);//ALL THE WAY TO subtree ROOT
    }
    
    public static List<BranchingInstruction> getCumulativeBranchingInstructions(String nodeID, String fromNode){
        List<BranchingInstruction> cumulativeBranchingInstructions = new ArrayList<BranchingInstruction>  ();
        
        NodeAttachment thisNode = IndexTree.solutionTreeNodeMap.get( nodeID);
        NodeAttachment parent = thisNode.parentData;
        while (parent !=null && !thisNode.nodeID.equals(fromNode)) {
            //check if right child, and get the branching instruction
            if (parent.leftChildNodeID.equals(nodeID)){
                cumulativeBranchingInstructions.add( parent.branchingInstructionForLeftChild);
            }else {
                cumulativeBranchingInstructions.add( parent.branchingInstructionForRightChild);
            }
            
            //climb up
            thisNode = thisNode.parentData ; 
            parent = thisNode.parentData;
              
        }
        
        return cumulativeBranchingInstructions;
    }
    
    //convert the node attachment of a CCA node into an indexnode
    private  IndexNode getIndexNodeFromNodeAttachment ( NodeAttachment ccaNodeAttachment){
        IndexNode result = new IndexNode( this.getLeafNodesUnderMe( ccaNodeAttachment.nodeID   ), ccaNodeAttachment.depthFromSubtreeRoot ,   ccaNodeAttachment.nodeID  );
        result.isCCA= true;
        //populate cumulative branching instructions
        result.cumulativeBranchingInstructions=getCumulativeBranchingInstructions(ccaNodeAttachment.nodeID);
        result.setNumberOfNodeLPsRequiredToConstructAllLeafs();                
                        
        return result;
    }
    
 
    
    private   void prepareIndexMaps () {
        
        solutionTreeNodeMap.clear();
        leftChildMap.clear();
        rightChildMap.clear();
        parentsMap.clear();
        
        NodeAttachment subtreeRoot =null;
        
        for (NodeAttachment leaf :allLeafNodes ){
            //climb all the way up to subtree root, recording parent ID and NodeAttachment
            
            NodeAttachment thisNode = leaf;
            NodeAttachment parent = leaf.parentData;
            while (parent !=null) {
                solutionTreeNodeMap.put(thisNode.nodeID, thisNode);
                parentsMap.put(thisNode.nodeID, parent.nodeID);
                
                if (parent.leftChildNodeID.equals( thisNode.nodeID)){
                    leftChildMap.put( parent.nodeID, thisNode.nodeID );
                } else {
                    rightChildMap.put( parent.nodeID, thisNode.nodeID );
                }
                
                //climb up
                thisNode = parent;
                parent = parent.parentData;
            }
            
            //parent is null
            subtreeRoot = thisNode;
        }
        
        solutionTreeNodeMap.put(subtreeRoot.nodeID, subtreeRoot );
        parentsMap.put(subtreeRoot.nodeID, null);
            
    }
    
    //get all leaf nodes under a node ID
    //Assumes argument is itself not a leaf
    private List<NodeAttachment> getLeafNodesUnderMe(String nodeID){
        List<NodeAttachment> result = new ArrayList<NodeAttachment>();
        
        NodeAttachment thisNode = solutionTreeNodeMap.get(nodeID );
        if (thisNode.leftChildNodeID !=null) {
            NodeAttachment leftChild = solutionTreeNodeMap.get( thisNode.leftChildNodeID );
            if (leftChild.isLeaf()) result.add(leftChild); else result.addAll(getLeafNodesUnderMe( leftChild.nodeID));
        }
        if (thisNode.rightChildNodeID !=null) {
            NodeAttachment rightChild = solutionTreeNodeMap.get( thisNode.rightChildNodeID );
            if (rightChild.isLeaf()) result.add(rightChild); else result.addAll(getLeafNodesUnderMe( rightChild.nodeID));
        }
        
        return result;
    }
    
}
